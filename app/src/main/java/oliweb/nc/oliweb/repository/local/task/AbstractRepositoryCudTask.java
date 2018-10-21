package oliweb.nc.oliweb.repository.local.task;

import android.os.AsyncTask;
import androidx.annotation.Nullable;
import android.util.Log;

import oliweb.nc.oliweb.database.dao.AbstractDao;

/**
 * Created by orlanth23 on 28/01/2018.
 */

public class AbstractRepositoryCudTask<T, U> extends AsyncTask<T, Void, DataReturn<U>> {

    private TypeTask typeTask;
    private AbstractDao<T, U> dao;
    private OnRespositoryPostExecute postExecute;

    public AbstractRepositoryCudTask(AbstractDao<T, U> dao, TypeTask typeTask, @Nullable OnRespositoryPostExecute postExecute) {
        this.typeTask = typeTask;
        this.dao = dao;
        this.postExecute = postExecute;
    }

    public AbstractRepositoryCudTask(AbstractDao<T, U> dao, TypeTask typeTask) {
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
                    U[] ids = this.dao.insert(entities);
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
