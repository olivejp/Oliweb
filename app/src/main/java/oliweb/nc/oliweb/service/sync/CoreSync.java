package oliweb.nc.oliweb.service.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.database.entity.StatusRemote.FAILED_TO_SEND;
import static oliweb.nc.oliweb.database.entity.StatusRemote.TO_SEND;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_CHATS_REF;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_MESSAGES_REF;
import static oliweb.nc.oliweb.utility.Constants.notificationSyncAnnonceId;
import static oliweb.nc.oliweb.utility.Constants.notificationSyncPhotoId;

/**
 * Created by orlanth23 on 18/12/2017.
 * <p>
 * This class contains the series of network calls to make to sync local db with firebase
 */
class CoreSync {
    private static final String TAG = CoreSync.class.getName();

    private static CoreSync instance;
    private static FirebaseDatabase fireDb;
    private static StorageReference fireStorage;

    private PhotoRepository photoRepository;
    private AnnonceRepository annonceRepository;
    private AnnonceFullRepository annonceFullRepository;
    private FirebasePhotoStorage photoStorage;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    private int nbAnnonceCompletedTask = 0;
    private int nbPhotoCompletedTask = 0;
    private NotificationCompat.Builder mBuilderAnnonce;
    private NotificationCompat.Builder mBuilderPhoto;
    private NotificationManagerCompat notificationManager;
    private ContentResolver contentResolver;
    private boolean mNotificationAnnonceCreated;
    private boolean mNotificationPhotoCreated;

    private CoreSync() {
    }

    public static CoreSync getInstance(Context context) {
        if (instance == null) {
            instance = new CoreSync();
            fireDb = FirebaseDatabase.getInstance();
            fireStorage = FirebaseStorage.getInstance().getReference();
            instance.photoRepository = PhotoRepository.getInstance(context);
            instance.annonceRepository = AnnonceRepository.getInstance(context);
            instance.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(context);
            instance.annonceFullRepository = AnnonceFullRepository.getInstance(context);
            instance.photoStorage = FirebasePhotoStorage.getInstance();
            instance.notificationManager = NotificationManagerCompat.from(context);
            instance.mBuilderAnnonce = new NotificationCompat.Builder(context, Constants.CHANNEL_ID);
            instance.contentResolver = context.getContentResolver();
        }
        return instance;
    }

    void synchronize() {
        Log.d(TAG, "Launch synchronyse");
        manageNotificationDependingOnAnnonceToSend();
        syncToSend();
        syncToDelete();
    }

    /**
     * Manage notification, depending on count annonce to send in the database
     */
    private void manageNotificationDependingOnAnnonceToSend() {
        mNotificationAnnonceCreated = false;
        mNotificationPhotoCreated = false;
        isAnotherAnnonceWithStatus(TO_SEND, count -> {
            if (count > 0 && !mNotificationAnnonceCreated) {
                createNotification();
                mNotificationAnnonceCreated = true;
            }
            if (mNotificationAnnonceCreated) {
                if (count == 0) {
                    mBuilderAnnonce.setContentText("Téléchargement terminé").setProgress(0, 0, false);
                    notificationManager.notify(notificationSyncAnnonceId, mBuilderAnnonce.build());
                    notificationManager.cancel(notificationSyncAnnonceId);
                } else {
                    updateProgressBar(count, nbAnnonceCompletedTask++, notificationSyncAnnonceId);
                }
            }
        });

        isAnotherPhotoWithStatus(TO_SEND, countPhoto -> {
            if (countPhoto > 0 && !mNotificationPhotoCreated) {
                createNotification();
                mNotificationPhotoCreated = true;
            }
            if (mNotificationPhotoCreated) {
                if (countPhoto == 0) {
                    mBuilderPhoto.setContentText("Téléchargement terminé").setProgress(0, 0, false);
                    notificationManager.notify(notificationSyncPhotoId, mBuilderPhoto.build());
                    notificationManager.cancel(notificationSyncPhotoId);
                } else {
                    updateProgressBar(countPhoto, nbPhotoCompletedTask++, notificationSyncPhotoId);
                }
            }
        });
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
        nbAnnonceCompletedTask = 0;
        nbPhotoCompletedTask = 0;
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

        this.photoStorage.savePhotoToStorage(photo)
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
                .doOnSuccess(this::deletePhotosFromMultiService)
                .subscribe();
    }

