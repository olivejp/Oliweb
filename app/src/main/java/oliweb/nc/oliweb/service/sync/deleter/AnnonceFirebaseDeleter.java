package oliweb.nc.oliweb.service.sync.deleter;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;
import oliweb.nc.oliweb.utility.MediaUtility;

/**
 * Cette classe découpe toutes les étapes nécessaires pour l'envoi d'une annonce sur Firebase
 */
public class AnnonceFirebaseDeleter {

    private static final String TAG = AnnonceFirebaseDeleter.class.getName();

    private static AnnonceFirebaseDeleter instance;

    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    private AnnonceRepository annonceRepository;
    private PhotoRepository photoRepository;
    private AnnonceFullRepository annonceFullRepository;
    private FirebasePhotoStorage firebasePhotoStorage;
    private ContentResolver contentResolver;

    private AnnonceFirebaseDeleter() {
    }

    public static AnnonceFirebaseDeleter getInstance(Context context) {
        if (instance == null) {
            instance = new AnnonceFirebaseDeleter();
            instance.annonceRepository = AnnonceRepository.getInstance(context);
            instance.photoRepository = PhotoRepository.getInstance(context);
            instance.annonceFullRepository = AnnonceFullRepository.getInstance(context);
            instance.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(context);
            instance.firebasePhotoStorage = FirebasePhotoStorage.getInstance(context);
            instance.contentResolver = context.getContentResolver();
        }
        return instance;
    }

    /**
     * Récupération de l'annonce full
     * Pour chaque annonceFull, je vais supprimer les photos
     * Puis supprimer l'annonce de Firebase et de la base de données locale
     */
    public void processToDeleteAnnonce(AnnonceEntity annonceEntity) {
        Log.d(TAG, "Starting processToDeleteAnnonce annonceEntity : " + annonceEntity);
        annonceFullRepository.findAnnonceFullByAnnonceEntity(annonceEntity)
                .doOnNext(annonceFull -> deleteAllPhotos(annonceFull.getPhotos()))
                .switchMap(this::deleteAnnonceAllService)
                .subscribe();
    }

    /**
     * Suppression de Firebase Database
     * Suppression de la DB locale
     *
     * @param annonceFull
     * @return
     */
    private Observable<AtomicBoolean> deleteAnnonceAllService(AnnonceFull annonceFull) {
        Log.d(TAG, "Starting deleteAnnonceAllService annonceFull : " + annonceFull);
        return firebaseAnnonceRepository.delete(annonceFull.getAnnonce())
                .doOnError(exception -> {
                    Log.e(TAG, "Failed to delete from the Firebase Database => " + exception.getLocalizedMessage(), exception);
                    annonceRepository.markAsFailedToDelete(annonceFull.getAnnonce()).subscribe();
                })
                .doOnSuccess(atomicBoolean -> {
                    if (atomicBoolean.get()) {
                        annonceRepository.delete(dataReturn -> {
                            if (dataReturn.isSuccessful()) {
                                Log.d(TAG, "Delete from the local DB successful");
                            } else {
                                Log.e(TAG, "Fail to delete from the local DB");
                            }
                        }, annonceFull.getAnnonce());
                    } else {
                        Log.e(TAG, "Failed to delete from the local DB");
                    }
                })
                .toObservable();
    }

    /**
     * Lecture de toutes les photos avec un statut "à supprimer"
     * Pour chaque photo, je vais tenter de :
     * 1 - Supprimer sur Firebase Storage
     * 2 - Supprimer sur le storage local
     * 3 - Supprimer sur la database locale
     */
    private void deleteAllPhotos(List<PhotoEntity> listPhotosToDelete) {
        Log.d(TAG, "Starting deleteAllPhotos");
        for (PhotoEntity photo : listPhotosToDelete) {
            deleteOnePhoto(photo).subscribe();
        }
    }

    public Single<AtomicBoolean> deleteOnePhoto(PhotoEntity photo) {
        // 1 - Suppression du Firebase Storage
        return Single.create(emitter -> firebasePhotoStorage.delete(photo)
                .doOnError(e -> {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                    photoRepository.markAsFailedToDelete(photo).subscribe();
                    emitter.onError(e);
                })
                .doOnSuccess(atomicBoolean -> {
                    if (atomicBoolean.get()) {
                        // 2 - Suppression sur le device
                        if (MediaUtility.deletePhotoFromDevice(contentResolver, photo)) {
                            Log.d(TAG, "Successful delete from local device");
                        } else {
                            Log.e(TAG, "Failed to delete photo from local device");
                        }

                        // 3 - Suppression de la db locale
                        photoRepository.delete(dataReturn -> {
                            if (dataReturn.isSuccessful()) {
                                Log.d(TAG, "Photo delete successful");
                                emitter.onSuccess(new AtomicBoolean(true));
                            } else {
                                Log.e(TAG, "Photo delete FAILED");
                                emitter.onSuccess(new AtomicBoolean(false));
                            }
                        }, photo);
                    }
                })
                .subscribe()
        );
    }

}
