package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.annotation.NonNull;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchRequest;
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
    public static final String NO_RESULTS = "no_results";
    public static final String RESULTS = "results";
    public static final String FIELD_TITRE = "titre";
    public static final String FIELD_TITRE_SORT = "titre.keyword";
    public static final String FILED_PRIX = "prix";
    public static final String FIELD_DATE_PUBLICATION = "datePublication";
    public static final String FIELD_DESCRIPTION = "description";

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

    @Inject
    @Named("processScheduler")
    Scheduler processScheduler;

    @Inject
    @Named("androidScheduler")
    Scheduler androidScheduler;

    private ArrayList<AnnonceFull> listAnnonce;
    private MutableLiveData<ArrayList<AnnonceFull>> liveListAnnonce;
    private DatabaseReference newRequestRef;
    private MutableLiveData<AtomicBoolean> loading;
    private Gson gson = new Gson();
    private DatabaseReference requestReference;
    private Observable<Long> obsDelay = Observable.timer(30, TimeUnit.SECONDS);
    private ValueEventListener requestValueListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if (dataSnapshot.child(NO_RESULTS).exists()) {
                liveListAnnonce.postValue(listAnnonce);
                newRequestRef.removeEventListener(this);
                newRequestRef.removeValue();
                updateLoadingStatus(false);
            } else {
                if (dataSnapshot.child(RESULTS).exists()) {
                    DataSnapshot snapshotResults = dataSnapshot.child(RESULTS);
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
            newRequestRef.removeValue();
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

    public void search(List<String> libellesCategorie, boolean withPhotoOnly, int lowestPrice, int highestPrice, String query, int pagingSize, int from, int tri, int direction) {
        updateLoadingStatus(true);

        if (from == 0) {
            listAnnonce.clear();
        }
        ElasticsearchQueryBuilder builder = initQueryBuilder(pagingSize, direction, from, tri);
        if (query != null) {
            builder.setMultiMatchQuery(Arrays.asList(FIELD_TITRE, FIELD_DESCRIPTION), query);
        }
        if (libellesCategorie != null) {
            builder.setCategorie(libellesCategorie);
        }
        if (lowestPrice >= 0 && highestPrice > 0) {
            builder.setRangePrice(lowestPrice, highestPrice);
        }
        if (withPhotoOnly) {
            builder.setWithPhotoOnly();
        }

        String requestJson = gson.toJson(builder.build());
        utilityService.getServerTimestamp()
                .subscribeOn(processScheduler).observeOn(androidScheduler)
                .timeout(30, TimeUnit.SECONDS)
                .doOnError(throwable -> {
                    Log.e(TAG, throwable.getLocalizedMessage(), throwable);
                    updateLoadingStatus(false);
                })
                .doOnSuccess(timestamp -> {
                    newRequestRef = requestReference.push();
                    newRequestRef.setValue(new ElasticsearchRequest(timestamp, requestJson));
                    newRequestRef.addValueEventListener(requestValueListener);
                    delayBeforeDeleteRequest();
                })
                .subscribe();
    }

    /**
     * Création d'un délai 30 sec pour supprimer la requête si elle n'a rien retournée.
     */
    private void delayBeforeDeleteRequest() {
        obsDelay.observeOn(androidScheduler)
                .doOnNext(aLong -> {
                    newRequestRef.removeValue();
                    updateLoadingStatus(false);
                })
                .subscribe();
    }

    public LiveData<List<AnnonceFull>> getFavoritesByUidUser(String uidUtilisateur) {
        return annonceFullRepository.findFavoritesByUidUser(uidUtilisateur);
    }

    private ElasticsearchQueryBuilder initQueryBuilder(int pagingSize, int direction, int from, int tri) {
        String directionStr;
        if (direction == ASC) {
            directionStr = "asc";
        } else {
            directionStr = "desc";
        }

        String field;
        if (tri == SORT_TITLE) {
            field = FIELD_TITRE_SORT;
        } else if (tri == SORT_PRICE) {
            field = FILED_PRIX;
        } else {
            field = FIELD_DATE_PUBLICATION;
        }

        ElasticsearchQueryBuilder builder = new ElasticsearchQueryBuilder();
        builder.setSize(pagingSize)
                .setFrom(from)
                .addSortingFields(field, directionStr);

        return builder;
    }
}
