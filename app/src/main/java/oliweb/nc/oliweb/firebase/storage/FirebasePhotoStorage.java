package oliweb.nc.oliweb.firebase.storage;

import android.content.Context;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.io.File;
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

        Pair<Uri, File> pairUriFile = MediaUtility.createNewMediaFileUri(context, useExternalStorage, MediaUtility.MediaType.IMAGE);
        if (pairUriFile != null && pairUriFile.second != null && pairUriFile.first != null) {
            httpsReference.getFile(pairUriFile.second)
                    .addOnSuccessListener(taskSnapshot -> {
                        Log.d(TAG, "Download successful for image : " + urlPhoto + " to URI : " + pairUriFile.first);
                        if (pairUriFile.first != null) {
                            PhotoEntity photoEntity = new PhotoEntity();
                            photoEntity.setStatut(StatusRemote.SEND);
                            photoEntity.setFirebasePath(urlPhoto);
                            photoEntity.setUriLocal(pairUriFile.first.toString());
                            photoEntity.setIdAnnonce(idAnnonce);
                            photoRepository.saveWithSingle(photoEntity).subscribe();
                        }
                    })
                    .addOnFailureListener(exception -> Log.d(TAG, "Download failed for image : " + urlPhoto));
        }
    }

    /**
     * Renverra True si la photo a été supprimée du Storage
     * S'il n'y a pas de photo à supprimer sur le storage, la fonction renverra quand même true
     * S'il y a une erreur lors de la suppression de la photo, on renverra l'exception
     *
     * @param photoEntity à supprimer
     * @return True si la photo a bien été supprimée ou s'il n'y avait pas de photo à supprimer, false sinon
     */
    public Single<AtomicBoolean> delete(PhotoEntity photoEntity) {
        Log.d(TAG, "Starting delete " + photoEntity.toString());
        return Single.create(emitter -> {
            if (photoEntity.getFirebasePath() != null && !photoEntity.getFirebasePath().isEmpty()) {
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
                                if (e instanceof StorageException && ((StorageException) e).getErrorCode() == StorageException.ERROR_OBJECT_NOT_FOUND) {
                                    Log.e(TAG, "Object not found on Firebase Storage. Return True anyway.", e);
                                    emitter.onSuccess(new AtomicBoolean(true));
                                } else {
                                    Log.e(TAG, "Failed to delete image on Firebase Storage : " + fileName + "exception : " + e.getMessage(), e);
                                    emitter.onSuccess(new AtomicBoolean(false));
                                }
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
