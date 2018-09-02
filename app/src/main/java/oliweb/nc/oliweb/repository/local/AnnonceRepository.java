package oliweb.nc.oliweb.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.AnnonceDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.StatusRemote;

/**
 * Created by 2761oli on 29/01/2018.
 */
@Singleton
public class AnnonceRepository extends AbstractRepository<AnnonceEntity, Long> {

    private static final String TAG = AnnonceRepository.class.getName();

    private PhotoRepository photoRepository;
    private ChatRepository chatRepository;

    private AnnonceDao annonceDao;

    @Inject
    public AnnonceRepository(Context context, PhotoRepository photoRepository, ChatRepository chatRepository) {
        super(context);
        this.dao = this.db.getAnnonceDao();
        this.annonceDao = (AnnonceDao) this.dao;
        this.photoRepository = photoRepository;
        this.chatRepository = chatRepository;
    }

    public LiveData<AnnonceEntity> findLiveById(long idAnnonce) {
        return this.annonceDao.findLiveById(idAnnonce);
    }

    public LiveData<AnnonceEntity> findByUid(String uidAnnonce) {
        return this.annonceDao.findByUid(uidAnnonce);
    }

    public Maybe<AnnonceEntity> findMaybeByUidAndFavorite(String uidAnnonce, int favorite) {
        return this.annonceDao.findMaybeByUidAndFavorite(uidAnnonce, favorite);
    }


    public Flowable<AnnonceEntity> findFlowableByUidUserAndStatusIn(String uidUser, List<String> status) {
        return annonceDao.findFlowableByUidUserAndStatusIn(uidUser, status);
    }

    public Single<List<AnnonceEntity>> findAllByStatus(List<String> status) {
        return this.annonceDao.getAllAnnonceByStatus(status);
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uidUser, List<String> statusToAvoid) {
        return this.annonceDao.countAllAnnoncesByUser(uidUser, statusToAvoid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uidUser) {
        return this.annonceDao.countAllFavoritesByUser(uidUser);
    }

    public Single<Integer> countByUidUserAndUidAnnonce(String uidUtilisateur, String uidAnnonce) {
        return this.annonceDao.existByUidUtilisateurAndUidAnnonce(uidUtilisateur, uidAnnonce);
    }

    /**
     * Retire l'annonce des favoris
     *
     * @param uidCurrentUser
     * @param annoncePhotos
     * @return
     */
    public void removeFromFavorite(String uidCurrentUser, AnnoncePhotos annoncePhotos) {
        annonceDao.deleteFromFavorite(uidCurrentUser, annoncePhotos.getAnnonceEntity().getUid());
    }

    /**
     * Will return > 0 if true
     *
     * @param uidAnnonce
     * @return
     */
    public Maybe<AnnonceEntity> getAnnonceFavoriteByUidUserAndUidAnnonce(String uidUser, String uidAnnonce) {
        return this.annonceDao.getAnnonceFavoriteByUidUserAndUidAnnonce(uidUser, uidAnnonce);
    }

    public Single<AtomicBoolean> markAsToDelete(Long idAnnonce) {
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
                            emitter.onSuccess(new AtomicBoolean(true));
                        })
                        .subscribe()
        );
    }

    public Observable<AnnonceEntity> markAsSending(AnnonceEntity annonceEntity) {
        annonceEntity.setStatut(StatusRemote.SENDING);
        return this.singleSave(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    public Observable<AnnonceEntity> markAsSend(AnnonceEntity annonceEntity) {
        annonceEntity.setStatut(StatusRemote.SEND);
        return this.singleSave(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    private Observable<AnnonceEntity> markAsToDelete(AnnonceEntity annonceEntity) {
        annonceEntity.setStatut(StatusRemote.TO_DELETE);
        return this.singleSave(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    public Observable<AnnonceEntity> markAsFailedToSend(AnnonceEntity annonceEntity) {
        annonceEntity.setStatut(StatusRemote.FAILED_TO_SEND);
        return this.singleSave(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    public Observable<AnnonceEntity> markAsFailedToDelete(AnnonceEntity annonceEntity) {
        annonceEntity.setStatut(StatusRemote.FAILED_TO_DELETE);
        return this.singleSave(annonceEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }
}
