package oliweb.nc.oliweb.job;

import android.content.Context;
import android.net.Uri;
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
import oliweb.nc.oliweb.SharedPreferencesHelper;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.PhotoRepository;
import oliweb.nc.oliweb.media.MediaType;
import oliweb.nc.oliweb.media.MediaUtility;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;

/**
 * Created by orlanth23 on 03/03/2018.
 */

public class FirebaseSync {

    private static final String TAG = FirebaseSync.class.getName();

    private static FirebaseSync instance;

    private PhotoRepository photoRepository;
    private AnnonceRepository annonceRepository;
    public static GenericTypeIndicator<HashMap<String, AnnonceSearchDto>> genericClass = new GenericTypeIndicator<HashMap<String, AnnonceSearchDto>>() {
    };

    private FirebaseSync() {
    }

    public static FirebaseSync getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseSync();
        }
        instance.photoRepository = PhotoRepository.getInstance(context);
        instance.annonceRepository = AnnonceRepository.getInstance(context);
        return instance;
    }

    void synchronize(Context context, String uidUtilisateur) {
        catchAnnonceFromFirebase(context, uidUtilisateur);
    }

    private void catchAnnonceFromFirebase(Context context, String uidUtilisateur) {
        getAllAnnonceByUidUtilisateur(uidUtilisateur).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    HashMap<String, AnnonceSearchDto> mapAnnonceSearchDto = dataSnapshot.getValue(genericClass);
                    if (mapAnnonceSearchDto != null && !mapAnnonceSearchDto.isEmpty()) {
                        for (Map.Entry<String, AnnonceSearchDto> entry : mapAnnonceSearchDto.entrySet()) {
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

    private void checkAnnonceExistInLocalOrSaveIt(Context context, AnnonceSearchDto annonceSearchDto) {
        existByUidUtilisateurAndUidAnnonce(annonceSearchDto.getUtilisateur().getUuid(), annonceSearchDto.getUuid())
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(integer -> {
                    if (integer == null || integer.equals(0)) {
                        saveAnnonceFromFirebaseToLocalDb(context, annonceSearchDto);
                    }
                });
    }

    public Query getAllAnnonceByUidUtilisateur(String uidUtilisateur) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("annonces");
        return ref.orderByChild("utilisateur/uuid").equalTo(uidUtilisateur);
    }

    public Single<Integer> existByUidUtilisateurAndUidAnnonce(String UidUtilisateur, String UidAnnonce) {
        return annonceRepository.existByUidUtilisateurAndUidAnnonce(UidUtilisateur, UidAnnonce);
    }

    private void saveAnnonceFromFirebaseToLocalDb(Context context, final AnnonceSearchDto annonceSearchDto) {
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setUUID(annonceSearchDto.getUuid());
        annonceEntity.setStatut(StatusRemote.SEND);
        annonceEntity.setTitre(annonceSearchDto.getTitre());
        annonceEntity.setDescription(annonceSearchDto.getDescription());
        annonceEntity.setDatePublication(annonceSearchDto.getDatePublication());
        annonceEntity.setPrix(annonceSearchDto.getPrix());
        annonceEntity.setIdCategorie(annonceSearchDto.getCategorie().getId());
        String uidUtilisateur = annonceSearchDto.getUtilisateur().getUuid();
        annonceEntity.setUuidUtilisateur(uidUtilisateur);
        annonceRepository.save(annonceEntity, dataReturn -> {
            // Now we can save Photos, if any
            if (dataReturn.isSuccessful()) {
                Log.d(TAG, "Annonce has been stored successfully");
                long idAnnonce = dataReturn.getIds()[0];
                for (String photoUrl : annonceSearchDto.getPhotos()) {
                    savePhotoFromFirebaseStorageToLocal(context, idAnnonce, photoUrl, uidUtilisateur);
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
}
