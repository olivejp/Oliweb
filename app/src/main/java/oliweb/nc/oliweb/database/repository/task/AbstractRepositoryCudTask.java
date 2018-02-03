package oliweb.nc.oliweb.database.repository.task;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import oliweb.nc.oliweb.database.dao.AbstractDao;

/**
 * Created by orlanth23 on 28/01/2018.
 */

public class AbstractRepositoryCudTask<T> extends AsyncTask<T, Void, Long[]> {

    private TypeTask typeTask;
    private AbstractDao<T> dao;
    private OnRespositoryPostExecute postExecute;

    public AbstractRepositoryCudTask(AbstractDao<T> dao, TypeTask typeTask, @Nullable OnRespositoryPostExecute postExecute) {
        this.typeTask = typeTask;
        this.dao = dao;
        this.postExecute = postExecute;
    }

    @Override
    protected Long[] doInBackground(T... entities) {
        Long[] returnValue = new Long[]{};
        switch (this.typeTask) {
            case DELETE:
                int nbDeleted;
                nbDeleted = this.dao.delete(entities);
                returnValue[0] = (long) nbDeleted;
                return returnValue;
            case INSERT:
                return this.dao.insert(entities);
            case UPDATE:
                int nbUpdated;
                nbUpdated = this.dao.update(entities);
                returnValue[0] = (long) nbUpdated;
                return returnValue;
            default:
                return new Long[0];
        }
    }

    @Override
    protected void onPostExecute(Long[] longs) {
        super.onPostExecute(longs);
        if (this.postExecute != null) {
            this.postExecute.onReposirotyPostExecute(longs);
        }
    }

    public interface OnRespositoryPostExecute {
        void onReposirotyPostExecute(Long[] ids);
    }
}
