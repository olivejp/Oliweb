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
import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUtility;
import oliweb.nc.oliweb.network.ElasticsearchQueryBuilder;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.network.elasticsearchDto.ElasticsearchResult;

import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ASC;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_PRICE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_TITLE;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_REQUEST_REF;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class SearchActivityViewModel extends AndroidViewModel {

    private static final String TAG = SearchActivityViewModel.class.getName();

    private GenericTypeIndicator<ElasticsearchResult<AnnonceDto>> genericClassDetail;

    private ArrayList<AnnoncePhotos> listAnnonce;
    private MutableLiveData<ArrayList<AnnoncePhotos>> liveListAnnonce;
    private DatabaseReference newRequestRef;
    private MutableLiveData<AtomicBoolean> loading;
    private AnnonceRepository annonceRepository;
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
                        ElasticsearchResult<AnnonceDto> elasticsearchResult = child.getValue(genericClassDetail);
                        if (elasticsearchResult != null) {
                            AnnoncePhotos annoncePhotos = AnnonceConverter.convertDtoToAnnoncePhotos(elasticsearchResult.get_source());
                            listAnnonce.add(annoncePhotos);
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
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
        requestReference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_REQUEST_REF);
        listAnnonce = new ArrayList<>();
        genericClassDetail = new GenericTypeIndicator<ElasticsearchResult<AnnonceDto>>() {
        };
    }

    public Single<AnnonceEntity> saveToFavorite(String uidUser, AnnoncePhotos annoncePhotos) {
        return annonceRepository.saveToFavorite(getApplication().getApplicationContext(), uidUser, annoncePhotos);
    }

    public LiveData<ArrayList<AnnoncePhotos>> getLiveListAnnonce() {
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

    public Single<Integer> isAnnonceFavorite(String uidUser, String uidAnnonce) {
        return annonceRepository.isAnnonceFavorite(uidUser, uidAnnonce);
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
}
