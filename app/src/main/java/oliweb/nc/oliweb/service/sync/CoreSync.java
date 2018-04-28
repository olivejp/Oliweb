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
import oliweb.nc.oliweb.database.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.utility.Constants;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_CHATS_REF;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_MESSAGES_REF;
import static oliweb.nc.oliweb.utility.Constants.notificationSyncAnnonceId;

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
    private FirebaseAnnonceRepository firebaseAnnonceRepository;

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
            instance.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(context);
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
                createNotification();
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
    private void syncToSend() {
        annonceFullRepository
                .getAllAnnoncesByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(annoncesFulls -> {
                    if (annoncesFulls != null && !annoncesFulls.isEmpty()) {
                        nbAnnonceCompletedTask = 0;
                        for (AnnonceFull annonceFull : annoncesFulls) {
                            Log.d(TAG, "Tentative d'envoi d'une annonce : " + annonceFull.toString());
                            sendAnnonceToFirebaseDatabase(annonceFull);
                        }
                    }
                })
                .subscribe();
    }

    /**
     * Create/update an annonce to Firebase from an AnnonceFull from the Database
     * If operation succeed we try to send the photo of this AnnonceFull.
     *
     * @param annonceFull to send to Firebase
     */
    private void sendAnnonceToFirebaseDatabase(AnnonceFull annonceFull) {
        AnnonceDto annonceDto = AnnonceConverter.convertEntityToDto(annonceFull);
        firebaseAnnonceRepository.saveAnnonceToFirebase(annonceDto)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(annonceDtoSaved -> {
                    AnnonceEntity annonceEntityToSaved = AnnonceConverter.convertDtoToEntity(annonceDtoSaved);
                    annonceEntityToSaved.setStatut(StatusRemote.SEND);
                    annonceRepository.saveWithSingle(annonceEntityToSaved)
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(annonceEntitySaved -> sendPhotosToFbStorage(annonceEntitySaved.getIdAnnonce()))
                            .doOnError(saveSingleException -> Log.e(TAG, saveSingleException.getLocalizedMessage(), saveSingleException))
                            .subscribe();
                })
                .doOnError(saveToFirebaseException -> {
                    annonceFull.getAnnonce().setStatut(StatusRemote.FAILED_TO_SEND);
                    annonceRepository.update(annonceFull.getAnnonce());
                })
                .subscribe();
    }


    /**
     * List all the photos for an Annonce and try to send them to Fb Storage
     *
     * @param idAnnonce of the AnnonceFull we try to send to Fb
     */
    private void sendPhotosToFbStorage(long idAnnonce) {
        Log.d(TAG, "Starting sendPhotosToFbStorage");
        photoRepository
                .getAllPhotosByStatusAndIdAnnonce(StatusRemote.TO_SEND.getValue(), idAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
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
                    photoRepository.update(photo);
                });
    }

    /**
     * Update the AnnonceFull to the Fb Database.
     * In this method we only send the annonce.
     *
     * @param idAnnonce of the AnnonceFull we want to send.
     */
    private void updateAnnonceFullFromDbToFirebase(long idAnnonce) {
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
        photoRepository
                .findAllPhotosByIdAnnonce(idAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(this::deletePhotosFromMultiService)
                .subscribe();
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
     * @param listPhotoEntity List des photos à supprimer de Firebase Storage
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
     * @param annonce à supprimer de Firebase Database
     */
    private void deleteAnnonceFromFirebaseDatabase(AnnonceEntity annonce) {
        // Si un chemin firebase est trouvé, on va également supprimer sur Firebase.
        fireDb.getReference(FIREBASE_DB_ANNONCE_REF)
                .child(annonce.getUUID())
                .removeValue()
                .addOnFailureListener(e -> Log.e(TAG, "Fail to delete annonce on Firebase Database : " + annonce.getUUID()))
                .addOnSuccessListener(aVoid -> {
                    deleteChatFromFirebaseByAnnonceUid(annonce.getUUID());
                    Log.d(TAG, "Successful delete annonce on Firebase Database : " + annonce.getUUID());
                });
    }

    private void deleteChatFromFirebaseByAnnonceUid(String uidAnnonce) {
        // Si un chemin firebase est trouvé, on va également supprimer sur Firebase.
        fireDb.getReference(FIREBASE_DB_CHATS_REF)
                .orderByChild("uidAnnonce")
                .equalTo(uidAnnonce)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            ChatFirebase chat = data.getValue(ChatFirebase.class);
                            if (chat != null) {
                                fireDb.getReference(FIREBASE_DB_CHATS_REF)
                                        .child(chat.getUid())
                                        .removeValue()
                                        .addOnSuccessListener(aVoid -> deleteMessageFromFirebase(chat.getUid()));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Do nothing
                    }
                });
    }

    private void deleteMessageFromFirebase(String uidChat) {
        fireDb.getReference(FIREBASE_DB_MESSAGES_REF)
                .child(uidChat)
                .removeValue();
    }

    /**
     * Read all annonces with TO_DELETE status
     */
    private void syncToDelete() {
        syncDeleteAnnonce();
        syncDeletePhoto();
    }

    private void syncDeleteAnnonce() {
        // Read all annonces with TO_DELETE status to delete them on remote and local
        annonceRepository
                .getAllAnnonceByStatus(StatusRemote.TO_DELETE.getValue())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(listAnnoncesToDelete -> {
                    if (listAnnoncesToDelete != null && !listAnnoncesToDelete.isEmpty()) {
                        for (AnnonceEntity annonce : listAnnoncesToDelete) {

                            // We delete only when it left 0 photos for this annonce
                            photoRepository.countAllPhotosByIdAnnonce(annonce.getIdAnnonce())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.io())
                                    .doOnSuccess(integer -> {
                                        if (integer == null || integer.equals(0)) {
                                            annonceRepository.delete(annonce);
                                        }
                                    })
                                    .subscribe();

                            deletePhotoByIdAnnonce(annonce.getIdAnnonce());
                            deleteAnnonceFromFirebaseDatabase(annonce);
                        }
                    }
                })
                .subscribe();
    }

    private void syncDeletePhoto() {
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
        mBuilder.setContentTitle("Oliweb - Envoi de vos annonces")
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
                .countFlowableAllAnnoncesByStatus(statusRemote.getValue())
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
