package oliweb.nc.oliweb.service.sync.sender;

import android.content.Context;
import android.util.Log;

import io.reactivex.Observable;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;

/**
 * Cette classe décompose toutes les étapes nécessaires pour l'envoi d'un chat
 * sur Firebase.
 */
public class PhotoFirebaseSender {

    private static final String TAG = PhotoFirebaseSender.class.getName();

    private static PhotoFirebaseSender instance;

    private FirebasePhotoStorage firebasePhotoStorage;
    private PhotoRepository photoRepository;
    private AnnonceRepository annonceRepository;
    private AnnonceFirebaseSender annonceFirebaseSender;

    private PhotoFirebaseSender() {
    }

    public static PhotoFirebaseSender getInstance(Context context) {
        if (instance == null) {
            instance = new PhotoFirebaseSender();
            instance.firebasePhotoStorage = FirebasePhotoStorage.getInstance(context);
            instance.photoRepository = PhotoRepository.getInstance(context);
            instance.annonceRepository = AnnonceRepository.getInstance(context);
            instance.annonceFirebaseSender = AnnonceFirebaseSender.getInstance(context);
        }
        return instance;
    }

    public void sendPhotoToRemoteAndUpdateAnnonce(PhotoEntity photoEntity) {
        Log.d(TAG, "sendPhotoToRemoteAndUpdateAnnonce photoEntity : " + photoEntity);
        sendPhotoToRemote(photoEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .switchMap(photoEntity1 -> annonceRepository.findById(photoEntity1.getIdAnnonce()).toObservable())
                .switchMap(annonceFirebaseSender::convertToFullAndSendToFirebase)
                .subscribe();
    }


    private Observable<PhotoEntity> sendPhotoToRemote(PhotoEntity photoEntity) {
        Log.d(TAG, "sendPhotoToRemote photoEntity : " + photoEntity);
        return this.firebasePhotoStorage.sendPhotoToRemote(photoEntity).toObservable()
                .doOnError(exception -> {
                    Log.e(TAG, exception.getLocalizedMessage(), exception);
                    markPhotoAsFailedToSend(photoEntity);
                })
                .switchMap(uri -> markPhotoAsSend(photoEntity, uri.toString()));
    }

    /**
     * Photo bien envoyée, on la passe au statut SEND
     *
     * @param photoEntity
     * @param downloadUrl
     * @return
     */
    private Observable<PhotoEntity> markPhotoAsSend(final PhotoEntity photoEntity, String downloadUrl) {
        Log.d(TAG, "Mark photo has been Send photo : " + photoEntity + " downloadUrl : " + downloadUrl);
        photoEntity.setStatut(StatusRemote.SEND);
        photoEntity.setFirebasePath(downloadUrl);
        return photoRepository.singleSave(photoEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    /**
     * Cas d'une erreur dans l'envoi, on passe la photo en statut Failed To Send.
     *
     * @param photoEntity
     */
    private void markPhotoAsFailedToSend(final PhotoEntity photoEntity) {
        Log.d(TAG, "Mark photo Failed To Send message : " + photoEntity);
        photoEntity.setStatut(StatusRemote.FAILED_TO_SEND);
        photoRepository.singleSave(photoEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }
}