    private void deletePhotosFromMultiService(List<PhotoEntity> listPhotoEntity) {
        Log.d(TAG, "Starting deletePhotosFromMultiService " + listPhotoEntity);
        deletePhotosFromFirebaseStorage(listPhotoEntity);
        deletePhotosFromDevice(listPhotoEntity);
        deletePhotosFromDatabase(listPhotoEntity);
    }

    private void deletePhotosFromDatabase(List<PhotoEntity> listPhotoEntity) {
        Log.d(TAG, "Starting deletePhotosFromDatabase " + listPhotoEntity);
        for (PhotoEntity photo : listPhotoEntity) {
            photoRepository.delete(photo);
        }
    }

    private void deletePhotoFromDevice(PhotoEntity photoToDelete, @Nullable CustomOnSuccessListener listener) {
        Log.d(TAG, "Starting deletePhotoFromDevice " + photoToDelete);
        // Suppression du fichier physique
        if (contentResolver.delete(Uri.parse(photoToDelete.getUriLocal()), null, null) != 0) {
            Log.d(TAG, "Successful deleting physical photo : " + photoToDelete.getUriLocal());
            if (listener != null) {
                listener.run(true);
            }
        } else {
            Log.e(TAG, "Fail to delete physical photo : " + photoToDelete.getUriLocal());
            if (listener != null) {
                listener.run(false);
            }
        }
    }

    private void deletePhotosFromDevice(List<PhotoEntity> listPhotoToDelete) {
        Log.d(TAG, "Starting deletePhotosFromDevice " + listPhotoToDelete);
        if (listPhotoToDelete != null && !listPhotoToDelete.isEmpty()) {
            for (PhotoEntity photo : listPhotoToDelete) {
                deletePhotoFromDevice(photo, null);
            }
        }
    }

