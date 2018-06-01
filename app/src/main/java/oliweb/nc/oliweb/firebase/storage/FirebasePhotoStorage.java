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

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;

import static oliweb.nc.oliweb.database.converter.PhotoConverter.createPhotoEntityFromUrl;
import static oliweb.nc.oliweb.utility.MediaUtility.createNewImagePairUriFile;

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
                saveFileToStorage(fileName, Uri.parse(photo.getUriLocal()))
                        .doOnSuccess(e::onSuccess)
                        .doOnError(e::onError)
                        .subscribe();

            }
        });
    }

    /**
     * Will send the file located at uriLocalFile to the Firebase Storage
     * with the name passed in fileName.
     *
     * @param fileName     futur name of the file in Firebase Storage
     * @param uriLocalFile location of the local file to send
     * @return Will return the URI of the file in Firebase Storage. You could use this URI to download the file from Internet
     */
    private Single<Uri> saveFileToStorage(String fileName, Uri uriLocalFile) {
        return Single.create(e -> {
            StorageReference storageReference = fireStorage.child(fileName);
            storageReference.putFile(uriLocalFile)
                    .addOnFailureListener(e::onError)
                    .addOnSuccessListener(taskSnapshot ->
                            storageReference.getDownloadUrl()
                                    .addOnFailureListener(e::onError)
                                    .addOnSuccessListener(e::onSuccess)
                    );
        });
    }

    /**
     * @param context
     * @param idAnnonce
     * @param listPhoto
     * @return The number of photos correctly saved
     */
    public void savePhotosFromRemoteToLocal(Context context, final long idAnnonce, final List<PhotoEntity> listPhoto) {
        Log.d(TAG, "savePhotosFromRemoteToLocal : " + listPhoto);
        for (PhotoEntity photo : listPhoto) {
            if (photo.getFirebasePath() != null && !photo.getFirebasePath().isEmpty()) {
                savePhotoToLocalByUrl(context, idAnnonce, photo.getFirebasePath())
                        .doOnSuccess(photoEntity1 -> Log.d(TAG, "Photo correctly inserted " + photoEntity1.getUriLocal()))
                        .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                        .subscribe();
            }
        }
    }

    public void savePhotoToLocalByListUrl(Context context, final long idAnnonce, List<String> listPhotoUrl) {
        Log.d(TAG, "savePhotoToLocalByListUrl : " + listPhotoUrl);
        for (String urlPhoto : listPhotoUrl) {
            Pair<Uri, File> pairUriFile = createNewImagePairUriFile(context);
            downloadFileFromStorage(pairUriFile.second, urlPhoto)
                    .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                    .doOnSuccess(atomicBoolean -> {
                        if (atomicBoolean.get()) {
                            photoRepository.singleSave(createPhotoEntityFromUrl(idAnnonce, urlPhoto, pairUriFile.first.toString()))
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                                    .subscribe();
                        }
                    })
                    .subscribe();
        }
    }


    public Single<PhotoEntity> savePhotoToLocalByUrl(Context context, final long idAnnonce, final String urlPhoto) {
        Log.d(TAG, "savePhotoToLocalByUrl : " + urlPhoto);
        return Single.create(emitter -> {
            if (urlPhoto == null || urlPhoto.isEmpty()) {
                emitter.onError(new RuntimeException("Impossible de sauvegarder une photo sans URL"));
            } else {
                Pair<Uri, File> pairUriFile = createNewImagePairUriFile(context);
                downloadFileFromStorage(pairUriFile.second, urlPhoto)
                        .doOnError(emitter::onError)
                        .doOnSuccess(atomicBoolean ->
                                photoRepository.singleSave(createPhotoEntityFromUrl(idAnnonce, urlPhoto, pairUriFile.first.toString()))
                                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                        .doOnSuccess(emitter::onSuccess)
                                        .subscribe()
                        )
                        .subscribe();
            }
        });
    }

    /**
     * Will download a file from Firebase Storage and record it to the file resultFile
     *
     * @param resultFile    where the downloaded file will be recorded
     * @param urlToDownload where to find the file to download on the internet
     * @return
     */
    private Single<AtomicBoolean> downloadFileFromStorage(File resultFile, String urlToDownload) {
        return Single.create(emitter -> {
            StorageReference httpsReference = FirebaseStorage.getInstance().getReferenceFromUrl(urlToDownload);
            httpsReference.getFile(resultFile)
                    .addOnFailureListener(emitter::onError)
                    .addOnSuccessListener(taskSnapshot -> emitter.onSuccess(new AtomicBoolean(true)));
        });
    }

    /**
     * Renverra True si la photo a été supprimée du Storage
     * S'il n'y a pas de photo à supprimer sur le storage, la fonction renverra quand même true
     * S'il y a une erreur lors de la suppression de la photo, une exception pourra être récupérer dans le doOnError()
     *
     * @param photoEntity à supprimer
     * @return True si la photo a bien été supprimée ou s'il n'y avait pas de photo à supprimer, false sinon
     */
    public Single<AtomicBoolean> delete(PhotoEntity photoEntity) {
        Log.d(TAG, "Starting delete " + photoEntity.toString());
        return Single.create(emitter -> {
            if (photoEntity.getFirebasePath() == null || photoEntity.getFirebasePath().isEmpty()) {
                emitter.onSuccess(new AtomicBoolean(true));
            } else {
                File file = new File(photoEntity.getUriLocal());
                deleteFromStorage(file.getName())
                        .doOnError(emitter::onError)
                        .doOnSuccess(emitter::onSuccess)
                        .subscribe();
            }
        });
    }

    private Single<AtomicBoolean> deleteFromStorage(String fileName) {
        return Single.create(emitter -> {
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
        });
    }
}
