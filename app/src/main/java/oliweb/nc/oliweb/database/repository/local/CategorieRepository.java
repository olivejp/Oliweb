package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.CategorieDao;
import oliweb.nc.oliweb.database.entity.CategorieEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */

public class CategorieRepository extends AbstractRepository<CategorieEntity> {
    private static final String TAG = CategorieRepository.class.getName();
    private static CategorieRepository INSTANCE;
    private CategorieDao categorieDao;

    private CategorieRepository(Context context) {
        super(context);
        this.categorieDao = db.categorieDao();
        this.dao = categorieDao;
    }

    public static synchronized CategorieRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new CategorieRepository(context);
        }
        return INSTANCE;
    }

    private Single<CategorieEntity> insertSingle(CategorieEntity utilisateurEntity) {
        return Single.create(e -> {
            try {
                Long[] ids = dao.insert(utilisateurEntity);
                if (ids.length == 1) {
                    findSingleById(utilisateurEntity.getIdCategorie())
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
    private Single<CategorieEntity> updateSingle(CategorieEntity entity) {
        return Single.create(e -> {
            try {
                int updatedCount = dao.update(entity);
                if (updatedCount == 1) {
                    findSingleById(entity.getIdCategorie())
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

    public Single<CategorieEntity> saveWithSingle(CategorieEntity categorieEntity) {
        return Single.create(emitter -> existById(categorieEntity.getIdCategorie())
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(emitter::onError)
                .doOnSuccess(atomicBoolean -> {
                    if (atomicBoolean.get()) {
                        updateSingle(categorieEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    } else {
                        insertSingle(categorieEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    }
                })
                .subscribe());
    }

    private Single<AtomicBoolean> existById(Long idCategorie) {
        return Single.create(e -> categorieDao.countById(idCategorie)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }

    public Maybe<List<CategorieEntity>> getListCategorie() {
        return this.categorieDao.getListCategorie();
    }

    public Single<CategorieEntity> findSingleById(long idCategorie) {
        return this.categorieDao.findSingleById(idCategorie);
    }

    public LiveData<CategorieEntity> findById(Long id) {
        return this.categorieDao.findById(id);
    }
}
