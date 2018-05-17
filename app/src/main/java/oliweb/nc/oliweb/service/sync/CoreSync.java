package oliweb.nc.oliweb.service.sync;

import android.content.ContentResolver;
import android.content.Context;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;
import oliweb.nc.oliweb.utility.MediaUtility;
import oliweb.nc.oliweb.utility.Utility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

/**
 * Created by orlanth23 on 18/12/2017.
 * <p>
 * This class contains the series of network calls to make to sync local db with firebase
 */
public class CoreSync {
    private static final String TAG = CoreSync.class.getName();

    private static CoreSync instance;

    private PhotoRepository photoRepository;
    private AnnonceRepository annonceRepository;
    private AnnonceFullRepository annonceFullRepository;
    private FirebasePhotoStorage firebasePhotoStorage;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private FirebaseUserRepository firebaseUserRepository;
    private UtilisateurRepository utilisateurRepository;
    private String uidUser;
    private SharedPreferencesHelper preferencesHelper;
    private AnnonceFirebaseSender annonceFirebaseSender;
    private PhotoFirebaseSender photoFirebaseSender;

    private ContentResolver contentResolver;

    private CoreSync() {
    }

    public static CoreSync getInstance(Context context) {
        if (instance == null) {
            instance = new CoreSync();
            instance.photoRepository = PhotoRepository.getInstance(context);
            instance.annonceRepository = AnnonceRepository.getInstance(context);
            instance.annonceFullRepository = AnnonceFullRepository.getInstance(context);
            instance.utilisateurRepository = UtilisateurRepository.getInstance(context);
            instance.firebasePhotoStorage = FirebasePhotoStorage.getInstance(context);
            instance.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(context);
            instance.firebaseUserRepository = FirebaseUserRepository.getInstance();
            instance.contentResolver = context.getContentResolver();
            instance.preferencesHelper = SharedPreferencesHelper.getInstance(context);
            instance.uidUser = instance.preferencesHelper.getUidFirebaseUser();
            instance.annonceFirebaseSender = AnnonceFirebaseSender.getInstance(context);
            instance.photoFirebaseSender = PhotoFirebaseSender.getInstance(context);

        }
        return instance;
    }

    public void synchronize() {
        Log.d(TAG, "Launch synchronyse");
        syncToSend();
        syncToDelete();
    }

    public void synchronizeUser() {
        startSendingUser();
    }

    /**
     * Liste toutes les annonces et photos à envoyer
     */
    private void syncToSend() {
        startSendingAnnonces();
        startSendingUser();
    }

    /**
     * Once is complete, we start sending photos
     */
    private void startSendingAnnonces() {
        Log.d(TAG, "Starting syncToSend");
        annonceFullRepository
                .getAllAnnoncesByUidUserAndStatus(uidUser,Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .flattenAsObservable(list -> list)
                .doOnNext(annonceFirebaseSender::sendAnnonceToRemoteDatabase)
                .doOnComplete(this::startSendingPhotos)
                .subscribe();
    }

    private void startSendingPhotos() {
        Log.d(TAG, "Starting startSendingPhotos");
        photoRepository
                .getAllPhotosByUidUserAndStatus(uidUser, Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .doOnNext(photoFirebaseSender::sendPhotoToRemote)
                .subscribe();
    }

    private void startSendingUser() {
        Log.d(TAG, "Starting startSendingUser");
        utilisateurRepository
                .observeAllUtilisateursByStatus(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .doOnComplete(() -> Log.d(TAG, "All users to send has been send"))
                .doOnNext(utilisateur ->
                        firebaseUserRepository.insertUserIntoFirebase(utilisateur)
                                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                                .doOnSuccess(success -> {
                                    if (success.get()) {
                                        Log.d(TAG, "insertUserIntoFirebase successfully send user : " + utilisateur);
                                        utilisateur.setStatut(StatusRemote.SEND);
                                        utilisateurRepository.saveWithSingle(utilisateur)
                                                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                                                .subscribe();
                                    }
                                })
                                .subscribe()
                )
                .subscribe();
    }

       /**
     * Read all annonces with TO_DELETE status
     */
    private void syncToDelete() {
        Log.d(TAG, "Starting syncToDelete");
        deleteAnnonces()
                .doOnSuccess(atomic -> deletePhotos())
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
    private Single<AtomicBoolean> deleteAnnonces() {
        return Single.create(emitter -> {
            Log.d(TAG, "Starting deleteAnnonces");
            annonceRepository
                    .getAllAnnonceByStatus(Utility.allStatusToDelete())
                    .doOnError(emitter::onError)
                    .doOnNext(this::getAllPhotosToDelete)
                    .doOnComplete(() -> emitter.onSuccess(new AtomicBoolean(true)))
                    .subscribe();
        });
    }

    private void getAllPhotosToDelete(AnnonceEntity annonceEntity) {
        photoRepository
                .observeAllPhotosByIdAnnonce(annonceEntity.getId())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnNext(photo -> {
                    deletePhotoFromRemoteStorage(photo);
                    deleteFromLocalDb(photo, annonceEntity);
                })
                .subscribe();
    }

    // 1 - Suppression des photos du firebase Storage
    private void deletePhotoFromRemoteStorage(PhotoEntity photo) {
        firebasePhotoStorage.delete(photo)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
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
                .getAllPhotosByStatus(Utility.allStatusToDelete())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .flattenAsObservable(list -> list)
                .filter(photoEntity -> photoEntity.getFirebasePath() != null)
                .doOnNext(this::deleteFromStorage)
                .subscribe();
    }

    // TODO finir cette méthode
    // 1 - Supprimer de Firebase storage
    private void deleteFromStorage(PhotoEntity photoEntity) {
        firebasePhotoStorage.delete(photoEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(list -> {
                    // 2 - Supprimer de Firebase database (mise à jour de l'annonce)
                    annonceRepository.findById(photoEntity.getIdAnnonce());
                })
                .subscribe();
    }
}
