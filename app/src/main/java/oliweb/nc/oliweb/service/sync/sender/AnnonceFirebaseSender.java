package oliweb.nc.oliweb.service.sync.sender;

import android.content.Context;
import android.util.Log;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
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
     * @param annonceEntity to send to Firebase
     */
    public void processTosendAnnonceToFirebase(AnnonceEntity annonceEntity) {
        Log.d(TAG, "Starting processTosendAnnonceToFirebase annonceEntity : " + annonceEntity);
        firebaseAnnonceRepository.getUidAndTimestampFromFirebase(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> annonceRepository.markAnnonceAsFailedToSend(annonceEntity))
                .toObservable()
                .switchMap(annonceRepository::markAsSending)
                .switchMap(this::convertToFullAndSendToFirebase)
                .switchMap(annonceRepository::findObservableByUid)
                .switchMap(annonceRepository::markAsSend)
                .switchMap(annonceFullRepository::findAnnonceFullByAnnonceEntity)
                .filter(annonceFull -> annonceFull.getPhotos() != null && !annonceFull.getPhotos().isEmpty())
                .switchMap(photoFirebaseSender::sendAllPhotosToRemote)
                .doOnComplete(() -> convertToFullAndSendToFirebase(annonceEntity).subscribe())
                .subscribe();
    }

    /**
     * Query the database to retreive the annonce by is Id
     * Convert the AnnonceFull to AnnonceFirebase
     * Try to send the AnnonceFirebase to Firbase
     *
     * @param annonceEntity to save to Firebase
     * @return UID of the recorded annonce
     */
    public Observable<String> convertToFullAndSendToFirebase(AnnonceEntity annonceEntity) {
        Log.d(TAG, "convertToFullAndSendToFirebase idAnnonce : " + annonceEntity);
        return annonceFullRepository.findAnnoncesByIdAnnonce(annonceEntity.getIdAnnonce())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .toObservable()
                .map(AnnonceConverter::convertFullEntityToDto)
                .switchMap(annonceDto -> firebaseAnnonceRepository.saveAnnonceToFirebase(annonceDto).toObservable());
    }
}
