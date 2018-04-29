package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.AnnonceDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class AnnonceRepository extends AbstractRepository<AnnonceEntity> {

    private static final String TAG = AnnonceRepository.class.getName();

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

    public Single<AtomicBoolean> existById(Long idAnnonce) {
        return Single.create(e -> annonceDao.countById(idAnnonce)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }

    private Single<AnnonceEntity> insertSingle(AnnonceEntity entity) {
        Log.d(TAG, "Starting insertSingle " + entity.toString());
        return Single.create(e -> {
            try {
                Long[] ids = dao.insert(entity);
                if (ids.length == 1) {
                    findSingleById(ids[0])
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(e::onSuccess)
                            .doOnError(e::onError)
                            .subscribe();
                } else {
                    e.onError(new RuntimeException("Failed to insert into AnnonceRepository"));
                }
            } catch (Exception exception) {
                Log.e(TAG, exception.getMessage());
                e.onError(exception);
            }
        });
    }

    /**
     * You have to subscribe to this Single on a background thread
     * because it queries the Database which only accept background queries.
     */
    private Single<AnnonceEntity> updateSingle(AnnonceEntity entity) {
        Log.d(TAG, "Starting updateSingle " + entity.toString());
        return Single.create(e -> {
            try {
                int updatedCount = dao.update(entity);
                if (updatedCount == 1) {
                    findSingleById(entity.getIdAnnonce())
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(e::onSuccess)
                            .subscribe();
                } else {
                    e.onError(new RuntimeException("Failed to update into AnnonceRepository"));
                }
            } catch (Exception exception) {
                Log.e(TAG, exception.getMessage());
                e.onError(exception);
            }
        });
    }

    public Single<AnnonceEntity> saveWithSingle(AnnonceEntity annonceEntity) {
        Log.d(TAG, "Starting saveWithSingle " + annonceEntity.toString());
        return Single.create(emitter -> existById(annonceEntity.getIdAnnonce())
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(e -> {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                    emitter.onError(e);
                })
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

    public Maybe<List<AnnonceEntity>> saveWithSingle(List<AnnonceEntity> annonceEntityList) {
        Log.d(TAG, "Starting saveWithSingle annonceEntityList : " + annonceEntityList);
        return Maybe.create(emitter -> {
            if (annonceEntityList == null || annonceEntityList.isEmpty()) {
                emitter.onSuccess(Collections.emptyList());
            } else {
                AtomicInteger countSuccess = new AtomicInteger();
                ArrayList<AnnonceEntity> listResult = new ArrayList<>();
                for (AnnonceEntity annonce : annonceEntityList) {
                    saveWithSingle(annonce)
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(annonceEntity -> {
                                Log.d(TAG, "saveWithSingle.doOnSuccess annonceEntity : " + annonceEntity);
                                listResult.add(annonceEntity);
                                countSuccess.getAndIncrement();
                                if (countSuccess.get() == annonceEntityList.size()) {
                                    emitter.onSuccess(listResult);
                                }
                            })
                            .doOnError(emitter::onError)
                            .subscribe();
                }
            }
        });
    }

    public LiveData<AnnonceEntity> findById(long idAnnonce) {
        Log.d(TAG, "Starting findById " + idAnnonce);
        return this.annonceDao.findById(idAnnonce);
    }

    public LiveData<AnnonceEntity> findByUid(String uidAnnonce) {
        Log.d(TAG, "Starting findByUid " + uidAnnonce);
        return this.annonceDao.findByUid(uidAnnonce);
    }

    public Maybe<AnnonceEntity> findSingleById(long idAnnonce) {
        Log.d(TAG, "Starting findSingleById " + idAnnonce);
        return this.annonceDao.findSingleById(idAnnonce);
    }

    public Maybe<List<AnnonceEntity>> getAllAnnonceByStatus(String status) {
        Log.d(TAG, "Starting getAllAnnonceByStatus " + status);
        return this.annonceDao.getAllAnnonceByStatus(status);
    }

    public Flowable<Integer> countFlowableAllAnnoncesByStatus(String status) {
        return this.annonceDao.countFlowableAllAnnoncesByStatus(status);
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
