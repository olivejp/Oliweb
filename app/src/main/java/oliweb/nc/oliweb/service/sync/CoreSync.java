package oliweb.nc.oliweb.service.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;

/**
 * Created by orlanth23 on 18/12/2017.
 * <p>
 * This class contains the series of network calls to make to sync local db with firebase
 */
class CoreSync {
    private static final String TAG = CoreSync.class.getName();

    private static CoreSync instance;
    private static FirebaseDatabase fireDb;

    private PhotoRepository photoRepository;
    private AnnonceRepository annonceRepository;
    private AnnonceFullRepository annonceFullRepository;
    private FirebasePhotoStorage photoStorage;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    private ContentResolver contentResolver;

    private CoreSync() {
    }

    public static CoreSync getInstance(Context context) {
        if (instance == null) {
            instance = new CoreSync();
            fireDb = FirebaseDatabase.getInstance();
            instance.photoRepository = PhotoRepository.getInstance(context);
            instance.annonceRepository = AnnonceRepository.getInstance(context);
            instance.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(context);
            instance.annonceFullRepository = AnnonceFullRepository.getInstance(context);
            instance.photoStorage = FirebasePhotoStorage.getInstance(context);
            instance.contentResolver = context.getContentResolver();
        }
        return instance;
    }

    void synchronize() {
        Log.d(TAG, "Launch synchronyse");
        syncToSend();
        syncToDelete();
    }

