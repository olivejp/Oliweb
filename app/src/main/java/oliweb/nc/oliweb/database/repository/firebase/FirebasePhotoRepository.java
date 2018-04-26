package oliweb.nc.oliweb.database.repository.firebase;

import android.content.Context;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.UUID;

import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_PHOTO_REF;

// TODO Faire des tests sur ce repository
public class FirebasePhotoRepository {

    private static final String TAG = FirebasePhotoRepository.class.getName();
    private DatabaseReference PHOTO_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_PHOTO_REF);

    private static FirebasePhotoRepository instance;

    private PhotoRepository photoRepository;

    private FirebasePhotoRepository() {
    }

    public static FirebasePhotoRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FirebasePhotoRepository();
        }
        instance.photoRepository = PhotoRepository.getInstance(context);
        return instance;
    }

    public void savePhotoFromFirebaseStorageToLocal(Context context, final long idAnnonce, final String urlPhoto, String uidUser) {
        Log.d(TAG, "savePhotoFromFirebaseStorageToLocal : " + urlPhoto);
        boolean useExternalStorage = SharedPreferencesHelper.getInstance(context).getUseExternalStorage();
        StorageReference httpsReference = FirebaseStorage.getInstance().getReferenceFromUrl(urlPhoto);

        Pair<Uri, File> pairUriFile = MediaUtility.createNewMediaFileUri(context, useExternalStorage, MediaUtility.MediaType.IMAGE, UUID.randomUUID().toString());
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
