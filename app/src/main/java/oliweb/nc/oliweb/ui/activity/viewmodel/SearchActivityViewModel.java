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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchResult;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.service.AnnonceService;
import oliweb.nc.oliweb.service.misc.ElasticsearchQueryBuilder;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;
import oliweb.nc.oliweb.utility.LiveDataOnce;

import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ASC;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_PRICE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_TITLE;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_REQUEST_REF;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class SearchActivityViewModel extends AndroidViewModel {

    private static final String TAG = SearchActivityViewModel.class.getName();

    public enum AddRemoveFromFavorite {
        ONE_OF_YOURS,
        REMOVE_SUCCESSFUL,
        REMOVE_FAILED,
        ADD_SUCCESSFUL,
        ADD_FAILED
    }

    private GenericTypeIndicator<ElasticsearchResult<AnnonceFirebase>> genericClassDetail;

    @Inject
    AnnonceService annonceService;

    @Inject
    AnnonceFullRepository annonceFullRepository;

    @Inject
    FirebaseUtilityService utilityService;

    private ArrayList<AnnonceFull> listAnnonce;
    private MutableLiveData<ArrayList<AnnonceFull>> liveListAnnonce;
    private DatabaseReference newRequestRef;
    private MutableLiveData<AtomicBoolean> loading;
    private Gson gson = new Gson();
    private DatabaseReference requestReference;
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
                        ElasticsearchResult<AnnonceFirebase> elasticsearchResult = child.getValue(genericClassDetail);
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

    public SearchActivityViewModel(@NonNull Application application) {
        super(application);

        ((App) application).getFirebaseServicesComponent().inject(this);
        ((App) application).getServicesComponent().inject(this);
        ((App) application).getDatabaseRepositoriesComponent().inject(this);

        requestReference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_REQUEST_REF);
        listAnnonce = new ArrayList<>();
        genericClassDetail = new GenericTypeIndicator<ElasticsearchResult<AnnonceFirebase>>() {
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

    public LiveDataOnce<AddRemoveFromFavorite> addOrRemoveFromFavorite(String uidCurrentUser, AnnonceFull annoncePhotos) {
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

    public void makeAnAdvancedSearch(String libelleCategorie, boolean withPhotoOnly, Integer lowestPrice, Integer highestPrice, String query, int pagingSize, int from, int tri, int direction) {
        updateLoadingStatus(true);
        utilityService.getServerTimestamp()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.e(TAG, throwable.getLocalizedMessage(), throwable);
                    updateLoadingStatus(false);
                })
                .doOnSuccess(timestamp -> {
                    if (from == 0) {
                        listAnnonce.clear();
                    }

                    ElasticsearchQueryBuilder builder = initQueryBuilder(timestamp, pagingSize, direction, from, tri);
                    if (query != null) {
                        builder.setMultiMatchQuery(Arrays.asList("titre", "description"), query);
                    }
                    if (libelleCategorie != null) {
                        builder.setCategorie(libelleCategorie);
                    }
                    if (lowestPrice != null && highestPrice != null) {
                        builder.setRangePrice(lowestPrice, highestPrice);
                    }
                    builder.setWithPhotoOnly(withPhotoOnly);

                    // Création d'une nouvelle request dans la table request
                    newRequestRef = requestReference.push();
                    newRequestRef.setValue(gson.fromJson(builder.build(), Object.class));

                    // Ensuite on va écouter les changements pour cette nouvelle requête
                    newRequestRef.addValueEventListener(requestValueListener);
                })
                .subscribe();
    }

    /**
     * Launch a search with the Query
     *
     * @param query
     * @param pagingSize
     * @param from
     * @param tri
     * @param direction
     */
    public void makeASearch(String query, int pagingSize, int from, int tri, int direction) {
        updateLoadingStatus(true);
        utilityService.getServerTimestamp()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnError(throwable -> {
                    Log.e(TAG, throwable.getLocalizedMessage(), throwable);
                    updateLoadingStatus(false);
                })
                .doOnSuccess(timestamp -> {
                    if (from == 0) {
                        listAnnonce.clear();
                    }

                    ElasticsearchQueryBuilder builder = initQueryBuilder(timestamp, pagingSize, direction, from, tri);
                    builder.setMultiMatchQuery(Arrays.asList("titre", "description"), query);

                    // Création d'une nouvelle request dans la table request
                    newRequestRef = requestReference.push();
                    newRequestRef.setValue(gson.fromJson(builder.build(), Object.class));

                    // Ensuite on va écouter les changements pour cette nouvelle requête
                    newRequestRef.addValueEventListener(requestValueListener);
                })
                .subscribe();
    }

    public LiveData<List<AnnonceFull>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceFullRepository.findFavoritesByUidUser(uidUtilisateur);
    }

    private ElasticsearchQueryBuilder initQueryBuilder(Long timestamp, int pagingSize, int direction, int from, int tri) {
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
                .addSortingFields(field, directionStr);

        return builder;
    }
}
