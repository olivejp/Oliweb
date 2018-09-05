package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.dto.elasticsearch.AnnonceDto;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchResult;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.service.AnnonceService;
import oliweb.nc.oliweb.service.misc.ElasticsearchQueryBuilder;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.ServicesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.utility.FirebaseUtility;
import oliweb.nc.oliweb.utility.LiveDataOnce;

import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ASC;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_PRICE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_TITLE;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_REQUEST_REF;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class AdvancedSearchActivityViewModel extends AndroidViewModel {

    private static final String TAG = AdvancedSearchActivityViewModel.class.getName();

    private GenericTypeIndicator<ElasticsearchResult<AnnonceDto>> genericClassDetail;

    private ArrayList<AnnonceFull> listAnnonce;
    private MutableLiveData<ArrayList<AnnonceFull>> liveListAnnonce;
    private DatabaseReference newRequestRef;
    private MutableLiveData<AtomicBoolean> loading;
    private AnnonceService annonceService;
    private Gson gson = new Gson();
    private DatabaseReference requestReference;
    private CategorieRepository categorieRepository;
    private CategorieEntity currentCategorie;

    private ValueEventListener requestValueListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if (dataSnapshot.child("no_results").exists()) {
                liveListAnnonce.postValue(listAnnonce);
                newRequestRef.removeEventListener(this);
                newRequestRef.removeValue();
                updateLoadingStatus(false);
            } else {
                if (dataSnapshot.child("results").exists()) {
                    DataSnapshot snapshotResults = dataSnapshot.child("results");
                    for (DataSnapshot child : snapshotResults.getChildren()) {
                        ElasticsearchResult<AnnonceDto> elasticsearchResult = child.getValue(genericClassDetail);
                        if (elasticsearchResult != null) {
                            AnnonceFull annonceFull = AnnonceConverter.convertDtoToAnnonceFull(elasticsearchResult.get_source());
                            listAnnonce.add(annonceFull);
                        }
                    }
                    liveListAnnonce.postValue(listAnnonce);
                    newRequestRef.removeEventListener(this);
                    newRequestRef.removeValue();
                    updateLoadingStatus(false);
                }
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            updateLoadingStatus(false);
        }
    };

    public AdvancedSearchActivityViewModel(@NonNull Application application) {
        super(application);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        ServicesComponent componentServices = DaggerServicesComponent.builder().contextModule(new ContextModule(application)).build();

        categorieRepository = component.getCategorieRepository();
        annonceService = componentServices.getAnnonceService();
        requestReference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_REQUEST_REF);
        listAnnonce = new ArrayList<>();
        genericClassDetail = new GenericTypeIndicator<ElasticsearchResult<AnnonceDto>>() {
        };
    }

    public LiveData<ArrayList<AnnonceFull>> getLiveListAnnonce() {
        if (liveListAnnonce == null) {
            liveListAnnonce = new MutableLiveData<>();
            listAnnonce = new ArrayList<>();
            liveListAnnonce.setValue(listAnnonce);
        }
        return liveListAnnonce;
    }

    public LiveData<AtomicBoolean> getLoading() {
        if (loading == null) {
            loading = new MutableLiveData<>();
            loading.setValue(new AtomicBoolean(false));
        }
        return loading;
    }

    public LiveDataOnce<SearchActivityViewModel.AddRemoveFromFavorite> addOrRemoveFromFavorite(String uidCurrentUser, AnnonceFull annoncePhotos) {
        return annonceService.addOrRemoveFromFavorite(uidCurrentUser, annoncePhotos);
    }

    private void updateLoadingStatus(boolean status) {
        if (loading == null) {
            loading = new MutableLiveData<>();
        }
        loading.postValue(new AtomicBoolean(status));
    }

    public boolean isConnected() {
        return NetworkReceiver.checkConnection(getApplication().getApplicationContext());
    }

    public void makeAnAdvancedSearch(String categorie, boolean photo, Integer lowestPrice, Integer highestPrice, String query, int pagingSize, int from, int tri, int direction) {
        updateLoadingStatus(true);
        FirebaseUtility.getServerTimestamp()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.e(TAG, throwable.getLocalizedMessage(), throwable);
                    updateLoadingStatus(false);
                })
                .doOnSuccess(timestamp -> {
                    if (from == 0) {
                        listAnnonce.clear();
                    }

                    List<String> listField = new ArrayList<>();
                    listField.add("titre");
                    listField.add("description");

                    String directionStr;

                    if (direction == ASC) {
                        directionStr = "asc";
                    } else {
                        directionStr = "desc";
                    }

                    String field;
                    if (tri == SORT_TITLE) {
                        field = "titre";
                    } else if (tri == SORT_PRICE) {
                        field = "prix";
                    } else {
                        field = "datePublication";
                    }

                    ElasticsearchQueryBuilder builder = new ElasticsearchQueryBuilder();
                    builder.setSize(pagingSize)
                            .setTimestamp(timestamp)
                            .setFrom(from)
                            .setMultiMatchQuery(listField, query)
                            .addSortingFields(field, directionStr);

                    // Création d'une nouvelle request dans la table request
                    newRequestRef = requestReference.push();
                    newRequestRef.setValue(gson.fromJson(builder.build(), Object.class));

                    // Ensuite on va écouter les changements pour cette nouvelle requête
                    newRequestRef.addValueEventListener(requestValueListener);
                })
                .subscribe();
    }

    public Single<List<CategorieEntity>> getListCategorie() {
        return categorieRepository.getListCategorie();
    }

    public void setCurrentCategorie(CategorieEntity categorie) {
        this.currentCategorie = categorie;
    }
}
