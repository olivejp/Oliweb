package oliweb.nc.oliweb.service.firebase;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.dto.elasticsearch.AnnonceDto;

/**
 * Created by orlanth23 on 03/03/2018.
 * This class allows to retrieve {@link AnnonceDto} from Firebase corresponding to the given UID User.
 */
@Singleton
public class FirebaseRetrieverService {

    private static final String TAG = FirebaseRetrieverService.class.getCanonicalName();
    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private AnnonceRepository annonceRepository;
    private FirebasePhotoStorage firebasePhotoStorage;

    @Inject
    public FirebaseRetrieverService(FirebaseAnnonceRepository firebaseAnnonceRepository,
                                    AnnonceRepository annonceRepository,
                                    FirebasePhotoStorage firebasePhotoStorage) {
        this.firebaseAnnonceRepository = firebaseAnnonceRepository;
        this.annonceRepository = annonceRepository;
        this.firebasePhotoStorage = firebasePhotoStorage;
    }

    /**
     * Retrieve all the annonces on the Fb database for the specified User uid.
     * Then we try to find them in the local DB.
     * If not present the MutableLiveData shouldAskQuestion will receive True.
     *
     * @param uidUser
     * @param shouldAskQuestion
     */
    public void checkFirebaseRepository(final String uidUser, @NonNull MutableLiveData<AtomicBoolean> shouldAskQuestion) {
        Log.d(TAG, "Starting checkFirebaseRepository called with uidUser = " + uidUser);
        firebaseAnnonceRepository.observeAllAnnonceByUidUser(uidUser)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(throwable -> Log.e(TAG, throwable.getMessage()))
                .switchMapSingle(annonceDto -> annonceRepository.countByUidUserAndUidAnnonce(uidUser, annonceDto.getUuid()))
                .filter(integer -> integer != null && integer == 0)
                .doOnNext(integer -> shouldAskQuestion.postValue(new AtomicBoolean(true)))
                .subscribe();
    }

    /**
     * Lecture de toutes les annonces présentes dans Firebase pour cet utilisateur
     * et récupération de ces annonces dans la base locale
     */
    public void synchronize(Context context, String uidUser) {
        firebaseAnnonceRepository.observeAllAnnonceByUidUser(uidUser)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnNext(annonceDto -> {
                    Log.d(TAG, "Starting saveAnnonceDtoToLocalDb called with annonceDto = " + annonceDto.toString());
                    annonceRepository.countByUidUserAndUidAnnonce(annonceDto.getUtilisateur().getUuid(), annonceDto.getUuid())
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnError(throwable -> Log.e(TAG, "countByUidUserAndUidAnnonce.doOnError " + throwable.getMessage()))
                            .filter(integer -> integer == null || integer.equals(0))
                            .map(integer -> {
                                AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceDto);
                                String uidUtilisateur = annonceDto.getUtilisateur().getUuid();
                                annonceEntity.setUidUser(uidUtilisateur);
                                return annonceEntity;
                            })
                            .flatMapSingle(annonceEntity -> annonceRepository.singleSave(annonceEntity))
                            .filter(annonceEntity -> annonceDto.getPhotos() != null && !annonceDto.getPhotos().isEmpty())
                            .doOnSuccess(annonceEntity1 -> firebasePhotoStorage.savePhotoToLocalByListUrl(context, annonceEntity1.getIdAnnonce(), annonceDto.getPhotos()))
                            .subscribe();
                })
                .subscribe();
    }
}
