package oliweb.nc.oliweb.service.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 18/12/2017.
 * <p>
 * This class contains the series of network calls to make to sync local db with firebase
 */
class CoreSync {
    private static final String TAG = CoreSync.class.getName();

    private static CoreSync instance;
    private static FirebaseDatabase fireDb;

    private PhotoRepository photoRepository;
    private AnnonceRepository annonceRepository;
    private AnnonceFullRepository annonceFullRepository;
    private FirebasePhotoStorage photoStorage;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    private ContentResolver contentResolver;

    private CoreSync() {
    }

    public static CoreSync getInstance(Context context) {
        if (instance == null) {
            instance = new CoreSync();
            fireDb = FirebaseDatabase.getInstance();
            instance.photoRepository = PhotoRepository.getInstance(context);
            instance.annonceRepository = AnnonceRepository.getInstance(context);
            instance.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(context);
            instance.annonceFullRepository = AnnonceFullRepository.getInstance(context);
            instance.photoStorage = FirebasePhotoStorage.getInstance(context);
            instance.contentResolver = context.getContentResolver();
        }
        return instance;
    }

    void synchronize() {
        Log.d(TAG, "Launch synchronyse");
        syncToSend();
        syncToDelete();
    }

    /**
     * Liste toutes les annonces et photos à envoyer
     */
    private void syncToSend() {
        sendAnnonces();
        sendPhotos();
    }

    /**
     * Read all annonces with TO_DELETE status
     */
    private void syncToDelete() {
        Log.d(TAG, "Starting syncToDelete");
        deleteAnnonces();
        deletePhotos();
    }

