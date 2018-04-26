package oliweb.nc.oliweb.database.repository.local;

import android.content.Context;
import android.support.annotation.Nullable;

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

    public void delete(T... entities) {
        AbstractRepositoryCudTask<T> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.DELETE);
        repositoryTask.execute(entities);
    }

    public void delete(@Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute, T... entities) {
        AbstractRepositoryCudTask<T> repositoryTask = new AbstractRepositoryCudTask<>(dao, TypeTask.DELETE, onRespositoryPostExecute);
        repositoryTask.execute(entities);
    }
}
