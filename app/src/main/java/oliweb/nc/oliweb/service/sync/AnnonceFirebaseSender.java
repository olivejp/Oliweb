package oliweb.nc.oliweb.service.sync;

import android.content.Context;
import android.util.Log;

import io.reactivex.Observable;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;

/**
 * Cette classe découpe toutes les étapes nécessaires pour l'envoi d'une annonce sur Firebase
 */
public class AnnonceFirebaseSender {

    private static final String TAG = AnnonceFirebaseSender.class.getName();

    private static AnnonceFirebaseSender instance;

    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    private AnnonceRepository annonceRepository;
    private AnnonceFullRepository annonceFullRepository;
    private PhotoFirebaseSender photoFirebaseSender;

    private AnnonceFirebaseSender() {
    }

    public static AnnonceFirebaseSender getInstance(Context context) {
        if (instance == null) {
            instance = new AnnonceFirebaseSender();
            instance.annonceRepository = AnnonceRepository.getInstance(context);
            instance.annonceFullRepository = AnnonceFullRepository.getInstance(context);
            instance.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(context);
            instance.photoFirebaseSender = PhotoFirebaseSender.getInstance(context);
        }
        return instance;
    }

    /**
     * Create/update an annonce to Firebase from an AnnonceFull from the Database
     * If operation succeed we try to save the annonce in the local DB
     *
     * @param annonceFull to send to Firebase
     */
    public void sendAnnonceToRemoteDatabase(AnnonceFull annonceFull) {
        Log.d(TAG, "Starting sendAnnonceToRemoteDatabase annonceFull : " + annonceFull);
        firebaseAnnonceRepository.getUidAndTimestampFromFirebase(annonceFull.getAnnonce())
                .doOnError(e -> markAnnonceAsFailedToSend(annonceFull.getAnnonce()))
                .toObservable()
                .switchMap(this::markAsSending)
                .switchMap(photoFirebaseSender::sendAllPhotosToRemote)
                .switchMap(annonceEntity -> annonceFullRepository.findAnnoncesByIdAnnonce(annonceEntity.getIdAnnonce()).toObservable())
                .map(AnnonceConverter::convertFullEntityToDto)
                .switchMap(annonceDto -> firebaseAnnonceRepository.saveAnnonceToFirebase(annonceDto).toObservable())
                .subscribe();
    }

    private Observable<AnnonceEntity> markAsSending(AnnonceEntity annonceEntity) {
        Log.d(TAG, "markAsSending annonceEntity : " + annonceEntity);
        annonceEntity.setStatut(StatusRemote.SENDING);
        return annonceRepository.saveWithSingle(annonceEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }



    /**
     * Cas d'une erreur dans l'envoi, on passe l'annonce en statut Failed To Send.
     *
     * @param annonceEntity
     */
    private void markAnnonceAsFailedToSend(final AnnonceEntity annonceEntity) {
        Log.d(TAG, "Mark message Failed To Send message : " + annonceEntity);
        annonceEntity.setStatut(StatusRemote.FAILED_TO_SEND);
        annonceRepository.saveWithSingle(annonceEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }




}
