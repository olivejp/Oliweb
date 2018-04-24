package oliweb.nc.oliweb.database.repository;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AbstractDao;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;
import oliweb.nc.oliweb.database.repository.task.TypeTask;

/**
 * Created by 2761oli on 29/01/2018.
 * T : Entity
 */
public abstract class AbstractRepository<T> {
    protected AbstractDao<T> dao;
    protected OliwebDatabase db;

    AbstractRepository(Context context) {
        db = OliwebDatabase.getInstance(context);
    }

    /**
     * Cette méthode doit absolument être appelée dans le constructeur des classes filles
     *
     * @param dao
     */
    void setDao(AbstractDao<T> dao) {
        this.dao = dao;
    }

    /**
     * You have to subscribe to this Single on a background thread
     * because it queries the Database which only accept background queries.
     */
    public Single<AtomicBoolean> insertSingle(T... entities) {
        return Single.create(e -> {
            try {
                Long[] ids = dao.insert(entities);
                if (ids.length == entities.length) {
                    e.onSuccess(new AtomicBoolean(true));
                } else {
                    e.onSuccess(new AtomicBoolean(false));
                }
            } catch (Exception exception) {
                Log.e("AbstractRepository", exception.getMessage());
                e.onError(exception);
            }
        });
    }

    public void insert(T... entities) {
        AbstractRepositoryCudTask<T> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.INSERT);
        repositoryTask.execute(entities);
    }

    public void insert(@Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute, T... entities) {
        AbstractRepositoryCudTask<T> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.INSERT, onRespositoryPostExecute);
        repositoryTask.execute(entities);
    }

    public void update(T... entities) {
        AbstractRepositoryCudTask<T> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.UPDATE);
        repositoryTask.execute(entities);
    }

    public void update(@Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute, T... entities) {
        AbstractRepositoryCudTask<T> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.UPDATE, onRespositoryPostExecute);
        repositoryTask.execute(entities);
    }

    /**
     * You have to subscribe to this Single on a background thread
     * because it queries the Database which only accept background queries.
     */
    public Single<AtomicBoolean> updateSingle(T... entities) {
        return Single.create(e -> {
            try {
                int updatedCount = dao.update(entities);
                if (updatedCount == entities.length) {
                    e.onSuccess(new AtomicBoolean(true));
                } else {
                    e.onSuccess(new AtomicBoolean(false));
                }
            } catch (Exception exception) {
                Log.e("AbstractRepositoryCudTa", exception.getMessage());
                e.onError(exception);
            }
        });
    }

    public void delete(T... entities) {
        AbstractRepositoryCudTask<T> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.DELETE);
        repositoryTask.execute(entities);
    }

    public void delete(@Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute, T... entities) {
        AbstractRepositoryCudTask<T> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.DELETE, onRespositoryPostExecute);
        repositoryTask.execute(entities);
    }
}
