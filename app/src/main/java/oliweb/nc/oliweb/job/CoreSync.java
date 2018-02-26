package oliweb.nc.oliweb.job;

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
import oliweb.nc.oliweb.Utility;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.PhotoRepository;
import oliweb.nc.oliweb.network.NetworkReceiver;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_ANNONCE_REF;
import static oliweb.nc.oliweb.Constants.notificationSyncAnnonceId;
import static oliweb.nc.oliweb.Constants.notificationSyncPhotoId;

/**
 * Created by orlanth23 on 18/12/2017.
 * <p>
 * This class contains the series of network calls to make to sync local db with firebase
 */
class CoreSync implements NetworkReceiver.NetworkChangeListener {
    private static final String TAG = CoreSync.class.getName();

    private static CoreSync INSTANCE;
    private static FirebaseDatabase fireDb;
    private static StorageReference fireStorage;
    private static PhotoRepository photoRepository;
    private static AnnonceRepository annonceRepository;

    private Context context;
    private int nbPhotoCompletedTask = 0;
    private int nbAnnonceCompletedTask = 0;
    private NotificationCompat.Builder mBuilder;
    private NotificationManagerCompat notificationManager;
    private boolean syncInProgress;

    /**
     * Private constructor
     *
     * @param context
     */
    private CoreSync(Context context) {
        if (context != null) {
            this.context = context;
        }
    }

