package oliweb.nc.oliweb.firebase.storage;

import android.content.Context;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

public class FirebasePhotoStorage {

    private static final String TAG = FirebasePhotoStorage.class.getName();

    private static FirebasePhotoStorage instance;
    private StorageReference fireStorage;
    private PhotoRepository photoRepository;

    private FirebasePhotoStorage() {
    }

    public static FirebasePhotoStorage getInstance(Context context) {
        if (instance == null) {
            instance = new FirebasePhotoStorage();
            instance.fireStorage = FirebaseStorage.getInstance().getReference();
            instance.photoRepository = PhotoRepository.getInstance(context);
        }
        return instance;
    }

    /**
     * Read the photo URI, then send the photo to Firebase Storage.
     * The Single<String> will return the Downloadpath of the photo
     *
     * @param photo to store
     * @return the download path of the downloaded photo
     */
    public Single<Uri> savePhotoToRemote(PhotoEntity photo) {
        Log.d(TAG, "Starting savePhotoToRemote photo : " + photo);
        return Single.create(e -> {
            File file = new File(photo.getUriLocal());
            String fileName = file.getName();
            StorageReference storageReference = fireStorage.child(fileName);
            storageReference.putFile(Uri.parse(photo.getUriLocal()))
                    .addOnFailureListener(e::onError)
                    .addOnSuccessListener(taskSnapshot ->
                            // Récupération du lien pour télécharger l'image
                            storageReference.getDownloadUrl()
                                    .addOnFailureListener(e::onError)
                                    .addOnSuccessListener(e::onSuccess)
                    );
        });
    }

    public void saveFromRemoteToLocal(Context context, final long idAnnonce, final String urlPhoto) {
        Log.d(TAG, "saveFromRemoteToLocal : " + urlPhoto);
        boolean useExternalStorage = SharedPreferencesHelper.getInstance(context).getUseExternalStorage();
        StorageReference httpsReference = FirebaseStorage.getInstance().getReferenceFromUrl(urlPhoto);

        Pair<Uri, File> pairUriFile = MediaUtility.createNewMediaFileUri(context, useExternalStorage, MediaUtility.MediaType.IMAGE, UUID.randomUUID().toString());
        if (pairUriFile != null && pairUriFile.second != null && pairUriFile.first != null) {
            httpsReference.getFile(pairUriFile.second).addOnSuccessListener(taskSnapshot -> {
                // Local temp file has been created
                Log.d(TAG, "Download successful for image : " + urlPhoto + " to URI : " + pairUriFile.first);

                // Save photo to DB now
                if (pairUriFile.first != null) {
                    PhotoEntity photoEntity = new PhotoEntity();
                    photoEntity.setStatut(StatusRemote.SEND);
                    photoEntity.setFirebasePath(urlPhoto);
                    photoEntity.setUriLocal(pairUriFile.first.toString());
                    photoEntity.setIdAnnonce(idAnnonce);
                    photoRepository.saveWithSingle(photoEntity).subscribe();
                }

            }).addOnFailureListener(exception -> Log.d(TAG, "Download failed for image : " + urlPhoto));
        }
    }

    public Single<AtomicBoolean> delete(PhotoEntity photoEntity) {
        Log.d(TAG, "Starting delete " + photoEntity.toString());
        return Single.create(emitter -> {
            if (photoEntity.getFirebasePath() != null) {
                try {
                    File file = new File(photoEntity.getUriLocal());
                    String fileName = file.getName();
                    StorageReference storageReference = fireStorage.child(fileName);
                    storageReference.delete()
                            .addOnSuccessListener(taskSnapshot -> {
                                Log.d(TAG, "Successful deleting photo on Firebase Storage : " + fileName);
                                emitter.onSuccess(new AtomicBoolean(true));
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to delete image on Firebase Storage : " + fileName + "exception : " + e.getMessage());
                                emitter.onError(e);
                            });
                } catch (Exception e) {
                    emitter.onError(e);
                }
            } else {
                emitter.onSuccess(new AtomicBoolean(true));
            }
        });
    }
}
