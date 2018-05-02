package oliweb.nc.oliweb.service.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

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

import static oliweb.nc.oliweb.database.entity.StatusRemote.FAILED_TO_SEND;
import static oliweb.nc.oliweb.database.entity.StatusRemote.TO_SEND;
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
                .getAllAnnoncesByStatus(TO_SEND, FAILED_TO_SEND)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .doOnSuccess(this::sendAnnonceToFirebaseDatabase)
                .subscribe();
    }

    /**
     * Create/update an annonce to Firebase from an AnnonceFull from the Database
     * If operation succeed we try to send the photo of this AnnonceFull.
     *
     * @param annonceFulls to send to Firebase
     */
    private void sendAnnonceToFirebaseDatabase(List<AnnonceFull> annonceFulls) {
        Log.d(TAG, "Starting sendAnnonceToFirebaseDatabase annonceFulls : " + annonceFulls);
        for (AnnonceFull annonceFull : annonceFulls) {
            Log.d(TAG, "Tentative d'envoi d'une annonce : " + annonceFull.toString());
            AnnonceDto annonceDto = AnnonceConverter.convertEntityToDto(annonceFull);
            firebaseAnnonceRepository.saveAnnonceToFirebase(annonceDto)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnSuccess(annonceFbSaved -> {
                        Log.d(TAG, "saveAnnonceToFirebase.doOnSuccess annonceFbSaved : " + annonceFbSaved);
                        annonceFull.getAnnonce().setDatePublication(annonceFbSaved.getDatePublication());
                        updateAndSaveAnnonceToLocalDb(annonceFull.getAnnonce());
                    })
                    .doOnError(saveToFirebaseException -> {
                        Log.d(TAG, "saveAnnonceToFirebase.doOnError saveToFirebaseException : " + saveToFirebaseException.getLocalizedMessage(), saveToFirebaseException);
                        annonceFull.getAnnonce().setStatut(StatusRemote.FAILED_TO_SEND);
                        annonceRepository.update(annonceFull.getAnnonce());
                    })
                    .subscribe();
        }
    }

    private void updateAndSaveAnnonceToLocalDb(AnnonceEntity annonceEntity) {
        Log.d(TAG, "Starting updateAndSaveAnnonceToLocalDb annonceDto : " + annonceEntity);
        annonceEntity.setStatut(StatusRemote.SEND);
        annonceRepository.saveWithSingle(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(annonceEntitySaved -> sendPhotosToFbStorageByIdAnnonce(annonceEntitySaved.getIdAnnonce()))
                .doOnError(saveSingleException -> Log.e(TAG, saveSingleException.getLocalizedMessage(), saveSingleException))
                .subscribe();
    }

    /**
     * List all the photos for an Annonce and try to send them to Fb Storage
     *
     * @param idAnnonce of the AnnonceFull we try to send to Fb
     */
    private void sendPhotosToFbStorageByIdAnnonce(long idAnnonce) {
        Log.d(TAG, "Starting sendPhotosToFbStorageByIdAnnonce");
        photoRepository
                .getAllPhotosByStatusAndIdAnnonce(idAnnonce, TO_SEND, FAILED_TO_SEND)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(listPhoto -> {
                    if (listPhoto != null && !listPhoto.isEmpty()) {
                        for (PhotoEntity photo : listPhoto) {
                            sendPhotoToFirebaseStorage(photo);
                        }
                    }
                })
                .subscribe();
    }

    /**
     * We send the content of the file to Fb Storage
     * If succeed, we will receive the Download Path of the image.
     * This path should be send to update the AnnonceFull on Firebase Database
     *
     * @param photo PhotoEntity to send to Firebase Storage
     */
    private void sendPhotoToFirebaseStorage(PhotoEntity photo) {
        Log.d(TAG, "Sending " + photo.getUriLocal() + " to Firebase storage");

        this.photoStorage.sendToRemote(photo)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> {
                    Log.e(TAG, exception.getLocalizedMessage(), exception);
                    photo.setStatut(StatusRemote.FAILED_TO_SEND);
                    photoRepository.save(photo);
                })
                .doOnSuccess(downloadPath -> {
                    photo.setFirebasePath(downloadPath);
                    photoRepository.saveWithSingle(photo)
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
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
                    AnnonceDto annonceDto = AnnonceConverter.convertEntityToDto(annonceFull);

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
     * Delete all photos
     * -on Firebase storage
     * -on device
     * -in local database
     *
     * @param idAnnonce id de l'annonce à supprimer
     */
    private void deletePhotoByIdAnnonce(long idAnnonce) {
        Log.d(TAG, "Starting deletePhotoByIdAnnonce " + idAnnonce);
        photoRepository
                .findAllPhotosByIdAnnonce(idAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(listPhotos -> {

                    for (PhotoEntity photo : listPhotos) {
                        // Suppression de firebase Storage
                        photoStorage.delete(photo).subscribe();

                        // Suppression du device
                        MediaUtility.deletePhotoFromDevice(contentResolver, photo).subscribe();

                        // Suppression de la base locale
                        photoRepository.delete(photo);
                    }
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
                .doOnNext(annonceEntity -> {
                    photoRepository
                            .observeAllPhotosByIdAnnonce(annonceEntity.getIdAnnonce())
                            .doOnNext(photo -> {
                                // 1 - Suppression des photos du firebase Storage
                                photoStorage.delete(photo)
                                        .doOnSuccess(atomicBoolean -> {
                                            if (atomicBoolean.get()) {

                                                // 2 - Suppression de l'annonce de firebase
                                                firebaseAnnonceRepository.delete(annonceEntity)
                                                        .doOnSuccess(deleteResult -> {
                                                            if (deleteResult.get()) {

                                                                // 3 - Suppression du device
                                                                MediaUtility.deletePhotoFromDevice(contentResolver, photo)
                                                                        .doOnSuccess(deleteDeviceResult -> {
                                                                            if (deleteDeviceResult.get()) {

                                                                                // 4 - Suppression de la base locale
                                                                                photoRepository.delete(photo);
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
                            .subscribe();
                })
                .subscribe();
    }

    private void syncDeletePhoto() {
        Log.d(TAG, "Starting syncDeletePhoto");

        // Read all PhotoEntities with TO_DELETE status to delete them on remote storage and local and on Firebase Database
        photoRepository
                .getAllPhotosByStatus(StatusRemote.TO_DELETE.getValue())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(listPhotoToDelete -> {
                    if (listPhotoToDelete != null && !listPhotoToDelete.isEmpty()) {
                        for (PhotoEntity photo : listPhotoToDelete) {
                            if (photo.getFirebasePath() != null) {
                                deletePhotoFromFirebaseStorage(photo, successFirebaseDeleting ->
                                        deletePhotoFromDevice(photo, successDeviceDeleting ->
                                                photoRepository.delete(dataReturn -> {
                                                    if (dataReturn.isSuccessful()) {
                                                        updateAnnonceFullFromDbToFirebase(photo.getIdAnnonce());
                                                    }
                                                }, photo)
                                        )
                                );
                            }
                        }
                    }
                })
                .subscribe();
    }
}
