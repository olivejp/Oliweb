package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.PhotoRepository;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.service.sync.FirebaseSync;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MyAnnoncesViewModel extends AndroidViewModel {

    private static final String TAG = MyAnnoncesViewModel.class.getName();

    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private AnnonceRepository annonceRepository;
    private PhotoRepository photoRepository;
    private boolean questionAsked;
    private MutableLiveData<AtomicBoolean> isAnnoncesAvailableToSync;

    public MyAnnoncesViewModel(@NonNull Application application) {
        super(application);
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
        photoRepository = PhotoRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<List<AnnoncePhotos>> findActiveAnnonceByUidUtilisateur(String uuidUtilisateur) {
        return annonceWithPhotosRepository.findActiveAnnonceByUidUser(uuidUtilisateur);
    }

    public LiveData<AtomicBoolean> retrieveAnnoncesFromFirebase(final String uidUtilisateur) {
        isAnnoncesAvailableToSync = new MutableLiveData<>();
        isAnnoncesAvailableToSync.setValue(new AtomicBoolean(false));
        FirebaseSync firebaseSync = FirebaseSync.getInstance(getApplication().getApplicationContext());
        firebaseSync.getAllAnnonceFromFirebaseByUidUser(uidUtilisateur)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                            HashMap<String, AnnonceDto> mapAnnonceSearchDto = dataSnapshot.getValue(FirebaseSync.genericClass);
                            if (mapAnnonceSearchDto != null && !mapAnnonceSearchDto.isEmpty()) {
                                questionAsked = false;
                                for (Map.Entry<String, AnnonceDto> entry : mapAnnonceSearchDto.entrySet()) {
                                    if (questionAsked) {
                                        break;
                                    }
                                    firebaseSync.existInLocalByUidUserAndUidAnnonce(uidUtilisateur, entry.getValue().getUuid())
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(Schedulers.io())
                                            .subscribe(integer -> {
                                                if ((integer == null || integer.equals(0)) && !questionAsked) {
                                                    questionAsked = true;
                                                    isAnnoncesAvailableToSync.postValue(new AtomicBoolean(true));
                                                }
                                            });

                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d(TAG, "onCancelled");
                    }
                });
        return isAnnoncesAvailableToSync;
    }

    /**
     * Update annonce and photo status to TO_DELETE
     * CoreSync will do the trick.
     *
     * @param idAnnonce
     * @param onRespositoryPostExecute
     */
    public Disposable deleteAnnonceById(long idAnnonce, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        return this.annonceRepository.findSingleById(idAnnonce)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(annonceEntity -> {

                    // Update annonce status
                    annonceEntity.setStatut(StatusRemote.TO_DELETE);
                    this.annonceRepository.update(onRespositoryPostExecute, annonceEntity);

                    // Update photo status
                    this.photoRepository.findAllSingleByIdAnnonce(annonceEntity.getIdAnnonce())
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .subscribe(photoEntities -> {
                                for (PhotoEntity photoEntity : photoEntities) {
                                    photoEntity.setStatut(StatusRemote.TO_DELETE);
                                    photoRepository.update(dataReturn -> {
                                        if (dataReturn.getNb() != 0) {
                                            Log.d(TAG, "PhotoEntity successfully updated TO_DELETE");
                                        }
                                    }, photoEntity);
                                }
                            });


                });
    }
}
