package oliweb.nc.oliweb.repository.local;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.Log;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AbstractDao;
import oliweb.nc.oliweb.database.entity.AbstractEntity;
import oliweb.nc.oliweb.repository.local.task.AbstractRepositoryCudTask;
import oliweb.nc.oliweb.repository.local.task.TypeTask;

/**
 * Created by 2761oli on 29/01/2018.
 * T : Entity
 */
public abstract class AbstractRepository<T extends AbstractEntity<U>, U> {
    private static final String TAG = AbstractRepository.class.getName();
    protected AbstractDao<T, U> dao;
    protected OliwebDatabase db;

    AbstractRepository(Context context) {
        db = OliwebDatabase.getInstance(context);
    }

    /**
     * Cette méthode doit absolument être appelée dans le constructeur des classes filles
     *
     * @param dao
     */
    void setDao(AbstractDao<T, U> dao) {
        this.dao = dao;
    }

    public AbstractRepositoryCudTask<T, U> insert(T... entities) {
        AbstractRepositoryCudTask<T, U> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.INSERT);
        repositoryTask.execute(entities);
        return repositoryTask;
    }

    public void insert(@Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute, T... entities) {
        AbstractRepositoryCudTask<T, U> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.INSERT, onRespositoryPostExecute);
        repositoryTask.execute(entities);
    }

    public void update(T... entities) {
        AbstractRepositoryCudTask<T, U> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.UPDATE);
        repositoryTask.execute(entities);
    }

    public void update(@Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute, T... entities) {
        AbstractRepositoryCudTask<T, U> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.UPDATE, onRespositoryPostExecute);
        repositoryTask.execute(entities);
    }

    public void delete(T entities) {
        AbstractRepositoryCudTask<T, U> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.DELETE);
        repositoryTask.execute(entities);
    }

    public void delete(T... entities) {
        AbstractRepositoryCudTask<T, U> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.DELETE);
        repositoryTask.execute(entities);
    }

    public void delete(@Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute, T... entities) {
        AbstractRepositoryCudTask<T, U> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.DELETE, onRespositoryPostExecute);
        repositoryTask.execute(entities);
    }

    public Maybe<T> findById(U id) {
        return this.dao.findById(id);
    }

    public Maybe<T> singleInsert(T entity) {
        Log.d(TAG, "singleUpdate:" + entity.toString());
        U id = dao.insert(entity);
        if (id != null) {
            return findById(id);
        } else {
            return Maybe.empty();
        }
    }

    public Maybe<T> singleUpdate(T entity) {
        Log.d(TAG, "singleUpdate:" + entity.toString());
        int updatedCount = dao.update(entity);
        if (updatedCount == 1) {
            return findById(entity.getId());
        } else {
            return Maybe.empty();
        }
    }

    public Single<T> singleSave(T entity) {
        return Single.create(emitter -> findById(entity.getId())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(emitter::onError)
                .doOnSuccess(entityRead ->
                        singleUpdate(entity)
                                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .doOnComplete(() -> Log.e(TAG, "Failed to update"))
                                .subscribe()
                )
                .doOnComplete(() ->
                        singleInsert(entity)
                                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .doOnComplete(() -> Log.e(TAG, "Failed to insert"))
                                .subscribe()
                ).subscribe()
        );
    }

    public Single<List<T>> getAll() {
        return dao.getAll();
    }

    public Single<Integer> deleteAll() {
        Log.d(TAG, "deleteAll");
        return Single.create(e -> dao.getAll()
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(entity -> {
                    if (entity != null && !entity.isEmpty()) {
                        e.onSuccess(dao.delete(entity));
                    } else {
                        e.onSuccess(0);
                    }
                })
                .doOnError(e::onError)
                .subscribe());
    }

    public Single<Integer> count() {
        return this.dao.count();
    }
}
