package oliweb.nc.oliweb.firebase.storage;

import android.content.Context;
import android.net.Uri;
import android.support.v4.util.Pair;
import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
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
     * If success, Single<Uri> will return the DownloadPath of the photo
     * If error, the Single will launch an error
     *
     * @param photo to store
     * @return the download path of the downloaded photo
     */
    public Single<Uri> sendPhotoToRemote(PhotoEntity photo) {
        Log.d(TAG, "Starting sendPhotoToRemote photo : " + photo);
        return Single.create(e -> {
            if (photo.getUriLocal() == null || photo.getUriLocal().isEmpty()) {
                e.onError(new RuntimeException("URI nécessaire pour sauvegarder une photo"));
            } else {
                File file = new File(photo.getUriLocal());
                String fileName = file.getName();
                StorageReference storageReference = fireStorage.child(fileName);
                storageReference.putFile(Uri.parse(photo.getUriLocal()))
                        .addOnFailureListener(e::onError)
                        .addOnSuccessListener(taskSnapshot ->
                                storageReference.getDownloadUrl()
                                        .addOnFailureListener(e::onError)
                                        .addOnSuccessListener(e::onSuccess)
                        );
            }
        });
    }

    /**
     * @param context
     * @param idAnnonce
     * @param listPhoto
     * @return The number of photos correctly saved
     */
    public Single<AtomicInteger> savePhotosFromRemoteToLocal(Context context, final long idAnnonce, final List<PhotoEntity> listPhoto) {
        Log.d(TAG, "savePhotosFromRemoteToLocal : " + listPhoto);
        return Single.create(emitter -> {
            AtomicInteger result = new AtomicInteger(0);
            if (listPhoto == null || listPhoto.isEmpty()) {
                emitter.onError(new RuntimeException("Liste des photos à sauvegarder vide"));
            } else {
                Observable.fromIterable(listPhoto)
                        .doOnComplete(() -> emitter.onSuccess(result))
                        .filter(photo -> photo.getFirebasePath() != null && !photo.getFirebasePath().isEmpty())
                        .switchMapSingle(photoEntity -> savePhotoToLocalByUrl(context, idAnnonce, photoEntity.getFirebasePath())
                                .doOnSuccess(photoEntity1 -> result.incrementAndGet())
                                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception)))
                        .subscribe();
            }
        });
    }

    public Single<PhotoEntity> savePhotoToLocalByUrl(Context context, final long idAnnonce, final String urlPhoto) {
        Log.d(TAG, "savePhotoToLocalByUrl : " + urlPhoto);
        return Single.create(emitter -> {
            if (urlPhoto == null || urlPhoto.isEmpty()) {
                emitter.onError(new RuntimeException("Impossible de sauvegarder une photo sans URL"));
            } else {
                boolean useExternalStorage = SharedPreferencesHelper.getInstance(context).getUseExternalStorage();
                Pair<Uri, File> pairUriFile = MediaUtility.createNewMediaFileUri(context, useExternalStorage, MediaUtility.MediaType.IMAGE);
                if (pairUriFile != null && pairUriFile.second != null && pairUriFile.first != null) {
                    StorageReference httpsReference = FirebaseStorage.getInstance().getReferenceFromUrl(urlPhoto);
                    httpsReference.getFile(pairUriFile.second)
                            .addOnSuccessListener(taskSnapshot -> {
                                Log.d(TAG, "Download successful for image : " + urlPhoto + " to URI : " + pairUriFile.first);
                                if (pairUriFile.first != null) {
                                    PhotoEntity photoEntity = new PhotoEntity();
                                    photoEntity.setStatut(StatusRemote.SEND);
                                    photoEntity.setFirebasePath(urlPhoto);
                                    photoEntity.setUriLocal(pairUriFile.first.toString());
                                    photoEntity.setIdAnnonce(idAnnonce);
                                    photoRepository.singleSave(photoEntity)
                                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                            .doOnSuccess(emitter::onSuccess)
                                            .subscribe();
                                }
                            })
                            .addOnFailureListener(exception -> {
                                Log.d(TAG, "Download failed for image : " + urlPhoto);
                                emitter.onError(exception);
                            });
                } else {
                    emitter.onError(new RuntimeException("Impossible de sauvegarder une image sans URI dans le content provider"));
                }
            }
        });
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
