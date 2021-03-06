package oliweb.nc.oliweb.service.firebase;

import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Cette classe décompose toutes les étapes nécessaires pour l'envoi d'un chat
 * sur Firebase.
 */
@Singleton
public class PhotoFirebaseSender {

    private static final String TAG = PhotoFirebaseSender.class.getName();

    private FirebasePhotoStorage firebasePhotoStorage;
    private PhotoRepository photoRepository;

    @Inject
    public PhotoFirebaseSender(FirebasePhotoStorage firebasePhotoStorage, PhotoRepository photoRepository) {
        this.firebasePhotoStorage = firebasePhotoStorage;
        this.photoRepository = photoRepository;
    }

    public Single<AtomicBoolean> sendPhotosToRemote(List<PhotoEntity> listPhoto) {
        Log.d(TAG, "sendPhotosToRemote listPhoto : " + listPhoto);
        return Single.create(emitter ->
                Observable.fromIterable(listPhoto)
                        .filter(photoEntity -> Utility.allStatusToSend().contains(photoEntity.getStatut().getValue()))
                        .concatMap(this::sendPhotoToRemote)
                        .doOnComplete(() -> emitter.onSuccess(new AtomicBoolean(true)))
                        .doOnError(emitter::onError)
                        .subscribe()
        );
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
     */
    private void markPhotoAsFailedToSend(final PhotoEntity photoEntity) {
        Log.d(TAG, "Mark photo Failed To Send message : " + photoEntity);
        photoEntity.setStatut(StatusRemote.FAILED_TO_SEND);
        photoRepository.singleSave(photoEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }
}