    public static CoreSync getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new CoreSync(context);
            fireDb = FirebaseDatabase.getInstance();
            fireStorage = FirebaseStorage.getInstance().getReference();
            photoRepository = PhotoRepository.getInstance(context);
            annonceRepository = AnnonceRepository.getInstance(context);
        }
        return INSTANCE;
    }

    /**
     * First send the photo to catch the URL Download link, then send the annonces
     */
    void synchronize() {
        syncPhotos();

        // Read all photos with TO_DELETE status to delete them on remote and local
        PhotoRepository.getInstance(context)
                .getAllPhotosByStatus(StatusRemote.TO_DELETE.getValue())
                .subscribeOn(Schedulers.io())
                .doAfterTerminate(() -> {
                    // La suppression des annonces doit se faire après, pour cause de contrainte d'intégrité avec les photos.
                    // Read all annonces with TO_DELETE status to delete them on remote and local
                    AnnonceRepository.getInstance(context)
                            .getAllAnnonceByStatus(StatusRemote.TO_DELETE.getValue())
                            .subscribeOn(Schedulers.io())
                            .observeOn(Schedulers.io())
                            .subscribe(this::syncDeletedAnnonces);
                })
                .observeOn(Schedulers.io())
                .subscribe(this::syncDeletedPhotos);
    }

    private void syncAnnonce() {
        AnnonceFullRepository.getInstance(context)
                .getAllAnnoncesByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doAfterTerminate(() -> syncInProgress = false)
                .subscribe(annoncesFulls -> {

                    // On a des annonces à envoyer, on affiche une notification de téléchargement
                    if (annoncesFulls != null && !annoncesFulls.isEmpty()) {
                        syncInProgress = true;
                        nbAnnonceCompletedTask = 0;

                        createNotification("Oliweb - Envoi de vos annonces", "Téléchargement en cours");
                        updateProgressBar(annoncesFulls.size(), 0, notificationSyncAnnonceId);

                        // Parcours de la liste des annonces
                        for (AnnonceFull annonceFull : annoncesFulls) {
                            DatabaseReference dbRef;

                            // Création ou mise à jour de l'annonce
                            if (annonceFull.getAnnonce().getUUID() != null) {
                                dbRef = fireDb.getReference(FIREBASE_DB_ANNONCE_REF).child(annonceFull.getAnnonce().getUUID());
                            } else {
                                dbRef = fireDb.getReference(FIREBASE_DB_ANNONCE_REF).push();
                                annonceFull.getAnnonce().setUUID(dbRef.getKey());
                            }

                            // Conversion de notre annonce en DTO
                            AnnonceSearchDto annonceSearchDto = Utility.convertEntityToDto(annonceFull);
                            Log.d(TAG, annonceSearchDto.toString());

                            dbRef.setValue(annonceSearchDto)
                                    .addOnSuccessListener(o -> {
                                        annonceFull.getAnnonce().setStatut(StatusRemote.SEND);
                                        annonceRepository.update(dataReturn -> checkAnotherAnnonceToSend(), annonceFull.getAnnonce());
                                        updateProgressBar(annoncesFulls.size(), nbAnnonceCompletedTask++, notificationSyncAnnonceId);
                                    })
                                    .addOnFailureListener(e -> {
                                        annonceFull.getAnnonce().setStatut(StatusRemote.FAILED_TO_SEND);
                                        annonceRepository.update(dataReturn -> checkAnotherAnnonceToSend(), annonceFull.getAnnonce());
                                        updateProgressBar(annoncesFulls.size(), nbAnnonceCompletedTask++, notificationSyncAnnonceId);
                                    });
                        }
                    }
                });
    }

    /**
     * Envoi des photos sur FirebaseStorage
     */
    private void syncPhotos() {
        // Read all photos with TO_SEND status to send them
        PhotoRepository.getInstance(context)
                .getAllPhotosByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(listPhoto -> {

                    // On a des annonces à envoyer, on affiche une notification de téléchargement
                    if (listPhoto != null && !listPhoto.isEmpty()) {
                        syncInProgress = true;
                        nbPhotoCompletedTask = 0;

                        createNotification("Oliweb - Envoi de vos photos", "Téléchargement en cours");
                        updateProgressBar(listPhoto.size(), 0, notificationSyncPhotoId);

                        for (PhotoEntity photo : listPhoto) {
                            File file = new File(photo.getUriLocal());
                            String fileName = file.getName();
                            StorageReference storageReference = fireStorage.child(fileName);
                            storageReference.putFile(Uri.parse(photo.getUriLocal()))
                                    .addOnSuccessListener(taskSnapshot -> {
                                        photo.setStatut(StatusRemote.SEND);
                                        photo.setFirebasePath(taskSnapshot.getDownloadUrl().toString());
                                        photoRepository.save(photo, dataReturn -> checkAnotherPhotoToSend());
                                        updateProgressBar(listPhoto.size(), nbPhotoCompletedTask++, notificationSyncPhotoId);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed to upload image");
                                        photo.setStatut(StatusRemote.FAILED_TO_SEND);
                                        photoRepository.save(photo, dataReturn -> checkAnotherPhotoToSend());
                                        updateProgressBar(listPhoto.size(), nbPhotoCompletedTask++, notificationSyncPhotoId);
                                    });
                        }
                    }
                });
    }

    private void deleteLocalAndDbPhoto(PhotoEntity photo) {
        // Suppression du fichier physique
        if (context.getContentResolver().delete(Uri.parse(photo.getUriLocal()), null, null) != 0) {
            Log.d(TAG, "Successful deleting physical photo");

            // Suppression dans la base de données
            photoRepository.delete(null, photo);
        } else {
            Log.e(TAG, "Fail to delete physical photo");
        }
    }

    /**
     * Delete photos on remote (FB Storage and local)
     *
     * @param listPhotoToDelete
     */
    private void syncDeletedPhotos(List<PhotoEntity> listPhotoToDelete) {
        // On a des annonces à envoyer, on affiche une notification de téléchargement
        if (listPhotoToDelete != null && !listPhotoToDelete.isEmpty()) {
            for (PhotoEntity photo : listPhotoToDelete) {
                // Si un chemin firebase est trouvé, on va également supprimer sur Firebase.
                if (photo.getFirebasePath() != null) {
                    // Suppression firebase ... puis suppression dans la DB
                    File file = new File(photo.getUriLocal());
                    String fileName = file.getName();
                    StorageReference storageReference = fireStorage.child(fileName);
                    storageReference.delete()
                            .addOnCompleteListener(task -> deleteLocalAndDbPhoto(photo))
                            .addOnSuccessListener(taskSnapshot -> Log.d(TAG, "Successful deleting photo on Firebase Storage"))
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to delete image on Firebase Storage : " + e.getMessage()));
                } else {
                    deleteLocalAndDbPhoto(photo);
                }
            }
        }
    }

    /**
     * Delete photos on remote (FB Storage and local)
     *
     * @param listAnnoncesToDelete
     */
    private void syncDeletedAnnonces(List<AnnonceEntity> listAnnoncesToDelete) {
        // On a des annonces à envoyer, on affiche une notification de téléchargement
        if (listAnnoncesToDelete != null && !listAnnoncesToDelete.isEmpty()) {
            for (AnnonceEntity annonce : listAnnoncesToDelete) {
                // Si un chemin firebase est trouvé, on va également supprimer sur Firebase.
                fireDb.getReference(FIREBASE_DB_ANNONCE_REF)
                        .child(annonce.getUUID())
                        .removeValue()
                        .addOnFailureListener(e ->
                                Log.e(TAG, "Fail to delete annonce on Firebase Database : " + annonce.getUUID())
                        )
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successful delete annonce on Firebase Database : " + annonce.getUUID());

                            // Successful delete annonce
                            annonceRepository.delete(null, annonce);
                        });
            }
        }
    }

    private void createNotification(String title, String content) {
        notificationManager = NotificationManagerCompat.from(context);
        mBuilder = new NotificationCompat.Builder(context, Constants.CHANNEL_ID);
        mBuilder.setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_sync_white_48dp)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Issue the initial notification with zero progress
        int progressMax = 100;
        int progressCurrent = 0;
        mBuilder.setProgress(progressMax, progressCurrent, false);
    }

    private void updateProgressBar(int max, int current, int notificationId) {
        mBuilder.setProgress(max, current, false);
        notificationManager.notify(notificationId, mBuilder.build());
    }

    private void checkAnotherAnnonceToSend() {
        // Read all annonces with TO_SEND status, when reach 0, then cancel the notification
        AnnonceRepository.getInstance(context)
                .countAllAnnoncesByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(countAnnoncesToSend -> {
                    if (countAnnoncesToSend == null || countAnnoncesToSend == 0) {
                        mBuilder.setContentText("Téléchargement terminé").setProgress(0, 0, false);
                        notificationManager.notify(notificationSyncAnnonceId, mBuilder.build());
                        notificationManager.cancel(notificationSyncAnnonceId);
                    }
                });
    }

    private void checkAnotherPhotoToSend() {
        // Read all photos with TO_SEND status, when reach 0, then cancel the notification and sync the annonces
        PhotoRepository.getInstance(context)
                .countAllPhotosByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(countPhotosToSend -> {
                    if (countPhotosToSend == null || countPhotosToSend == 0) {
                        mBuilder.setContentText("Téléchargement terminé").setProgress(0, 0, false);
                        notificationManager.notify(notificationSyncPhotoId, mBuilder.build());
                        notificationManager.cancel(notificationSyncPhotoId);
                        syncAnnonce();
                    }
                });
    }

    @Override
    public void onNetworkEnable() {

    }

    @Override
    public void onNetworkDisable() {
        if (notificationManager != null) {
            notificationManager.cancel(notificationSyncPhotoId);
        }
    }
}
