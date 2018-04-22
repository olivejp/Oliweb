package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.EmptyResultSetException;
import android.content.Context;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.UtilisateurDao;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class UtilisateurRepository extends AbstractRepository<UtilisateurEntity> {
    private static UtilisateurRepository INSTANCE;
    private UtilisateurDao utilisateurDao;

    private UtilisateurRepository(Context context) {
        super(context);
        this.utilisateurDao = this.db.utilisateurDao();
        this.dao = utilisateurDao;
    }

    public static synchronized UtilisateurRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new UtilisateurRepository(context);
        }
        return INSTANCE;
    }

    public LiveData<UtilisateurEntity> findByUid(String UuidUtilisateur) {
        return this.utilisateurDao.findByUuid(UuidUtilisateur);
    }

    public Single<UtilisateurEntity> findSingleByUid(String UuidUtilisateur) {
        return this.utilisateurDao.findSingleByUuid(UuidUtilisateur);
    }

    public Single<AtomicBoolean> save(UtilisateurEntity utilisateurEntity) {
        return Single.create(emitter -> existByUid(utilisateurEntity.getUuidUtilisateur())
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(exception -> {
                    if (exception instanceof EmptyResultSetException) {
                        emitter.onSuccess(new AtomicBoolean(false));
                    } else {
                        emitter.onError(exception);
                    }
                })
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
        return Single.create(e -> utilisateurDao.findSingleByUuid(uidUser)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(utilisateurEntity -> e.onSuccess(new AtomicBoolean(utilisateurEntity != null)))
                .doOnError(exception -> {
                    if (exception instanceof EmptyResultSetException) {
                        e.onSuccess(new AtomicBoolean(false));
                    } else {
                        e.onError(exception);
                    }
                })
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
