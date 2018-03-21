package oliweb.nc.oliweb.service;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;

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
import static oliweb.nc.oliweb.Constants.notificationSyncPhotoId;

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

    private int nbPhotoCompletedTask = 0;
    private int nbAnnonceCompletedTask = 0;
    private NotificationCompat.Builder mBuilder;
    private NotificationManagerCompat notificationManager;
    private ContentResolver contentResolver;

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

    /**
     * First send the photo to catch the URL Download link, then send the annonces
     */
    void synchronize() {
        Log.d(TAG, "Launch synchronyse");
        syncPhotosToSend();

        isAnotherPhotoWithStatus(StatusRemote.TO_SEND, count -> {
            if (count == null || count.equals(0)) {
                // No more photos to send, we can send annonce
                Log.d(TAG, "No more photos to send, synchronyze annonces");
                syncAnnoncesToSend();
            }
        });

        syncDeleted();
    }

    private void syncAnnoncesToSend() {
        annonceFullRepository
                .getAllAnnoncesByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(annoncesFulls -> {

                    // On a des annonces à envoyer, on affiche une notification de téléchargement
                    if (annoncesFulls != null && !annoncesFulls.isEmpty()) {
                        nbAnnonceCompletedTask = 0;

                        createNotification("Oliweb - Envoi de vos annonces");
                        updateProgressBar(annoncesFulls.size(), 0, notificationSyncAnnonceId);

                        // Parcours de la liste des annonces
                        for (AnnonceFull annonceFull : annoncesFulls) {
                            Log.d(TAG, "Synchro d'une annonce : " + annonceFull.toString());
                            sendAnnonceToFireabaseDatabase(annonceFull, annoncesFulls.size());
                        }
                    }
                });
    }

    private void sendAnnonceToFireabaseDatabase(AnnonceFull annonceFull, int totalSize) {
        DatabaseReference dbRef;

        // Création ou mise à jour de l'annonce
        if (annonceFull.getAnnonce().getUUID() != null) {
            dbRef = fireDb.getReference(FIREBASE_DB_ANNONCE_REF).child(annonceFull.getAnnonce().getUUID());
        } else {
            dbRef = fireDb.getReference(FIREBASE_DB_ANNONCE_REF).push();
            annonceFull.getAnnonce().setUUID(dbRef.getKey());
        }

        // Conversion de notre annonce en DTO
        AnnonceDto annonceDto = AnnonceConverter.convertEntityToDto(annonceFull);
        Log.d(TAG, annonceDto.toString());

        OnCheckedListener onCheckedListener = count -> {
            if (count == null || count == 0) {
                mBuilder.setContentText("Téléchargement terminé").setProgress(0, 0, false);
                notificationManager.notify(notificationSyncAnnonceId, mBuilder.build());
                notificationManager.cancel(notificationSyncAnnonceId);
            } else {
                updateProgressBar(totalSize, nbAnnonceCompletedTask++, notificationSyncAnnonceId);
            }
        };

        dbRef.setValue(annonceDto)
                .addOnSuccessListener(o -> {
                    annonceFull.getAnnonce().setStatut(StatusRemote.SEND);
                    annonceRepository.update(dataReturn -> isAnotherAnnonceWithStatus(StatusRemote.TO_SEND, onCheckedListener), annonceFull.getAnnonce());
                })
                .addOnFailureListener(e -> {
                    annonceFull.getAnnonce().setStatut(StatusRemote.FAILED_TO_SEND);
                    annonceRepository.update(dataReturn -> isAnotherAnnonceWithStatus(StatusRemote.TO_SEND, onCheckedListener), annonceFull.getAnnonce());
                });
    }

    /**
     * Envoi des photos sur FirebaseStorage
     */
    private void syncPhotosToSend() {
        Log.d(TAG, "Starting syncPhotosToSend");
        // Read all photos with TO_SEND status to send them
        photoRepository
                .getAllPhotosByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(listPhoto -> {

                    if (listPhoto != null && !listPhoto.isEmpty()) {
                        nbPhotoCompletedTask = 0;

                        // On a des photos à envoyer, on affiche une notification de téléchargement
                        createNotification("Oliweb - Envoi de vos photos");
                        updateProgressBar(listPhoto.size(), 0, notificationSyncPhotoId);

                        for (PhotoEntity photo : listPhoto) {
                            sendPhotoToFirebaseStorage(photo, listPhoto.size());
                        }
                    }
                });
    }

    private void sendPhotoToFirebaseStorage(PhotoEntity photo, int totalSize) {
        Log.d(TAG, "Sending " + photo.getUriLocal() + " to Firebase storage");

        OnCheckedListener onCheckedListener = count -> {
            if (count == null || count == 0) {
                mBuilder.setContentText("Téléchargement terminé").setProgress(0, 0, false);
                notificationManager.notify(notificationSyncPhotoId, mBuilder.build());
                notificationManager.cancel(notificationSyncPhotoId);
            }
        };

        File file = new File(photo.getUriLocal());
        String fileName = file.getName();
        StorageReference storageReference = fireStorage.child(fileName);
        storageReference.putFile(Uri.parse(photo.getUriLocal()))
                .addOnSuccessListener(taskSnapshot -> {
                    photo.setStatut(StatusRemote.SEND);
                    if (taskSnapshot.getDownloadUrl() != null) {
                        photo.setFirebasePath(taskSnapshot.getDownloadUrl().toString());
                    }
                    photoRepository.save(photo, dataReturn -> isAnotherPhotoWithStatus(StatusRemote.TO_SEND, onCheckedListener));
                    updateProgressBar(totalSize, nbPhotoCompletedTask++, notificationSyncPhotoId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to upload image");
                    photo.setStatut(StatusRemote.FAILED_TO_SEND);
                    photoRepository.save(photo, dataReturn -> isAnotherPhotoWithStatus(StatusRemote.TO_SEND, onCheckedListener));
                    updateProgressBar(totalSize, nbPhotoCompletedTask++, notificationSyncPhotoId);
                });
    }


    /**
     * Delete all photos
     * -on Firebase storage
     * -on device
     * -in local database
     *
     * @param idAnnonce
     */
    private void deletePhotoByIdAnnonce(long idAnnonce) {
        photoRepository
                .findAllSingleByIdAnnonce(idAnnonce)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(listPhotoEntity -> {
                    deletePhotosFromFirebaseStorage(listPhotoEntity);
                    deletePhotosFromDevice(listPhotoEntity);
                    deletePhotosFromDatabase(listPhotoEntity);
                });
    }

    private void deletePhotosFromDatabase(List<PhotoEntity> listPhotoEntity) {
        for (PhotoEntity photo : listPhotoEntity) {
            photoRepository.delete(null, photo);
        }
    }

    private void deletePhotoFromDevice(PhotoEntity photoToDelete, OnSuccessListener listener) {
        // Suppression du fichier physique
        if (contentResolver.delete(Uri.parse(photoToDelete.getUriLocal()), null, null) != 0) {
            Log.d(TAG, "Successful deleting physical photo : " + photoToDelete.getUriLocal());
            listener.run(true);
        } else {
            Log.e(TAG, "Fail to delete physical photo : " + photoToDelete.getUriLocal());
            listener.run(false);
        }
    }

    private void deletePhotosFromDevice(List<PhotoEntity> listPhotoToDelete) {
        if (listPhotoToDelete != null && !listPhotoToDelete.isEmpty()) {
            for (PhotoEntity photo : listPhotoToDelete) {
                deletePhotoFromDevice(photo, null);
            }
        }
    }

    private void deletePhotoFromFirebaseStorage(PhotoEntity photoEntity, OnSuccessListener listener) {
        // Si un chemin firebase est trouvé, on va également supprimer sur Firebase.
        if (photoEntity.getFirebasePath() != null) {
            // Suppression firebase ... puis suppression dans la DB
            File file = new File(photoEntity.getUriLocal());
            String fileName = file.getName();
            StorageReference storageReference = fireStorage.child(fileName);
            storageReference.delete()
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Successful deleting photo on Firebase Storage : " + fileName);
                        listener.run(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to delete image on Firebase Storage : " + e.getMessage());
                        listener.run(false);
                    });
        }
    }

    private void deletePhotosFromFirebaseStorage(List<PhotoEntity> listPhotoEntity) {
        if (listPhotoEntity != null && !listPhotoEntity.isEmpty()) {
            for (PhotoEntity photo : listPhotoEntity) {
                deletePhotoFromFirebaseStorage(photo, null);
            }
        }
    }

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
    private void syncDeleted() {
        // Read all annonces with TO_DELETE status to delete them on remote and local
        annonceRepository
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
                                                    annonceRepository.delete(null, annonce);
                                                }
                                            });

                                    deletePhotoByIdAnnonce(annonce.getIdAnnonce());
                                    deleteAnnonceFromFirebaseDatabase(annonce);
                                }
                            }
                        }
                );


        // Read all PhotoEntities with TO_DELETE status to delete them on remote and local
        photoRepository
                .getAllPhotosByStatus(StatusRemote.TO_DELETE.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(listPhotoToDelete -> {
                            if (listPhotoToDelete != null && !listPhotoToDelete.isEmpty()) {
                                for (PhotoEntity photo : listPhotoToDelete) {
                                    deletePhotoFromFirebaseStorage(photo, successFirebaseDeleting ->
                                            deletePhotoFromDevice(photo, successDeviceDeleting ->
                                                    photoRepository.delete(null, photo)
                                            )
                                    );
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

    private void isAnotherAnnonceWithStatus(StatusRemote statusRemote, OnCheckedListener onCheckedListener) {
        // Read all annonces with statusRemote, when reach 0, then run onCheckedListener
        annonceRepository
                .countAllAnnoncesByStatus(statusRemote.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(onCheckedListener::run);
    }

    private void isAnotherPhotoWithStatus(StatusRemote statusRemote, OnCheckedListener onCheckedListener) {
        // Read all photos with statusRemote, when reach 0, then run onCheckedListener
        photoRepository
                .countAllPhotosByStatus(statusRemote.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(onCheckedListener::run);
    }


    private interface OnCheckedListener {
        void run(Integer count);
    }

    private interface OnSuccessListener {
        void run(boolean success);
    }
}
