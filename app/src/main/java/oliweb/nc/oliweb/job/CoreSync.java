package oliweb.nc.oliweb.job;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.Utility;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.PhotoRepository;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_ANNONCE_REF;

/**
 * Created by orlanth23 on 18/12/2017.
 * <p>
 * This class contains the series of network calls to make to sync local db with firebase
 */
class CoreSync {
    private static final String TAG = CoreSync.class.getName();

    private static CoreSync INSTANCE;
    private Context context;
    private Consumer<Throwable> consThrowable;
    private boolean sendNotification;
    private static FirebaseDatabase fireDb;
    private static StorageReference fireStorage;
    private static PhotoRepository photoRepository;
    private static AnnonceRepository annonceRepository;


    /**
     * Private constructor
     *
     * @param context
     * @param sendNotification
     */
    private CoreSync(Context context, boolean sendNotification) {
        if (context != null) {
            this.context = context;
        }
        this.sendNotification = sendNotification;
        this.consThrowable = throwable -> Log.e(TAG, throwable.getLocalizedMessage(), throwable);
    }

    public static CoreSync getInstance(Context context, boolean sendNotification) {
        if (INSTANCE == null) {
            INSTANCE = new CoreSync(context, sendNotification);
            fireDb = FirebaseDatabase.getInstance();
            fireStorage = FirebaseStorage.getInstance().getReference();
            photoRepository = PhotoRepository.getInstance(context);
            annonceRepository = AnnonceRepository.getInstance(context);
        }
        return INSTANCE;
    }

    // ToDo décalage possible lors de l'envoi des photos. Il faut attendre que toutes les photos aient été envoyées avant de lancer la synchro de l'annonce.
    private void sendAnnonce() {
        AnnonceFullRepository.getInstance(context)
                .getAllAnnoncesByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(annoncesFulls -> {
                    for (AnnonceFull annonceFull : annoncesFulls) {

                        // SuccessListener qui permettra de changer le statut de l'annonce après envoi à Firebase.
                        OnSuccessListener<Void> onSuccessListener = o -> {
                            annonceFull.getAnnonce().setStatut(StatusRemote.SEND);
                            annonceRepository.update(null, annonceFull.getAnnonce());
                        };

                        // Création ou mise à jour de l'annonce
                        if (annonceFull.getAnnonce().getUUID() != null) {
                            // Mise à jour de l'annonce
                            fireDb.getReference(FIREBASE_DB_ANNONCE_REF)
                                    .child(annonceFull.getAnnonce().getUUID())
                                    .setValue(Utility.convertEntityToDto(annonceFull))
                                    .addOnSuccessListener(onSuccessListener);
                        } else {
                            // Création d'une annonce
                            DatabaseReference dbRef = fireDb.getReference(FIREBASE_DB_ANNONCE_REF).push();
                            annonceFull.getAnnonce().setUUID(dbRef.getKey());
                            dbRef.setValue(Utility.convertEntityToDto(annonceFull)).addOnSuccessListener(onSuccessListener);
                        }
                    }
                });
    }

    /**
     * Envoi des photos sur FirebaseStorage
     * @param listPhoto
     */
    private void sendPhotos(List<PhotoEntity> listPhoto) {
        for (PhotoEntity photo : listPhoto) {
            File file = new File(photo.getUriLocal());
            String fileName = file.getName();
            StorageReference storageReference = fireStorage.child(fileName);
            storageReference.delete()
                    .addOnCompleteListener(task ->
                    storageReference.putFile(Uri.parse(photo.getUriLocal()))
                            .addOnSuccessListener(taskSnapshot -> {
                                photo.setFirebasePath(taskSnapshot.getDownloadUrl().toString());
                                photoRepository.save(photo, null);
                            }).addOnFailureListener(e -> Log.e(TAG, "Failed to upload image")));
        }
    }

    /**
     * First send the photo to catch the URL Download link, then onAfterTerminate send the annonces
     */
    void createOrUpdateAnnonce() {
        PhotoRepository.getInstance(context)
                .getAllPhotosByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doAfterTerminate(this::sendAnnonce)
                .subscribe(this::sendPhotos);
    }
}