    private void sendAnnonces() {
        Log.d(TAG, "Starting syncToSend");
        annonceFullRepository
                .observeAllAnnoncesByStatus(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .doOnNext(this::sendAnnonceToRemoteDatabase)
                .subscribe();
    }

    private void sendPhotos() {
        Log.d(TAG, "Starting sendPhotos");
        photoRepository
                .observeAllPhotosByStatus(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .doOnNext(this::sendPhotoToRemote)
                .subscribe();
    }

    /**
     * Create/update an annonce to Firebase from an AnnonceFull from the Database
     * If operation succeed we try to send the photo of this AnnonceFull.
     *
     * @param annonceFull to send to Firebase
     */
    private void sendAnnonceToRemoteDatabase(AnnonceFull annonceFull) {
        Log.d(TAG, "Starting sendAnnonceToRemoteDatabase annonceFull : " + annonceFull);
        AnnonceDto annonceDto = AnnonceConverter.convertFullEntityToDto(annonceFull);
        firebaseAnnonceRepository.saveAnnonceToFirebase(annonceDto)
                .map(annonceDto1 -> {
                    annonceFull.getAnnonce().setDatePublication(annonceDto1.getDatePublication());
                    AnnonceEntity annonceEntity = annonceFull.getAnnonce();
                    annonceEntity.setStatut(StatusRemote.SEND);
                    return annonceEntity;
                })
                .doOnSuccess(this::saveAnnonceToLocalDb)
                .doOnError(saveToFirebaseException -> {
                    Log.d(TAG, "saveAnnonceToFirebase.doOnError saveToFirebaseException : " + saveToFirebaseException.getLocalizedMessage(), saveToFirebaseException);
                    annonceFull.getAnnonce().setStatut(StatusRemote.FAILED_TO_SEND);
                    annonceRepository.update(annonceFull.getAnnonce());
                })
                .subscribe();
    }

    private void saveAnnonceToLocalDb(AnnonceEntity annonceEntity) {
        Log.d(TAG, "Starting saveAnnonceToLocalDb annonceEntity : " + annonceEntity);
        annonceRepository.saveWithSingle(annonceEntity)
                .doOnSuccess(annonceEntitySaved ->
                        photoRepository
                                .getAllPhotosByStatusAndIdAnnonce(annonceEntitySaved.getId(), Utility.allStatusToSend())
                                .flattenAsObservable(list -> list)
                                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                .doOnNext(this::sendPhotoToRemote)
                                .subscribe()
                )
                .doOnError(saveSingleException -> Log.e(TAG, saveSingleException.getLocalizedMessage(), saveSingleException))
                .subscribe();
    }

    private void sendPhotoToRemote(PhotoEntity photo) {
        Log.d(TAG, "Sending " + photo.getUriLocal() + " to Firebase storage");
        this.photoStorage.sendToRemote(photo)
                .doOnError(exception -> {
                    Log.e(TAG, exception.getLocalizedMessage(), exception);
                    photo.setStatut(StatusRemote.FAILED_TO_SEND);
                    photoRepository.save(photo);
                })
                .doOnSuccess(downloadPath -> {
                    photo.setFirebasePath(downloadPath);
                    photo.setStatut(StatusRemote.SEND);
                    photoRepository.saveWithSingle(photo)
                            .doOnError(exception1 -> Log.e(TAG, exception1.getLocalizedMessage(), exception1))
                            .doOnSuccess(photoEntity ->
                                    firebaseAnnonceRepository.saveAnnonceToFirebase(photo.getIdAnnonce())
                                            .doOnError(exception1 -> Log.e(TAG, exception1.getLocalizedMessage(), exception1))
                                            .subscribe()
                            )
                            .subscribe();
                })
                .subscribe();
    }

    /**
     * Lecture de toutes les annonces avec des statuts à supprimer
     * 1 - Suppression des photos du Firebase Storage
     * 2 - Suppression de l'annonce dans Firebase
     * 3 - Suppression des photos dans le storage local
     * 4 - Suppression des photos dans la base locale
     * 5 - Suppression de l'annonce dans la base locale
     */
    private void deleteAnnonces() {
        Log.d(TAG, "Starting deleteAnnonces");
        annonceRepository
                .getAllAnnonceByStatus(Utility.allStatusToDelete())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnNext(this::getAllPhotosToDelete)
                .subscribe();
    }

    private void getAllPhotosToDelete(AnnonceEntity annonceEntity) {
        photoRepository
                .observeAllPhotosByIdAnnonce(annonceEntity.getId())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnNext(photo -> deletePhotoFromRemoteStorage(photo, annonceEntity))
                .subscribe();
    }

    // 1 - Suppression des photos du firebase Storage
    private void deletePhotoFromRemoteStorage(PhotoEntity photo, AnnonceEntity annonceEntity) {
        photoStorage.delete(photo)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(atomicBoolean -> {
                    if (atomicBoolean.get()) {
                        deleteFromLocalDb(photo, annonceEntity);
                    }
                })
                .subscribe();
    }

    // 2 - Suppression de l'annonce de firebase
    private void deleteFromLocalDb(PhotoEntity photo, AnnonceEntity annonceEntity) {
        Log.d(TAG, "deleteAnnonces : Delete from Firebase Storage Successful");
        firebaseAnnonceRepository.delete(annonceEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(deleteResult -> {
                    if (deleteResult.get()) {
                        deleteFromLocalStorage(photo);
                    }
                })
                .subscribe();
    }

    // 3 - Suppression du device
    private void deleteFromLocalStorage(PhotoEntity photo) {
        Log.d(TAG, "deleteAnnonces : Delete from Firebase Database Successful");
        MediaUtility.deletePhotoFromDevice(contentResolver, photo)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(deleteDeviceResult -> {
                    if (deleteDeviceResult.get()) {
                        deleteFromLocalDb(photo);
                    }
                })
                .subscribe();
    }

    // 4 - Suppression de la base locale
    private void deleteFromLocalDb(PhotoEntity photo) {
        Log.d(TAG, "deleteAnnonces : Delete from Local Storage Successful");
        photoRepository.delete(datareturn -> {
            if (datareturn.isSuccessful()) {
                Log.d(TAG, "deleteAnnonces : Delete from Local Database Successful");
            } else {
                Log.e(TAG, datareturn.getThrowable().getLocalizedMessage(), datareturn.getThrowable());
            }
        }, photo);
    }

    /**
     * Lecture de toutes les photos avec un statut "à supprimer"
     * Pour chaque photo, je vais tenter de :
     * 1 - Supprimer sur Firebase Storage
     * 2 - Supprimer sur Firebase Database (mise à jour de l'annonce)
     * 3 - Supprimer sur le storage local
     * 4 - Supprimer sur la database locale
     */
    private void deletePhotos() {
        Log.d(TAG, "Starting deletePhotos");
        photoRepository
                .observeAllPhotosByStatus(StatusRemote.TO_DELETE.getValue())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .filter(photoEntity -> photoEntity.getFirebasePath() != null)
                .doOnNext(this::deleteFromStorage)
                .subscribe();
    }

    // TODO finir cette méthode
    // 1 - Supprimer de Firebase storage
    private void deleteFromStorage(PhotoEntity photoEntity) {
        photoStorage.delete(photoEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(list -> {
                    // 2 - Supprimer de Firebase database (mise à jour de l'annonce)
                    annonceRepository.findById(photoEntity.getIdAnnonce());
                })
                .subscribe();
    }
}
