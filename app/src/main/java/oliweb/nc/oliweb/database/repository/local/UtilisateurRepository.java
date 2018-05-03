package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.UtilisateurDao;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class UtilisateurRepository extends AbstractRepository<UtilisateurEntity> {
    private static final String TAG = UtilisateurRepository.class.getName();
    private static UtilisateurRepository INSTANCE;
    private UtilisateurDao utilisateurDao;

    private UtilisateurRepository(Context context) {
        super(context);
        this.utilisateurDao = this.db.getUtilisateurDao();
        this.dao = utilisateurDao;
    }

    public static synchronized UtilisateurRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new UtilisateurRepository(context);
        }
        return INSTANCE;
    }

    public LiveData<UtilisateurEntity> findByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findByUuid(uuidUtilisateur);
    }

    public Maybe<UtilisateurEntity> findSingleByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findSingleByUuid(uuidUtilisateur);
    }

    private Single<UtilisateurEntity> insertSingle(UtilisateurEntity utilisateurEntity) {
        return Single.create(e -> {
            try {
                Long[] ids = dao.insert(utilisateurEntity);
                if (ids.length == 1) {
                    findSingleByUid(utilisateurEntity.getUuidUtilisateur())
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(e::onSuccess)
                            .subscribe();
                } else {
                    e.onError(new RuntimeException("Failed to insert into UtilisateurRepository"));
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
    private Single<UtilisateurEntity> updateSingle(UtilisateurEntity entity) {
        return Single.create(e -> {
            try {
                int updatedCount = dao.update(entity);
                if (updatedCount == 1) {
                    findSingleByUid(entity.getUuidUtilisateur())
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(e::onSuccess)
                            .subscribe();
                } else {
                    e.onError(new RuntimeException("Failed to update into UtilisateurRepository"));
                }
            } catch (Exception exception) {
                Log.e(TAG, exception.getMessage());
                e.onError(exception);
            }
        });
    }

    public Single<UtilisateurEntity> saveWithSingle(UtilisateurEntity utilisateurEntity) {
        return Single.create(emitter -> existByUid(utilisateurEntity.getUuidUtilisateur())
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(emitter::onError)
                .doOnSuccess(atomicBoolean -> {
                    if (atomicBoolean.get()) {
                        updateSingle(utilisateurEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    } else {
                        insertSingle(utilisateurEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    }
                })
                .subscribe());
    }

    public Single<AtomicBoolean> existByUid(String uidUser) {
        return Single.create(e -> utilisateurDao.countByUid(uidUser)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }

    public Single<List<UtilisateurEntity>> getAll() {
        return utilisateurDao.getAll();
    }

    /**
     * @return Single to be observe. This Single emit an integer which represent the number of user correctly deleted
     */
    public Single<Integer> deleteAll() {
        return Single.create(e -> utilisateurDao.getAll()
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(utilisateurEntities -> {
                    if (utilisateurEntities != null && !utilisateurEntities.isEmpty()) {
                        e.onSuccess(utilisateurDao.delete(utilisateurEntities));
                    } else {
                        e.onSuccess(0);
                    }
                })
                .doOnError(e::onError)
                .subscribe());
    }

    public Single<Integer> count() {
        return this.utilisateurDao.count();
    }
}
