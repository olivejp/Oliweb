package oliweb.nc.oliweb.service;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.PhotoRepository;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_ANNONCE_REF;
import static oliweb.nc.oliweb.Constants.notificationSyncAnnonceId;

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

    private int nbAnnonceCompletedTask = 0;
    private NotificationCompat.Builder mBuilder;
    private NotificationManagerCompat notificationManager;
    private ContentResolver contentResolver;
    private boolean mNotificationCreated;

    private CoreSync() {
    }

    public static CoreSync getInstance(Context context) {
        if (instance == null) {
            instance = new CoreSync();
            fireDb = FirebaseDatabase.getInstance();
            fireStorage = FirebaseStorage.getInstance().getReference();
            instance.photoRepository = PhotoRepository.getInstance(context);
            instance.annonceRepository = AnnonceRepository.getInstance(context);
            instance.annonceFullRepository = AnnonceFullRepository.getInstance(context);
            instance.notificationManager = NotificationManagerCompat.from(context);
            instance.mBuilder = new NotificationCompat.Builder(context, Constants.CHANNEL_ID);
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
        mNotificationCreated = false;
        isAnotherAnnonceWithStatus(StatusRemote.TO_SEND, count -> {
            if (count > 0 && !mNotificationCreated) {
                createNotification("Oliweb - Envoi de vos annonces");
                mNotificationCreated = true;
            }

            if (mNotificationCreated) {
                if (count == 0) {
                    mBuilder.setContentText("Téléchargement terminé").setProgress(0, 0, false);
                    notificationManager.notify(notificationSyncAnnonceId, mBuilder.build());
                    notificationManager.cancel(notificationSyncAnnonceId);
                } else {
                    updateProgressBar(count, nbAnnonceCompletedTask++, notificationSyncAnnonceId);
                }
            }
        });
    }

    /**
     * Liste toutes les annonces à envoyer
     */
    private Disposable syncToSend() {
        return annonceFullRepository
                .getAllAnnoncesByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(annoncesFulls -> {
                    if (annoncesFulls != null && !annoncesFulls.isEmpty()) {
                        nbAnnonceCompletedTask = 0;
                        for (AnnonceFull annonceFull : annoncesFulls) {
                            Log.d(TAG, "Tentative d'envoi d'une annonce : " + annonceFull.toString());
                            sendAnnonceToFirebaseDatabase(annonceFull);
                        }
                    }
                });
    }

    /**
     * Create/update an annonce to Firebase from an AnnonceFull from the Database
     * If operation succeed we try to send the photo of this AnnonceFull.
     *
     * @param annonceFull to send to Firebase
     */
    private void sendAnnonceToFirebaseDatabase(AnnonceFull annonceFull) {
        DatabaseReference dbRef;
        if (annonceFull.getAnnonce().getUUID() != null) {
            // Update
            dbRef = fireDb.getReference(FIREBASE_DB_ANNONCE_REF).child(annonceFull.getAnnonce().getUUID());
        } else {
            // Create
            dbRef = fireDb.getReference(FIREBASE_DB_ANNONCE_REF).push();
            annonceFull.getAnnonce().setUUID(dbRef.getKey());
        }

        AnnonceDto annonceDto = AnnonceConverter.convertEntityToDto(annonceFull);

        dbRef.setValue(annonceDto)
                .addOnSuccessListener(o -> {
                    annonceFull.getAnnonce().setStatut(StatusRemote.SEND);
                    annonceRepository.update(dataReturn -> {
                        if (dataReturn.isSuccessful()) {
                            sendPhotosToFbStorage(annonceFull.annonce.getIdAnnonce());
                        }
                    }, annonceFull.getAnnonce());
                })
                .addOnFailureListener(e -> {
                    annonceFull.getAnnonce().setStatut(StatusRemote.FAILED_TO_SEND);
                    annonceRepository.update(annonceFull.getAnnonce());
                });
    }

    /**
     * List all the photos for an Annonce and try to send them to Fb Storage
     *
     * @param idAnnonce of the AnnonceFull we try to send to Fb
     */
    private Disposable sendPhotosToFbStorage(long idAnnonce) {
        Log.d(TAG, "Starting sendPhotosToFbStorage");
        return photoRepository
                .getAllPhotosByStatusAndIdAnnonce(StatusRemote.TO_SEND.getValue(), idAnnonce)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(listPhoto -> {
                    if (listPhoto != null && !listPhoto.isEmpty()) {
                        for (PhotoEntity photo : listPhoto) {
                            sendPhotoToFirebaseStorage(photo);
                        }
                    }
                });
    }

    /**
     * We send the content of the file to Fb Storage
     * If succeed, we will receive the Download Path of the image.
     * This path should be send to update the AnnonceFull on Firebase Database
     *
     * @param photo
     */
    private void sendPhotoToFirebaseStorage(PhotoEntity photo) {
        Log.d(TAG, "Sending " + photo.getUriLocal() + " to Firebase storage");
        File file = new File(photo.getUriLocal());
        String fileName = file.getName();
        StorageReference storageReference = fireStorage.child(fileName);
        storageReference.putFile(Uri.parse(photo.getUriLocal()))
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Succeed to send the photo to Fb Storage : " + taskSnapshot.getDownloadUrl());
                    photo.setStatut(StatusRemote.SEND);
                    if (taskSnapshot.getDownloadUrl() != null) {
                        photo.setFirebasePath(taskSnapshot.getDownloadUrl().toString());
                    }
                    photoRepository.save(photo, dataReturn -> {
                        Log.d(TAG, "Succeed to save URL path for the photo");
                        if (dataReturn.isSuccessful()) {
                            updateAnnonceFullFromDbToFirebase(photo.getIdAnnonce());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image");
                    photo.setStatut(StatusRemote.FAILED_TO_SEND);
                    photoRepository.save(photo, null);
                });
    }

    /**
     * Update the AnnonceFull to the Fb Database.
     * In this method we only send the annonce.
     *
     * @param idAnnonce of the AnnonceFull we want to send.
     */
    private Disposable updateAnnonceFullFromDbToFirebase(long idAnnonce) {
        return annonceFullRepository.findAnnoncesByIdAnnonce(idAnnonce)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(annonceFull -> {
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
                });
    }

//    private void syncPhotosToSend() {
//        Log.d(TAG, "Starting syncPhotosToSend");
//        // Read all photos with TO_SEND status to send them
//        photoRepository
//                .getAllPhotosByStatus(StatusRemote.TO_SEND.getValue())
//                .subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .subscribe(listPhoto -> {
//                    if (listPhoto != null && !listPhoto.isEmpty()) {
//                        for (PhotoEntity photo : listPhoto) {
//                            sendPhotoToFirebaseStorage(photo, listPhoto.size());
//                        }
//                    }
//                });
//    }

//    private void sendPhotoToFirebaseStorage(PhotoEntity photo, int totalSize) {
//        Log.d(TAG, "Sending " + photo.getUriLocal() + " to Firebase storage");
//
//        OnCheckedListener onCheckedListener = count -> {
//            if (count == null || count == 0) {
//                mBuilder.setContentText("Téléchargement terminé").setProgress(0, 0, false);
//                notificationManager.notify(notificationSyncPhotoId, mBuilder.build());
//                notificationManager.cancel(notificationSyncPhotoId);
//            }
//        };
//
//        File file = new File(photo.getUriLocal());
//        String fileName = file.getName();
//        StorageReference storageReference = fireStorage.child(fileName);
//        storageReference.putFile(Uri.parse(photo.getUriLocal()))
//                .addOnSuccessListener(taskSnapshot -> {
//                    photo.setStatut(StatusRemote.SEND);
//                    if (taskSnapshot.getDownloadUrl() != null) {
//                        photo.setFirebasePath(taskSnapshot.getDownloadUrl().toString());
//                    }
//                    photoRepository.save(photo, dataReturn -> isAnotherPhotoWithStatus(StatusRemote.TO_SEND, onCheckedListener));
//                    updateProgressBar(totalSize, nbPhotoCompletedTask++, notificationSyncPhotoId);
//                })
//                .addOnFailureListener(e -> {
//                    Log.e(TAG, "Failed to upload image");
//                    photo.setStatut(StatusRemote.FAILED_TO_SEND);
//                    photoRepository.save(photo, dataReturn -> isAnotherPhotoWithStatus(StatusRemote.TO_SEND, onCheckedListener));
//                    updateProgressBar(totalSize, nbPhotoCompletedTask++, notificationSyncPhotoId);
//                });
//    }

    /**
     * Delete all photos
     * -on Firebase storage
     * -on device
     * -in local database
     *
     * @param idAnnonce
     */
    private Disposable deletePhotoByIdAnnonce(long idAnnonce) {
        return photoRepository
                .findAllSingleByIdAnnonce(idAnnonce)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(this::deletePhotosFromMultiService);
    }

    private void deletePhotosFromMultiService(List<PhotoEntity> listPhotoEntity) {
        deletePhotosFromFirebaseStorage(listPhotoEntity);
        deletePhotosFromDevice(listPhotoEntity);
        deletePhotosFromDatabase(listPhotoEntity);
    }

    private void deletePhotosFromDatabase(List<PhotoEntity> listPhotoEntity) {
        for (PhotoEntity photo : listPhotoEntity) {
            photoRepository.delete(photo);
        }
    }

    private void deletePhotoFromDevice(PhotoEntity photoToDelete, @Nullable CustomOnSuccessListener listener) {
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
        if (listPhotoToDelete != null && !listPhotoToDelete.isEmpty()) {
            for (PhotoEntity photo : listPhotoToDelete) {
                deletePhotoFromDevice(photo, null);
            }
        }
    }

    private void deletePhotoFromFirebaseStorage(PhotoEntity photoEntity, @Nullable CustomOnSuccessListener listener) {
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
     * @param listPhotoEntity
     */
    private void deletePhotosFromFirebaseStorage(List<PhotoEntity> listPhotoEntity) {
        if (listPhotoEntity != null && !listPhotoEntity.isEmpty()) {
            for (PhotoEntity photo : listPhotoEntity) {
                deletePhotoFromFirebaseStorage(photo, null);
            }
        }
    }

    /**
     * Suppression d'une annonce sur Firebase Database
     * La suppression est basée sur l'UID de l'annonce.
     *
     * @param annonce
     */
    private void deleteAnnonceFromFirebaseDatabase(AnnonceEntity annonce) {
        // Si un chemin firebase est trouvé, on va également supprimer sur Firebase.
        fireDb.getReference(FIREBASE_DB_ANNONCE_REF)
                .child(annonce.getUUID())
                .removeValue()
                .addOnFailureListener(e -> Log.e(TAG, "Fail to delete annonce on Firebase Database : " + annonce.getUUID()))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Successful delete annonce on Firebase Database : " + annonce.getUUID()));
    }

    /**
     * Read all annonces with TO_DELETE status
     */
    private void syncToDelete() {
        syncDeleteAnnonce();
        syncDeletePhoto();
    }

    private Disposable syncDeleteAnnonce() {
        // Read all annonces with TO_DELETE status to delete them on remote and local
        return annonceRepository
                .getAllAnnonceByStatus(StatusRemote.TO_DELETE.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(listAnnoncesToDelete -> {
                            if (listAnnoncesToDelete != null && !listAnnoncesToDelete.isEmpty()) {
                                for (AnnonceEntity annonce : listAnnoncesToDelete) {

                                    // We delete only when it left 0 photos for this annonce
                                    photoRepository.countAllPhotosByIdAnnonce(annonce.getIdAnnonce())
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(Schedulers.io())
                                            .subscribe(integer -> {
                                                if (integer == null || integer.equals(0)) {
                                                    annonceRepository.delete(annonce);
                                                }
                                            });

                                    deletePhotoByIdAnnonce(annonce.getIdAnnonce());
                                    deleteAnnonceFromFirebaseDatabase(annonce);
                                }
                            }
                        }
                );
    }

    private Disposable syncDeletePhoto() {
        // Read all PhotoEntities with TO_DELETE status to delete them on remote storage and local and on Firebase Database
        return photoRepository
                .getAllPhotosByStatus(StatusRemote.TO_DELETE.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(listPhotoToDelete -> {
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
                        }
                );
    }

    private void createNotification(String title) {
        mBuilder.setContentTitle(title)
                .setContentText("Téléchargement en cours")
                .setSmallIcon(R.drawable.ic_sync_white_48dp)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Issue the initial notification with zero progress
        int progressMax = 100;
        int progressCurrent = 0;
        mBuilder.setProgress(progressMax, progressCurrent, false);
    }

    private void updateProgressBar(int max, int current, int notificationId) {
        mBuilder.setProgress(max, current, false);
        mBuilder.setContentText("Téléchargement en cours - " + current + "/" + max);
        notificationManager.notify(notificationId, mBuilder.build());
    }

    private Disposable isAnotherAnnonceWithStatus(StatusRemote statusRemote, OnCheckedListener onCheckedListener) {
        // Read all annonces with statusRemote, when reach 0, then run onCheckedListener
        return annonceRepository
                .countFlowableAllAnnoncesByStatus(statusRemote.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(onCheckedListener::run);
    }

    private interface OnCheckedListener {
        void run(Integer count);
    }

    private interface CustomOnSuccessListener {
        void run(boolean success);
    }
}
