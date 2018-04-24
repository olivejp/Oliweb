package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.AnnonceDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class AnnonceRepository extends AbstractRepository<AnnonceEntity> {
    private static AnnonceRepository INSTANCE;
    private AnnonceDao annonceDao;

    private AnnonceRepository(Context context) {
        super(context);
        this.dao = this.db.getAnnonceDao();
        this.annonceDao = (AnnonceDao) this.dao;
    }

    public static synchronized AnnonceRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnnonceRepository(context);
        }
        return INSTANCE;
    }


    public void save(AnnonceEntity annonceEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        this.annonceDao.findSingleById(annonceEntity.getIdAnnonce())
                .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                .subscribe((annonceEntity1, throwable) -> {
                    if (throwable != null) {
                        // This annonce don't exists already, create it
                        insert(onRespositoryPostExecute, annonceEntity);
                    } else {
                        if (annonceEntity1 != null) {
                            // Annonce exists, just update it
                            update(onRespositoryPostExecute, annonceEntity);
                        }
                    }
                });
    }

    public Single<AtomicBoolean> existById(Long idAnnonce) {
        return Single.create(e -> annonceDao.countById(idAnnonce)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }

    public Single<AtomicBoolean> saveWithSingle(AnnonceEntity annonceEntity) {
        return Single.create(emitter -> existById(annonceEntity.getIdAnnonce())
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(emitter::onError)
                .doOnSuccess(atomicBoolean -> {
                    if (atomicBoolean.get()) {
                        updateSingle(annonceEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    } else {
                        insertSingle(annonceEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    }
                })
                .subscribe());
    }

    public LiveData<AnnonceEntity> findById(long idAnnonce) {
        return this.annonceDao.findById(idAnnonce);
    }

    public LiveData<AnnonceEntity> findByUid(String uidAnnonce) {
        return this.annonceDao.findByUid(uidAnnonce);
    }

    public Single<AnnonceEntity> findSingleById(long idAnnonce) {
        return this.annonceDao.findSingleById(idAnnonce);
    }

    public Maybe<List<AnnonceEntity>> getAllAnnonceByStatus(String status) {
        return this.annonceDao.getAllAnnonceByStatus(status);
    }

    public Flowable<Integer> countFlowableAllAnnoncesByStatus(String status) {
        return this.annonceDao.countFlowableAllAnnoncesByStatus(status);
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uidUser) {
        return this.annonceDao.countAllAnnoncesByUser(uidUser);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uidUser) {
        return this.annonceDao.countAllFavoritesByUser(uidUser);
    }

    public Single<Integer> countByUidUtilisateurAndUidAnnonce(String uidUtilisateur, String uidAnnonce) {
        return this.annonceDao.existByUidUtilisateurAndUidAnnonce(uidUtilisateur, uidAnnonce);
    }

    public Single<Integer> isAnnonceFavorite(String uidAnnonce) {
        return this.annonceDao.isAnnonceFavorite(uidAnnonce);
    }

    public Single<List<AnnonceEntity>> getAll() {
        return annonceDao.getAll();
    }

    /**
     * @return Single to be observe. This Single emit an integer which represent the number of annonce correctly deleted
     */
    public Single<Integer> deleteAll() {
        return Single.create(e -> annonceDao.getAll()
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(annonceEntities -> {
                    if (annonceEntities != null && !annonceEntities.isEmpty()) {
                        e.onSuccess(annonceDao.delete(annonceEntities));
                    } else {
                        e.onSuccess(0);
                    }
                })
                .doOnError(e::onError)
                .subscribe());
    }

    public Single<Integer> count() {
        return this.annonceDao.count();
    }
}
