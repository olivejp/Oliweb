package oliweb.nc.oliweb.service.firebase;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;

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
    private Scheduler scheduler;

    @Inject
    public AnnonceFirebaseSender(FirebaseAnnonceRepository firebaseAnnonceRepository,
                                 AnnonceRepository annonceRepository,
                                 PhotoFirebaseSender photoFirebaseSender,
                                 AnnonceFullRepository annonceFullRepository,
                                 @Named("processScheduler")
                                 Scheduler scheduler) {
        this.firebaseAnnonceRepository = firebaseAnnonceRepository;
        this.annonceRepository = annonceRepository;
        this.photoFirebaseSender = photoFirebaseSender;
        this.annonceFullRepository = annonceFullRepository;
        this.scheduler = scheduler;
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
                .subscribeOn(scheduler).observeOn(scheduler)
                .toObservable()
                .switchMap(annonceRepository::markAsSending)
                .switchMap(this::convertToFullAndSendToFirebase)
                .switchMap(uidAnnonce -> annonceRepository.findMaybeByUidAndFavorite(uidAnnonce, 0).toObservable())
                .switchMap(annonceRepository::markAsSend)
                .switchMap(annonceFullRepository::findAnnonceFullByAnnonceEntity)
                .filter(annonceFull -> annonceFull.getPhotos() != null && !annonceFull.getPhotos().isEmpty())
                .switchMapSingle(annonceFull -> photoFirebaseSender.sendPhotosToRemote(annonceFull.getPhotos()))
                .switchMap(atomicBoolean -> convertToFullAndSendToFirebase(annonceEntity))
                .doOnError(e -> annonceRepository.markAsFailedToSend(annonceEntity))
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
                .subscribeOn(scheduler).observeOn(scheduler)
                .toObservable()
                .map(AnnonceConverter::convertFullEntityToDto)
                .switchMap(annonceDto -> firebaseAnnonceRepository.saveAnnonceToFirebase(annonceDto)
                        .subscribeOn(scheduler).observeOn(scheduler)
                        .toObservable())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception));
    }
}
