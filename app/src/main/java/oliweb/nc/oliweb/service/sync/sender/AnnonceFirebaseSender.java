package oliweb.nc.oliweb.service.sync.sender;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

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
@Singleton
public class AnnonceFirebaseSender {

    private static final String TAG = AnnonceFirebaseSender.class.getName();

    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private AnnonceRepository annonceRepository;
    private PhotoFirebaseSender photoFirebaseSender;
    private AnnonceFullRepository annonceFullRepository;

    @Inject
    public AnnonceFirebaseSender(FirebaseAnnonceRepository firebaseAnnonceRepository, AnnonceRepository annonceRepository, PhotoFirebaseSender photoFirebaseSender, AnnonceFullRepository annonceFullRepository) {
        this.firebaseAnnonceRepository = firebaseAnnonceRepository;
        this.annonceRepository = annonceRepository;
        this.photoFirebaseSender = photoFirebaseSender;
        this.annonceFullRepository = annonceFullRepository;
    }

    /**
     * Create/update an annonce to Firebase from an AnnonceFull from the Database
     * If operation succeed we try to save the annonce in the local DB
     *
     * @param annonceEntity to send to Firebase
     */
    public void processToSendAnnonceToFirebase(AnnonceEntity annonceEntity) {
        Log.d(TAG, "Starting processToSendAnnonceToFirebase annonceEntity : " + annonceEntity);
        firebaseAnnonceRepository.getUidAndTimestampFromFirebase(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> annonceRepository.markAsFailedToSend(annonceEntity))
                .toObservable()
                .switchMap(annonceRepository::markAsSending)
                .switchMap(this::convertToFullAndSendToFirebase)
                .switchMap(annonceRepository::findObservableByUid)
                .switchMap(annonceRepository::markAsSend)
                .switchMap(annonceFullRepository::findAnnonceFullByAnnonceEntity)
                .filter(annonceFull -> annonceFull.getPhotos() != null && !annonceFull.getPhotos().isEmpty())
                .switchMapSingle(annonceFull -> photoFirebaseSender.sendPhotosToRemote(annonceFull.getPhotos()))
                .switchMap(atomicBoolean -> convertToFullAndSendToFirebase(annonceEntity))
                .subscribe();
    }

    /**
     * Query the database to retrieve the annonce by is Id
     * Convert the AnnonceFull to AnnonceFirebase
     * Try to send the AnnonceFirebase to Firebase
     *
     * @param annonceEntity to save to Firebase
     * @return UID of the recorded annonce
     */
    public Observable<String> convertToFullAndSendToFirebase(AnnonceEntity annonceEntity) {
        Log.d(TAG, "convertToFullAndSendToFirebase idAnnonce : " + annonceEntity);
        return annonceFullRepository.findAnnoncesByIdAnnonce(annonceEntity.getIdAnnonce())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .toObservable()
                .map(AnnonceConverter::convertFullEntityToDto)
                .switchMap(annonceDto -> firebaseAnnonceRepository.saveAnnonceDtoToFirebase(annonceDto)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .toObservable());
    }
}