    /**
     * Liste toutes les annonces à envoyer
     */
    private void syncToSend() {
        Log.d(TAG, "Starting syncToSend");
        annonceFullRepository
                .observeAllAnnoncesByStatus(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .doOnNext(this::sendAnnonceToFirebaseDatabase)
                .subscribe();
    }

    /**
     * Create/update an annonce to Firebase from an AnnonceFull from the Database
     * If operation succeed we try to send the photo of this AnnonceFull.
     *
     * @param annonceFull to send to Firebase
     */
    private void sendAnnonceToFirebaseDatabase(AnnonceFull annonceFull) {
        Log.d(TAG, "Starting sendAnnonceToFirebaseDatabase annonceFull : " + annonceFull);
        AnnonceDto annonceDto = AnnonceConverter.convertFullEntityToDto(annonceFull);
        firebaseAnnonceRepository.saveAnnonceToFirebase(annonceDto)
                .map(annonceDto1 -> {
                    annonceFull.getAnnonce().setDatePublication(annonceDto1.getDatePublication());
                    AnnonceEntity annonceEntity = annonceFull.getAnnonce();
                    annonceEntity.setStatut(StatusRemote.SEND);
                    return annonceEntity;
                })
                .doOnSuccess(this::saveToLocalDb)
                .doOnError(saveToFirebaseException -> {
                    Log.d(TAG, "saveAnnonceToFirebase.doOnError saveToFirebaseException : " + saveToFirebaseException.getLocalizedMessage(), saveToFirebaseException);
                    annonceFull.getAnnonce().setStatut(StatusRemote.FAILED_TO_SEND);
                    annonceRepository.update(annonceFull.getAnnonce());
                })
                .subscribe();
    }

    private void saveToLocalDb(AnnonceEntity annonceEntity) {
        annonceRepository.saveWithSingle(annonceEntity)
                .doOnSuccess(annonceEntitySaved -> {
                    Log.d(TAG, "Starting sendPhotosToFbStorageByIdAnnonce");
                    photoRepository
                            .getAllPhotosByStatusAndIdAnnonce(annonceEntitySaved.getIdAnnonce(), Utility.allStatusToSend())
                            .flattenAsObservable(list -> list)
                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                            .doOnNext(this::savePhoto)
                            .subscribe();
                })
                .doOnError(saveSingleException -> Log.e(TAG, saveSingleException.getLocalizedMessage(), saveSingleException))
                .subscribe();
    }

    private void savePhoto(PhotoEntity photo) {
        Log.d(TAG, "Sending " + photo.getUriLocal() + " to Firebase storage");
        this.photoStorage.sendToRemote(photo)
                .doOnError(exception -> {
                    Log.e(TAG, exception.getLocalizedMessage(), exception);
                    photo.setStatut(StatusRemote.FAILED_TO_SEND);
                    photoRepository.save(photo);
                })
                .doOnSuccess(downloadPath -> {
                    photo.setFirebasePath(downloadPath);
                    photoRepository.saveWithSingle(photo)
                            .doOnError(exception1 -> Log.e(TAG, exception1.getLocalizedMessage(), exception1))
                            .doOnSuccess(photoEntity -> firebaseAnnonceRepository.saveAnnonceToFirebase(photo.getIdAnnonce()))
                            .subscribe();
                })
                .subscribe();
    }

    /**
     * Update the AnnonceFull to the Fb Database.
     * In this method we only send the annonce.
     *
     * @param idAnnonce of the AnnonceFull we want to send.
     */
    private void updateAnnonceFullFromDbToFirebase(long idAnnonce) {
        Log.d(TAG, "Starting updateAnnonceFullFromDbToFirebase " + idAnnonce);
        annonceFullRepository.findAnnoncesByIdAnnonce(idAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(annonceFull -> {
                    DatabaseReference dbRef = fireDb.getReference(FIREBASE_DB_ANNONCE_REF).child(annonceFull.getAnnonce().getUUID());

                    // Conversion de notre annonce en DTO
                    AnnonceDto annonceDto = AnnonceConverter.convertFullEntityToDto(annonceFull);

                    dbRef.setValue(annonceDto)
                            .addOnSuccessListener(o -> {
                                annonceFull.getAnnonce().setStatut(StatusRemote.SEND);
                                annonceRepository.update(annonceFull.getAnnonce());
                            })
                            .addOnFailureListener(e -> {
                                annonceFull.getAnnonce().setStatut(StatusRemote.FAILED_TO_SEND);
                                annonceRepository.update(annonceFull.getAnnonce());
                            });
                })
                .subscribe();
    }

    /**
     * Read all annonces with TO_DELETE status
     */
    private void syncToDelete() {
        Log.d(TAG, "Starting syncToDelete");
        syncDeleteAnnonce();
        syncDeletePhoto();
    }

    /**
     * Lecture de toutes les annonces avec des statuts à supprimer
     * 1 - Suppression des photos du Firebase Storage
     * 2 - Suppression de l'annonce dans Firebase
     * 3 - Suppression des photos dans le storage local
     * 4 - Suppression des photos dans la base locale
     * 5 - Suppression de l'annonce dans la base locale
     */
    private void syncDeleteAnnonce() {
        Log.d(TAG, "Starting syncDeleteAnnonce");
        annonceRepository
                .getAllAnnonceByStatus(Utility.allStatusToDelete())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnNext(annonceEntity ->
                        photoRepository
                                .observeAllPhotosByIdAnnonce(annonceEntity.getIdAnnonce())
                                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                .doOnNext(photo -> {
                                    // 1 - Suppression des photos du firebase Storage
                                    photoStorage.delete(photo)
                                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                            .doOnSuccess(atomicBoolean -> {
                                                if (atomicBoolean.get()) {
                                                    Log.d(TAG, "syncDeleteAnnonce : Delete from Firebase Storage Successful");
                                                    // 2 - Suppression de l'annonce de firebase
                                                    firebaseAnnonceRepository.delete(annonceEntity)
                                                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                                            .doOnSuccess(deleteResult -> {
                                                                if (deleteResult.get()) {
                                                                    Log.d(TAG, "syncDeleteAnnonce : Delete from Firebase Database Successful");
                                                                    // 3 - Suppression du device
                                                                    MediaUtility.deletePhotoFromDevice(contentResolver, photo)
                                                                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                                                            .doOnSuccess(deleteDeviceResult -> {
                                                                                if (deleteDeviceResult.get()) {
                                                                                    Log.d(TAG, "syncDeleteAnnonce : Delete from Local Storage Successful");
                                                                                    // 4 - Suppression de la base locale
                                                                                    photoRepository.delete(datareturn -> {
                                                                                        if (datareturn.isSuccessful()) {
                                                                                            Log.d(TAG, "syncDeleteAnnonce : Delete from Local Database Successful");
                                                                                        } else {
                                                                                            Log.e(TAG, datareturn.getThrowable().getLocalizedMessage(), datareturn.getThrowable());
                                                                                        }
                                                                                    }, photo);
                                                                                }
                                                                            })
                                                                            .subscribe();
                                                                }
                                                            })
                                                            .subscribe();
                                                }
                                            })
                                            .subscribe();
                                })
                                .subscribe()
                )
                .subscribe();
    }

    /**
     * Lecture de toutes les photos avec un statut "à supprimer"
     * Pour chaque photo, je vais tenter de :
     * 1 - Supprimer sur Firebase Storage
     * 2 - Supprimer sur Firebase Database (mise à jour de l'annonce)
     * 3 - Supprimer sur le storage local
     * 4 - Supprimer sur la database locale
     */
    private void syncDeletePhoto() {
        Log.d(TAG, "Starting syncDeletePhoto");

        // Read all PhotoEntities with TO_DELETE status to delete them on remote storage and local and on Firebase Database
        photoRepository
                .observeAllPhotosByStatus(StatusRemote.TO_DELETE.getValue())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnNext(photoEntity -> {
                    if (photoEntity.getFirebasePath() != null) {
                        // 1 - Supprimer de Firebase storage
                        photoStorage.delete(photoEntity)
                                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                .doOnSuccess(list -> {
                                    // 2 - Supprimer de Firebase database (mise à jour de l'annonce)
                                    annonceRepository.findById(photoEntity.getIdAnnonce())
                                })
                                .subscribe();
                    }
                })
                .subscribe();
    }
}
