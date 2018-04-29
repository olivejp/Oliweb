package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Maybe;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;

/**
 * Created by 2761oli on 06/02/2018.
 */

public class MyAnnoncesViewModel extends AndroidViewModel {

    private static final String TAG = MyAnnoncesViewModel.class.getName();

    private AnnonceWithPhotosRepository annonceWithPhotosRepository;
    private AnnonceRepository annonceRepository;
    private PhotoRepository photoRepository;
    private MutableLiveData<AtomicBoolean> shouldAskQuestion;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    public MyAnnoncesViewModel(@NonNull Application application) {
        super(application);
        annonceWithPhotosRepository = AnnonceWithPhotosRepository.getInstance(application.getApplicationContext());
        annonceRepository = AnnonceRepository.getInstance(application.getApplicationContext());
        photoRepository = PhotoRepository.getInstance(application.getApplicationContext());
        firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(application.getApplicationContext());
    }

    public LiveData<List<AnnoncePhotos>> findActiveAnnonceByUidUtilisateur(String uuidUtilisateur) {
        return annonceWithPhotosRepository.findActiveAnnonceByUidUser(uuidUtilisateur);
    }

    // TODO : Peut faire mieux
    public LiveData<AtomicBoolean> shouldIAskQuestionToRetreiveData(@Nullable String uidUtilisateur) {
        Log.d(TAG, "Starting shouldIAskQuestionToRetreiveData uidUtilisateur : " + uidUtilisateur);
        if (shouldAskQuestion == null) {
            shouldAskQuestion = new MutableLiveData<>();
        }
        shouldAskQuestion.setValue(new AtomicBoolean(false));

        if (uidUtilisateur != null) {
            firebaseAnnonceRepository.checkFirebaseRepository(uidUtilisateur, shouldAskQuestion);
        }

        return shouldAskQuestion;
    }

    /**
     * Update annonce and photo status to TO_DELETE
     * CoreSync will do the trick.
     *
     * @param idAnnonce
     */
    public Maybe<AtomicBoolean> deleteAnnonceById(long idAnnonce) {
        Log.d(TAG, "Starting deleteAnnonceById idAnnonce : " + idAnnonce);
        return Maybe.create(emitter ->
                this.annonceRepository.findSingleById(idAnnonce)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(annonceEntity -> {
                            Log.d(TAG, "findSingleById.doOnSuccess annonceEntity : " + annonceEntity);
                            annonceEntity.setStatut(StatusRemote.TO_DELETE);
                            annonceRepository.saveWithSingle(annonceEntity)
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .doOnError(emitter::onError)
                                    .doOnSuccess(annonceUpdated -> {
                                                Log.d(TAG, "saveWithSingle.doOnSuccess annonceUpdated : " + annonceUpdated);
                                                photoRepository.findAllPhotosByIdAnnonce(annonceUpdated.getIdAnnonce())
                                                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                                        .doOnSuccess(this::updateAllPhotosToDelete)
                                                        .subscribe();
                                            }
                                    )
                                    .subscribe();
                        })
                        .subscribe()
        );
    }

    private void updateAllPhotosToDelete(List<PhotoEntity> photoEntities) {
        Log.d(TAG, "Starting updateAllPhotosToDelete photoEntities : " + photoEntities);

        // Condition de garde
        if (photoEntities == null || photoEntities.isEmpty()) {
            return;
        }
        for (PhotoEntity photoEntity : photoEntities) {
            photoEntity.setStatut(StatusRemote.TO_DELETE);
        }
        photoRepository.saveWithSingle(photoEntities)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }
}