    private void deletePhotoFromFirebaseStorage(PhotoEntity photoEntity, @Nullable CustomOnSuccessListener listener) {
        Log.d(TAG, "Starting deletePhotoFromFirebaseStorage " + photoEntity.toString());
        // Si un chemin firebase est trouvé, on va également supprimer sur Firebase.
        if (photoEntity.getFirebasePath() != null) {
            // Suppression firebase ... puis suppression dans la DB
            File file = new File(photoEntity.getUriLocal());
            String fileName = file.getName();
            StorageReference storageReference = fireStorage.child(fileName);
            storageReference.delete()
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Successful deleting photo on Firebase Storage : " + fileName);
                        if (listener != null) {
                            listener.run(true);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete image on Firebase Storage : " + e.getMessage());
                        if (listener != null) {
                            listener.run(false);
                        }
                    });
        }
    }

    /**
     * Suppression d'une liste de Media présents sur notre Storage Firebase
     *
     * @param listPhotoEntity List des photos à supprimer de Firebase Storage
     */
    private void deletePhotosFromFirebaseStorage(List<PhotoEntity> listPhotoEntity) {
        Log.d(TAG, "Starting deletePhotosFromFirebaseStorage " + listPhotoEntity);
        if (listPhotoEntity != null && !listPhotoEntity.isEmpty()) {
            for (PhotoEntity photo : listPhotoEntity) {
                deletePhotoFromFirebaseStorage(photo, null);
            }
        }
    }

    /**
     * Read all annonces with TO_DELETE status
     */
    private void syncToDelete() {
        Log.d(TAG, "Starting syncToDelete");
        syncDeleteAnnonce();
        syncDeletePhoto();
    }

    private void syncDeleteAnnonce() {
        Log.d(TAG, "Starting syncDeleteAnnonce");

        annonceRepository
                .getAllAnnonceByStatus(Utility.allStatusToDelete())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(listAnnoncesToDelete -> {
                    Log.d(TAG, "getAllAnnonceByStatus.doOnSuccess listAnnoncesToDelete : " + listAnnoncesToDelete);
                    if (listAnnoncesToDelete != null && !listAnnoncesToDelete.isEmpty()) {
                        for (AnnonceEntity annonce : listAnnoncesToDelete) {

                            // First delete Annonce on Firebase
                            firebaseAnnonceRepository.delete(annonce)
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .doOnError(e1 -> Log.e(TAG, e1.getLocalizedMessage(), e1))
                                    .doOnSuccess(successDeleteFromFb -> {
                                        if (successDeleteFromFb.get()) {

                                            // Second delete Photo from Firebase
                                            firebasePhotoRepository.deleteByUidAnnonce(annonce.getUUID())
                                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                                    .doOnError(e2 -> Log.e(TAG, e2.getLocalizedMessage(), e2))
                                                    .doOnSuccess(atomicBoolean1 -> {
                                                        if (atomicBoolean1.get()) {

                                                        }
                                                    })
                                                    .subscribe();
                                        }
                                    })
                                    .subscribe();


                            photoRepository.deleteAllByIdAnnonce(annonce.getIdAnnonce())
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .doOnError(exceptionDeletePhotos -> Log.e(TAG, "deleteAllByIdAnnonce.doOnError " + exceptionDeletePhotos.getLocalizedMessage(), exceptionDeletePhotos))
                                    .doOnSuccess(countPhotosDeleted -> {
                                        annonceRepository.delete(annonce);
                                        deletePho(annonce);
                                        deleteAnnonceFromFirebaseDatabase(annonce);
                                    })
                                    .subscribe();


                        }
                    }
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

    private void createNotification() {
        Log.d(TAG, "Starting createNotification");

        mBuilderAnnonce.setContentTitle("Oliweb - Envoi de vos annonces")
                .setContentText("Téléchargement en cours")
                .setSmallIcon(R.drawable.ic_sync_white_48dp)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Issue the initial notification with zero progress
        int progressMax = 100;
        int progressCurrent = 0;
        mBuilderAnnonce.setProgress(progressMax, progressCurrent, false);
    }

    private void updateProgressBar(int max, int current, int notificationId) {
        Log.d(TAG, "Starting updateProgressBar max : " + max + " current : " + current + " notificationId : " + notificationId);

        mBuilderAnnonce.setProgress(max, current, false);
        mBuilderAnnonce.setContentText("Téléchargement en cours - " + current + "/" + max);
        notificationManager.notify(notificationId, mBuilderAnnonce.build());
    }

    private void isAnotherAnnonceWithStatus(StatusRemote statusRemote, OnCheckedListener onCheckedListener) {
        Log.d(TAG, "Starting isAnotherAnnonceWithStatus " + statusRemote.getValue());

        // Read all annonces with statusRemote, when reach 0, then run onCheckedListener
        annonceRepository
                .countFlowableAllAnnoncesByStatus(statusRemote.getValue())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnNext(onCheckedListener::run)
                .subscribe();
    }

    private void isAnotherPhotoWithStatus(StatusRemote statusRemote, OnCheckedListener onCheckedListener) {
        Log.d(TAG, "Starting isAnotherPhotoWithStatus " + statusRemote.getValue());

        // Read all photos with statusRemote, when reach 0, then run onCheckedListener
        photoRepository
                .countFlowableAllPhotosByStatus(statusRemote.getValue())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnNext(onCheckedListener::run)
                .subscribe();
    }

    private interface OnCheckedListener {
        void run(Integer count);
    }

    private interface CustomOnSuccessListener {
        void run(boolean success);
    }
}
