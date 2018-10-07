package oliweb.nc.oliweb.service.firebase;

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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.repository.local.PhotoRepository;

import static oliweb.nc.oliweb.database.converter.PhotoConverter.createPhotoEntityFromUrl;
import static oliweb.nc.oliweb.utility.MediaUtility.createNewImagePairUriFile;

@Singleton
public class FirebasePhotoStorage {

    private static final String TAG = FirebasePhotoStorage.class.getName();
    public static final String ERROR_MISSED_URI = "URI nécessaire pour sauvegarder une photo";

    private StorageReference fireStorage;
    private PhotoRepository photoRepository;
    private Scheduler processScheduler;

    @Inject
    public FirebasePhotoStorage(PhotoRepository photoRepository, @Named("processScheduler") Scheduler processScheduler) {
        this.photoRepository = photoRepository;
        this.processScheduler = processScheduler;
    }

    // Cette méthode sert pour les tests afin de pouvoir injecter notre storageReference
    private void checkFirebaseStorage() {
        if (fireStorage == null) {
            fireStorage = FirebaseStorage.getInstance().getReference();
        }
    }

    public void setFireStorage(StorageReference fireStorage) {
        this.fireStorage = fireStorage;
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
                e.onError(new RuntimeException(ERROR_MISSED_URI));
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
        checkFirebaseStorage();

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
    public Single<Long> savePhotosFromRemoteToLocal(Context context, final long idAnnonce, final List<PhotoEntity> listPhoto) {
        Log.d(TAG, "savePhotosFromRemoteToLocal : " + listPhoto);
        return Single.create(emitter ->
                Observable.fromIterable(listPhoto)
                        .filter(photo -> (photo.getFirebasePath() != null && !photo.getFirebasePath().isEmpty()))
                        .switchMapSingle(photoEntity -> savePhotoToLocalByUrl(context, idAnnonce, photoEntity.getFirebasePath()))
                        .doOnComplete(() -> emitter.onSuccess(idAnnonce))
                        .doOnError(emitter::onError)
                        .subscribe()
        );
    }

    /**
     * Pour toutes les urls passées dans la liste,
     * On va télécharger l'image
     *
     * @param context
     * @param idAnnonce
     * @param listPhotoUrl
     */
    public void savePhotoToLocalByListUrl(Context context, final long idAnnonce, List<String> listPhotoUrl) {
        Log.d(TAG, "savePhotoToLocalByListUrl : " + listPhotoUrl);
        for (String urlPhoto : listPhotoUrl) {
            Pair<Uri, File> pairUriFile = createNewImagePairUriFile(context);
            downloadFileFromStorage(pairUriFile.second, urlPhoto)
                    .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                    .doOnSuccess(atomicBoolean -> {
                        if (atomicBoolean.get()) {
                            PhotoEntity photoEntity = createPhotoEntityFromUrl(idAnnonce, urlPhoto, pairUriFile.first.toString());
                            photoRepository.singleSave(photoEntity)
                                    .subscribeOn(processScheduler).observeOn(processScheduler)
                                    .doOnSuccess(photoEntity1 -> Log.d(TAG, "Insertion de la photo réussie"))
                                    .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                                    .subscribe();
                        }
                    })
                    .subscribe();
        }
    }

    private Single<PhotoEntity> savePhotoToLocalByUrl(Context context, final long idAnnonce, final String urlPhoto) {
        Log.d(TAG, "savePhotoToLocalByUrl : " + urlPhoto);
        return Single.create(emitter -> {
            if (urlPhoto == null || urlPhoto.isEmpty()) {
                emitter.onError(new RuntimeException("Impossible de sauvegarder une photo sans URL"));
            } else {
                Pair<Uri, File> pairUriFile = createNewImagePairUriFile(context);
                downloadFileFromStorage(pairUriFile.second, urlPhoto)
                        .doOnSuccess(atomicBoolean -> {
                            if (atomicBoolean.get()) {
                                PhotoEntity photoEntity = createPhotoEntityFromUrl(idAnnonce, urlPhoto, pairUriFile.first.toString());
                                photoRepository.singleSave(photoEntity)
                                        .subscribeOn(processScheduler).observeOn(processScheduler)
                                        .doOnSuccess(emitter::onSuccess)
                                        .doOnError(emitter::onError)
                                        .subscribe();
                            }
                        })
                        .doOnError(emitter::onError)
                        .subscribe();
            }
        });
    }

    /**
     * Will download a file from Firebase Storage and record it to the file resultFile
     *
     * @param resultFile    where the downloaded file will be recorded
     * @param urlToDownload where to find the file to download on the internet
     * @return true if the task is successful
     */
    private Single<AtomicBoolean> downloadFileFromStorage(File resultFile, String urlToDownload) {
        return Single.create(emitter -> {
            StorageReference httpsReference = FirebaseStorage.getInstance().getReferenceFromUrl(urlToDownload);
            httpsReference.getFile(resultFile)
                    .addOnFailureListener(emitter::onError)
                    .addOnSuccessListener(taskSnapshot ->
                            emitter.onSuccess(new AtomicBoolean(taskSnapshot.getTask().isSuccessful()))
                    );
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

    /**
     * Delete a file from Firebase Storage based on its fileName
     *
     * @param fileName of the file we want to delete
     * @return atomicBoolean will be true :
     * -if no object with this fileName has been found
     * -if delete is successful
     * will return a exception in case of error
     */
    private Single<AtomicBoolean> deleteFromStorage(String fileName) {
        checkFirebaseStorage();
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
                            emitter.onError(e);
                        }
                    });
        });
    }
}
