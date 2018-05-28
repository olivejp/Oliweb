package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.AnnonceDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class AnnonceRepository extends AbstractRepository<AnnonceEntity, Long> {

    private static final String TAG = AnnonceRepository.class.getName();

    private static AnnonceRepository instance;
    private PhotoRepository photoRepository;
    private ChatRepository chatRepository;
    private FirebasePhotoStorage firebasePhotoStorage;
    private AnnonceDao annonceDao;

    private AnnonceRepository(Context context) {
        super(context);
        this.dao = this.db.getAnnonceDao();
        this.annonceDao = (AnnonceDao) this.dao;
    }

    public static synchronized AnnonceRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AnnonceRepository(context);
            instance.firebasePhotoStorage = FirebasePhotoStorage.getInstance(context);
            instance.photoRepository = PhotoRepository.getInstance(context);
            instance.chatRepository = ChatRepository.getInstance(context);
        }
        return instance;
    }

    public LiveData<AnnonceEntity> findLiveById(long idAnnonce) {
        Log.d(TAG, "Starting findLiveById " + idAnnonce);
        return this.annonceDao.findLiveById(idAnnonce);
    }

    public LiveData<AnnonceEntity> findByUid(String uidAnnonce) {
        Log.d(TAG, "Starting findByUid " + uidAnnonce);
        return this.annonceDao.findByUid(uidAnnonce);
    }

    public Observable<AnnonceEntity> findObservableByUid(String uidAnnonce) {
        Log.d(TAG, "Starting findObservableByUid " + uidAnnonce);
        return this.annonceDao.findSingleByUid(uidAnnonce).toObservable();
    }

    /**
     * Maybe renverra un onComplete si elle trouve déjà une annonce dans les favoris avec l'uid User et l'Uid annonce
     * renverra onSuccess avec l'AnnonceEntity qu'elle viendra de créer en base si l'annonce n'existait pas dans les favoris.
     * renverra onError dans le cas d'une erreur.
     *
     * @param context
     * @param uidUser
     * @param annoncePhotos
     * @return
     */
    public Maybe<AnnonceEntity> saveToFavorite(Context context, String uidUser, AnnoncePhotos annoncePhotos) {
        return Maybe.create(emitter ->
                getAnnonceFavoriteByUidUserAndUidAnnonce(annoncePhotos.getAnnonceEntity().getUidUser(), annoncePhotos.getAnnonceEntity().getUid())
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(annonceEntity -> emitter.onComplete())
                        .doOnComplete(() -> {
                            AnnonceEntity annonceEntity = annoncePhotos.getAnnonceEntity();
                            annonceEntity.setFavorite(1);
                            annonceEntity.setUidUserFavorite(uidUser);
                            singleSave(annonceEntity)
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .doOnError(emitter::onError)
                                    .doOnSuccess(annonceSaved -> {
                                        this.firebasePhotoStorage.saveFromRemoteToLocal(context, annonceSaved.getId(), annoncePhotos.getPhotos());
                                        emitter.onSuccess(annonceSaved);
                                    })
                                    .subscribe();
                        })
                        .subscribe()
        );
    }

    public Flowable<AnnonceEntity> findFlowableByUidUserAndStatusIn(String uidUser, List<String> status) {
        return annonceDao.findFlowableByUidUserAndStatusIn(uidUser, status);
    }

    public Observable<AnnonceEntity> getAllAnnonceByStatus(List<String> status) {
        Log.d(TAG, "Starting getAllAnnonceByStatus " + status);
        return Observable.create(emitter ->
                this.annonceDao.getAllAnnonceByStatus(status)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(listAnnonceStatus -> {
                            for (AnnonceEntity annonce : listAnnonceStatus) {
                                emitter.onNext(annonce);
                            }
                            emitter.onComplete();
                        })
                        .subscribe()
        );
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uidUser, List<String> statusToAvoid) {
        return this.annonceDao.countAllAnnoncesByUser(uidUser, statusToAvoid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uidUser) {
        return this.annonceDao.countAllFavoritesByUser(uidUser);
    }

    public Single<Integer> countByUidUtilisateurAndUidAnnonce(String uidUtilisateur, String uidAnnonce) {
        Log.d(TAG, "Starting countByUidUtilisateurAndUidAnnonce uidUtilisateur : " + uidUtilisateur + " uidAnnonce : " + uidAnnonce);
        return this.annonceDao.existByUidUtilisateurAndUidAnnonce(uidUtilisateur, uidAnnonce);
    }

    /**
     * Will return > 0 if true
     *
     * @param uidAnnonce
     * @return
     */
    public Single<Integer> isAnnonceFavorite(String uidUser, String uidAnnonce) {
        return this.annonceDao.isAnnonceFavorite(uidUser, uidAnnonce);
    }

    /**
     * Will return > 0 if true
     *
     * @param uidAnnonce
     * @return
     */
    public Maybe<AnnonceEntity> getAnnonceFavoriteByUidUserAndUidAnnonce(String uidUser, String uidAnnonce) {
        return this.annonceDao.getAnnonceFavoriteNotTheAuthor(uidUser, uidAnnonce);
    }

    public Single<AtomicBoolean> markToDelete(Long idAnnonce) {
        Log.d(TAG, "Starting markToDeleteByAnnonce idAnnonce : " + idAnnonce);
        return Single.create(emitter ->
                findById(idAnnonce)
                        .doOnComplete(() -> emitter.onError(new RuntimeException("No annonce to mark to delete")))
                        .toObservable()
                        .switchMap(this::markAsToDelete)
                        .doOnNext(annonceEntity -> {
                            photoRepository.markToDeleteByAnnonce(annonceEntity)
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .subscribe();
                            chatRepository.markToDeleteByUidAnnonceAndUidUser(annonceEntity.getUidUser(), annonceEntity.getUid())
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .subscribe();
                        })
                        .subscribe()
        );
    }

    public Observable<AnnonceEntity> markAsSending(AnnonceEntity annonceEntity) {
        Log.d(TAG, "markAsSending annonceEntity : " + annonceEntity);
        annonceEntity.setStatut(StatusRemote.SENDING);
        return this.singleSave(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    public Observable<AnnonceEntity> markAsSend(AnnonceEntity annonceEntity) {
        Log.d(TAG, "markAsSend annonceEntity : " + annonceEntity);
        annonceEntity.setStatut(StatusRemote.SEND);
        return this.singleSave(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    public Observable<AnnonceEntity> markAsToDelete(AnnonceEntity annonceEntity) {
        Log.d(TAG, "markAsToDelete annonceEntity : " + annonceEntity);
        annonceEntity.setStatut(StatusRemote.TO_DELETE);
        return this.singleSave(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    public Observable<AnnonceEntity> markAnnonceAsFailedToSend(AnnonceEntity annonceEntity) {
        Log.d(TAG, "markAnnonceAsFailedToSend annonceEntity : " + annonceEntity);
        annonceEntity.setStatut(StatusRemote.FAILED_TO_SEND);
        return this.singleSave(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    public Observable<AnnonceEntity> markAnnonceAsFailedToDelete(AnnonceEntity annonceEntity) {
        Log.d(TAG, "markAnnonceAsFailedToDelete annonceEntity : " + annonceEntity);
        annonceEntity.setStatut(StatusRemote.FAILED_TO_DELETE);
        return this.singleSave(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }
}
