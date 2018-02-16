package oliweb.nc.oliweb.job;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.PhotoRepository;

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
    private static final String FIREBASE_DB_ANNONCE_REF = "annonces";
    private static final String FIREBASE_DB_PHOTO_REF = "photos";
    private static final String FIREBASE_STORAGE_PHOTO = "photos";

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
        this.consThrowable = throwable -> Log.e(TAG, "Erreur sur l'API AfterShip : " + throwable.getMessage() + " Localized message : " + throwable.getLocalizedMessage(), throwable);
    }

    public static CoreSync getInstance(Context context, boolean sendNotification) {
        if (INSTANCE == null) {
            INSTANCE = new CoreSync(context, sendNotification);
            fireDb = FirebaseDatabase.getInstance();
            fireStorage = FirebaseStorage.getInstance().getReference();
            photoRepository = PhotoRepository.getInstance(context);
        }
        return INSTANCE;
    }

    /**
     * Création d'un Observable interval qui va envoyer un élément toutes les 10 secondes.
     * Cet élément sera synchronisé avec un élément de la liste de colis présents dans la DB.
     * Pour ce colis qui sera présent toutes les 10 secondes, on va appeler le service CoreSync.callOptTracking()
     * L'interval de temps nous permet de ne pas saturer le réseau avec des requêtes quand on a trop de colis dans la DB.
     */
    void createOrUpdateAnnonce() {
        AnnonceWithPhotosRepository.getInstance(context)
                .getAllAnnonceByStatus(StatusRemote.TO_SEND.getValue())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(annoncesWithPhoto -> {
                    for (AnnonceWithPhotos annonceWithPhoto : annoncesWithPhoto) {

                        // Envoi des photos sur FirebaseStorage
                        for (PhotoEntity photo : annonceWithPhoto.getPhotos()) {
                            File file = new File(photo.getUriLocal());
                            String fileName = file.getName();
                            StorageReference storageReference = fireStorage.child(fileName);
                            storageReference.putFile(Uri.parse(photo.getUriLocal()))
                                    .addOnSuccessListener(taskSnapshot -> {
                                        photo.setFirebasePath(taskSnapshot.getDownloadUrl().toString());
                                        photoRepository.save(photo, null);
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Failed to upload image"));
                        }


                        // On est sur le point d'envoyer l'annonce, on change son statut
                        annonceWithPhoto.getAnnonceEntity().setStatut(StatusRemote.SEND);

                        // Création ou mise à jour de l'annonce
                        if (annonceWithPhoto.getAnnonceEntity().getUUID() != null) {

                            // Mise à jour de l'annonce
                            fireDb.getReference(FIREBASE_DB_ANNONCE_REF)
                                    .child(annonceWithPhoto.getAnnonceEntity().getUUID())
                                    .setValue(annonceWithPhoto);
                        } else {
                            // Création d'une annonce
                            DatabaseReference dbRef = fireDb.getReference(FIREBASE_DB_ANNONCE_REF).push();
                            annonceWithPhoto.getAnnonceEntity().setUUID(dbRef.getKey());
                            dbRef.setValue(annonceWithPhoto);
                            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                }
                            });
                        }
                    }
                });
    }
}
