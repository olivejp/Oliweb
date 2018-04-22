package oliweb.nc.oliweb.database.repository.task;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;

import oliweb.nc.oliweb.database.dao.AbstractDao;

/**
 * Created by orlanth23 on 28/01/2018.
 */

public class AbstractRepositoryCudTask<T> extends AsyncTask<T, Void, DataReturn> {

    private TypeTask typeTask;
    private AbstractDao<T> dao;
    private OnRespositoryPostExecute postExecute;

    public AbstractRepositoryCudTask(AbstractDao<T> dao, TypeTask typeTask, @Nullable OnRespositoryPostExecute postExecute) {
        this.typeTask = typeTask;
        this.dao = dao;
        this.postExecute = postExecute;
    }

    public AbstractRepositoryCudTask(AbstractDao<T> dao, TypeTask typeTask) {
        this.typeTask = typeTask;
        this.dao = dao;
        this.postExecute = null;
    }

    @Override
    protected DataReturn doInBackground(T... entities) {
        DataReturn dataReturn = new DataReturn();
        dataReturn.setTypeTask(this.typeTask);
        switch (this.typeTask) {
            case DELETE:
                try {
                    dataReturn.setIds(null);
                    dataReturn.setNb(this.dao.delete(entities));
                } catch (RuntimeException e) {
                    Log.e("AbstractRepositoryCudTa", e.getMessage());
                    dataReturn.setThrowable(e);
                }
                break;
            case INSERT:
                try {
                    Long[] ids = this.dao.insert(entities);
                    dataReturn.setNb(ids.length);
                    dataReturn.setIds(ids);
                } catch (RuntimeException e) {
                    Log.e("AbstractRepositoryCudTa", e.getMessage());
                    dataReturn.setThrowable(e);
                }
                break;
            case UPDATE:
                try {
                    dataReturn.setIds(null);
                    dataReturn.setNb(this.dao.update(entities));
                } catch (RuntimeException e) {
                    Log.e("AbstractRepositoryCudTa", e.getMessage());
                    dataReturn.setThrowable(e);
                }
                break;
            default:
                break;
        }
        dataReturn.setSuccessful(dataReturn.getNb() > 0);
        return dataReturn;
    }

    @Override
    protected void onPostExecute(DataReturn dataReturn) {
        super.onPostExecute(dataReturn);
        if (this.postExecute != null) {
            this.postExecute.onReposirotyPostExecute(dataReturn);
        }
    }

    public interface OnRespositoryPostExecute {
        void onReposirotyPostExecute(DataReturn dataReturn);
    }


}
