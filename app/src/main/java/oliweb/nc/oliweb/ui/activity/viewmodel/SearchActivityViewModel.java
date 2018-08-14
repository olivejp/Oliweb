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
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.dagger.component.DaggerFirebaseRepositoriesComponent;
import oliweb.nc.oliweb.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.dagger.component.FirebaseRepositoriesComponent;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUtility;
import oliweb.nc.oliweb.network.ElasticsearchQueryBuilder;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.network.elasticsearchDto.ElasticsearchResult;
import oliweb.nc.oliweb.utility.MediaUtility;

import static oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel.AddRemoveFromFavorite.ADD_SUCCESSFUL;
import static oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel.AddRemoveFromFavorite.ONE_OF_YOURS;
import static oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel.AddRemoveFromFavorite.REMOVE_FAILED;
import static oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel.AddRemoveFromFavorite.REMOVE_SUCCESSFUL;
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
        ADD_SUCCESSFUL
    }

    private GenericTypeIndicator<ElasticsearchResult<AnnonceDto>> genericClassDetail;

    private ArrayList<AnnoncePhotos> listAnnonce;
    private MutableLiveData<ArrayList<AnnoncePhotos>> liveListAnnonce;
    private DatabaseReference newRequestRef;
    private MutableLiveData<AtomicBoolean> loading;
    private AnnonceRepository annonceRepository;
    private PhotoRepository photoRepository;
    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
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
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().build();
        annonceRepository = component.getAnnonceRepository();
        photoRepository = component.getPhotoRepository();
        annonceWithPhotosRepository = component.getAnnonceWithPhotosRepository();
        requestReference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_REQUEST_REF);
        listAnnonce = new ArrayList<>();
        genericClassDetail = new GenericTypeIndicator<ElasticsearchResult<AnnonceDto>>() {
        };
    }

    public Single<AnnonceEntity> saveToFavorite(String uidUser, AnnoncePhotos annoncePhotos) {
        Log.d(TAG, "Starting saveToFavorite called with annoncePhotos = " + annoncePhotos.toString());
        return annonceRepository.saveToFavorite(getApplication().getApplicationContext(), uidUser, annoncePhotos);
    }

    public Single<AtomicBoolean> removeFromFavorite(String uidUser, AnnoncePhotos annoncePhotos) {
        Log.d(TAG, "Starting removeFromFavorite called with annoncePhotos = " + annoncePhotos.toString());
        return Single.create(emitter ->
                annonceWithPhotosRepository.findFavoriteAnnonceByUidAnnonce(uidUser, annoncePhotos.getAnnonceEntity().getUid())
                        .doOnError(emitter::onError)
                        .doOnSuccess(annoncePhotos1 -> {
                                    if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
                                        // Suppression de toutes les photos
                                        for (PhotoEntity photo : annoncePhotos1.getPhotos()) {
                                            // Suppression du device
                                            MediaUtility.deletePhotoFromDevice(getApplication().getContentResolver(), photo);

                                            // Suppression de la DB
                                            photoRepository.delete(photo);
                                        }
                                    }
                                    Log.d(TAG, "Starting removeFromFavorite with uidUser : " + uidUser + " annoncePhotos1 : " + annoncePhotos1);
                                    annonceRepository.removeFromFavorite(uidUser, annoncePhotos1);
                                    emitter.onSuccess(new AtomicBoolean(true));
                                }
                        )
                        .subscribe()
        );
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

    public Single<AddRemoveFromFavorite> addOrRemoveFromFavorite(String uidCurrentUser, AnnoncePhotos annoncePhotos) {
        return new Single<AddRemoveFromFavorite>() {
            @Override
            protected void subscribeActual(SingleObserver<? super AddRemoveFromFavorite> observer) {
                if (annoncePhotos.getAnnonceEntity().getUidUser().equals(uidCurrentUser)) {
                    observer.onSuccess(ONE_OF_YOURS);
                } else {
                    if (annoncePhotos.getAnnonceEntity().getFavorite() == 1) {
                        removeFromFavorite(uidCurrentUser, annoncePhotos)
                                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                                .doOnError(observer::onError)
                                .doOnSuccess(atomicBoolean -> observer.onSuccess(atomicBoolean.get() ? REMOVE_SUCCESSFUL : REMOVE_FAILED))
                                .subscribe();
                    } else {
                        saveToFavorite(uidCurrentUser, annoncePhotos)
                                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                                .doOnError(observer::onError)
                                .doOnSuccess(annonceEntity -> observer.onSuccess(ADD_SUCCESSFUL))
                                .subscribe();
                    }
                }
            }
        };
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
