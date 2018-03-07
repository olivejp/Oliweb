package oliweb.nc.oliweb.service;

import android.content.Context;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.PhotoRepository;
import oliweb.nc.oliweb.helper.SharedPreferencesHelper;
import oliweb.nc.oliweb.media.MediaType;
import oliweb.nc.oliweb.media.MediaUtility;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_ANNONCE_REF;
import static oliweb.nc.oliweb.Constants.notificationSyncAnnonceId;

/**
 * Created by orlanth23 on 03/03/2018.
 */

public class FirebaseSync {

    private static final String TAG = FirebaseSync.class.getName();

    private static FirebaseSync instance;

    private PhotoRepository photoRepository;
    private AnnonceRepository annonceRepository;
    private NotificationCompat.Builder mBuilder;
    private NotificationManagerCompat notificationManager;
    public static GenericTypeIndicator<HashMap<String, AnnonceDto>> genericClass = new GenericTypeIndicator<HashMap<String, AnnonceDto>>() {
    };

    private FirebaseSync() {
    }

    public static FirebaseSync getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseSync();
        }
        instance.photoRepository = PhotoRepository.getInstance(context);
        instance.annonceRepository = AnnonceRepository.getInstance(context);
        instance.notificationManager = NotificationManagerCompat.from(context);
        instance.mBuilder = new NotificationCompat.Builder(context, Constants.CHANNEL_ID);
        return instance;
    }

    void synchronize(Context context, String uidUtilisateur) {
        getAllAnnonceFromFirebaseByUidUser(uidUtilisateur).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    HashMap<String, AnnonceDto> mapAnnonceSearchDto = dataSnapshot.getValue(genericClass);
                    if (mapAnnonceSearchDto != null && !mapAnnonceSearchDto.isEmpty()) {
                        notificationManager.notify(notificationSyncAnnonceId, createNotification("Oliweb - Réception de vos annonces", "Téléchargement en cours").build());
                        for (Map.Entry<String, AnnonceDto> entry : mapAnnonceSearchDto.entrySet()) {
                            checkAnnonceExistInLocalOrSaveIt(context, entry.getValue());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled");
            }
        });
    }

    private void checkAnnonceExistInLocalOrSaveIt(Context context, AnnonceDto annonceDto) {
        existInLocalByUidUserAndUidAnnonce(annonceDto.getUtilisateur().getUuid(), annonceDto.getUuid())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(integer -> {
                    if (integer == null || integer.equals(0)) {
                        saveAnnonceFromFirebaseToLocalDb(context, annonceDto);
                    }
                });
    }

    public Query getAllAnnonceFromFirebaseByUidUser(String uidUtilisateur) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_ANNONCE_REF);
        return ref.orderByChild("utilisateur/uuid").equalTo(uidUtilisateur);
    }

    public Single<Integer> existInLocalByUidUserAndUidAnnonce(String uidUtilisateur, String uidAnnonce) {
        return annonceRepository.existByUidUtilisateurAndUidAnnonce(uidUtilisateur, uidAnnonce);
    }

    private void saveAnnonceFromFirebaseToLocalDb(Context context, final AnnonceDto annonceDto) {
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setUUID(annonceDto.getUuid());
        annonceEntity.setStatut(StatusRemote.SEND);
        annonceEntity.setTitre(annonceDto.getTitre());
        annonceEntity.setDescription(annonceDto.getDescription());
        annonceEntity.setDatePublication(annonceDto.getDatePublication());
        annonceEntity.setPrix(annonceDto.getPrix());
        annonceEntity.setIdCategorie(annonceDto.getCategorie().getId());
        String uidUtilisateur = annonceDto.getUtilisateur().getUuid();
        annonceEntity.setUuidUtilisateur(uidUtilisateur);
        annonceRepository.save(annonceEntity, dataReturn -> {
            // Now we can save Photos, if any
            if (dataReturn.isSuccessful()) {
                Log.d(TAG, "Annonce has been stored successfully");
                long idAnnonce = dataReturn.getIds()[0];
                if (annonceDto.getPhotos() != null && !annonceDto.getPhotos().isEmpty()) {
                    for (String photoUrl : annonceDto.getPhotos()) {
                        savePhotoFromFirebaseStorageToLocal(context, idAnnonce, photoUrl, uidUtilisateur);
                    }
                }
            } else {
                Log.e(TAG, "Annonce has not been stored correctly UidAnnonce : " + annonceEntity.getUUID() + ", UidUtilisateur : " + uidUtilisateur);
            }
        });
    }

    private void savePhotoFromFirebaseStorageToLocal(Context context, final long idAnnonce, final String urlPhoto, String uidUtilisateur) {
        StorageReference httpsReference = FirebaseStorage.getInstance().getReferenceFromUrl(urlPhoto);
        boolean externalStorage = SharedPreferencesHelper.getInstance(context).getUseExternalStorage();
        Pair<Uri, File> pairUriFile = MediaUtility.createNewMediaFileUri(context, externalStorage, MediaType.IMAGE, uidUtilisateur);
        if (pairUriFile != null && pairUriFile.second != null && pairUriFile.first != null) {
            httpsReference.getFile(pairUriFile.second).addOnSuccessListener(taskSnapshot -> {
                // Local temp file has been created
                Log.d(TAG, "Download successful for image : " + urlPhoto + " to URI : " + pairUriFile.first);

                // Save photo to DB now
                PhotoEntity photoEntity = new PhotoEntity();
                photoEntity.setStatut(StatusRemote.SEND);
                photoEntity.setFirebasePath(urlPhoto);
                photoEntity.setUriLocal(pairUriFile.first.toString());
                photoEntity.setIdAnnonce(idAnnonce);

                photoRepository.save(photoEntity, dataReturn -> {
                    if (dataReturn.isSuccessful()) {
                        Log.d(TAG, "Insert into DB successful");
                    } else {
                        Log.d(TAG, "Insert into DB fail");
                    }
                });

            }).addOnFailureListener(exception -> {
                // Handle any errors
                Log.d(TAG, "Download failed for image : " + urlPhoto);
            });
        }
    }

    private NotificationCompat.Builder createNotification(String title, String contentText) {
        return mBuilder.setContentTitle(title)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_sync_white_48dp)
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }
}
