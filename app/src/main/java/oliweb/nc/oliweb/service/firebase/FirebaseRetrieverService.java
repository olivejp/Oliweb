package oliweb.nc.oliweb.service.firebase;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Scheduler;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.utility.LiveDataOnce;

/**
 * Created by orlanth23 on 03/03/2018.
 * This class allows to retrieve {@link AnnonceFirebase} from Firebase corresponding to the given UID User.
 */
@Singleton
public class FirebaseRetrieverService {

    private static final String TAG = FirebaseRetrieverService.class.getCanonicalName();
    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private AnnonceRepository annonceRepository;
    private FirebasePhotoStorage firebasePhotoStorage;
    private Scheduler scheduler;
    private Scheduler androidScheduler;

    @Inject
    public FirebaseRetrieverService(FirebaseAnnonceRepository firebaseAnnonceRepository,
                                    AnnonceRepository annonceRepository,
                                    FirebasePhotoStorage firebasePhotoStorage,
                                    @Named("processScheduler") Scheduler scheduler,
                                    @Named("androidScheduler") Scheduler androidScheduler) {
        this.firebaseAnnonceRepository = firebaseAnnonceRepository;
        this.annonceRepository = annonceRepository;
        this.firebasePhotoStorage = firebasePhotoStorage;
        this.scheduler = scheduler;
        this.androidScheduler = androidScheduler;
    }

    /**
     * Retrieve all the annonces on the Fb database for the specified User uid.
     * Then we try to find them in the local DB.
     * If not present the MutableLiveData shouldAskQuestion will receive True.
     *
     * @param uidUser
     */
    public LiveDataOnce<AtomicBoolean> checkFirebaseRepository(final String uidUser) {
        return observer -> firebaseAnnonceRepository.observeAllAnnonceByUidUser(uidUser)
                .subscribeOn(androidScheduler).observeOn(scheduler)
                .switchMapSingle(annonceDto -> annonceRepository.countByUidUserAndUidAnnonce(uidUser, annonceDto.getUuid()))
                .any(integer -> integer != null && integer == 0)
                .doOnSuccess(aBoolean -> observer.onChanged(new AtomicBoolean(aBoolean)))
                .doOnError(throwable -> Log.e(TAG, throwable.getMessage()))
                .subscribe();
    }

    /**
     * Lecture de toutes les annonces présentes dans Firebase pour cet utilisateur
     * et récupération de ces annonces dans la base locale
     */
    public void synchronize(Context context, String uidUser) {
        firebaseAnnonceRepository.observeAllAnnonceByUidUser(uidUser)
                .subscribeOn(scheduler).observeOn(scheduler)
                .doOnNext(annonceDto -> {
                    Log.d(TAG, "Starting saveAnnonceDtoToLocalDb called with annonceDto = " + annonceDto.toString());
                    annonceRepository.countByUidUserAndUidAnnonce(annonceDto.getUtilisateur().getUuid(), annonceDto.getUuid())
                            .subscribeOn(scheduler).observeOn(scheduler)
                            .doOnSuccess(integer -> {
                                if (integer == null || integer.equals(0)) {
                                    AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceDto);
                                    String uidUtilisateur = annonceDto.getUtilisateur().getUuid();
                                    annonceEntity.setUidUser(uidUtilisateur);

                                    annonceRepository.singleSave(annonceEntity)
                                            .filter(annonceEntity1 -> annonceDto.getPhotos() != null && !annonceDto.getPhotos().isEmpty())
                                            .doOnSuccess(annonceEntity1 -> firebasePhotoStorage.savePhotoToLocalByListUrl(context, annonceEntity1.getIdAnnonce(), annonceDto.getPhotos()))
                                            .doOnError(throwable -> Log.e(TAG, "singleSave.doOnError " + throwable.getMessage()))
                                            .subscribe();
                                }
                            })
                            .doOnError(throwable -> Log.e(TAG, "countByUidUserAndUidAnnonce.doOnError " + throwable.getMessage()))
                            .subscribe();
                })
                .doOnError(throwable -> Log.e(TAG, "synchronize.doOnError " + throwable.getMessage()))
                .subscribe();
    }
}
