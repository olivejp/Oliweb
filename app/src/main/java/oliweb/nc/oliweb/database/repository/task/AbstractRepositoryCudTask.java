package oliweb.nc.oliweb.database.repository.task;

import android.os.AsyncTask;
import android.support.annotation.Nullable;

import oliweb.nc.oliweb.database.dao.AbstractDao;

/**
 * Created by orlanth23 on 28/01/2018.
 */

public class AbstractRepositoryCudTask<T> extends AsyncTask<T, Void, AbstractRepositoryCudTask.DataReturn> {

    private TypeTask typeTask;
    private AbstractDao<T> dao;
    private OnRespositoryPostExecute postExecute;

    public AbstractRepositoryCudTask(AbstractDao<T> dao, TypeTask typeTask, @Nullable OnRespositoryPostExecute postExecute) {
        this.typeTask = typeTask;
        this.dao = dao;
        this.postExecute = postExecute;
    }

    @Override
    protected DataReturn doInBackground(T... entities) {
        DataReturn dataReturn = new DataReturn();
        dataReturn.setTypeTask(this.typeTask);
        switch (this.typeTask) {
            case DELETE:
                dataReturn.setNb(this.dao.delete(entities));
                break;
            case INSERT:
                Long[] ids = this.dao.insert(entities);
                dataReturn.setNb(ids.length);
                dataReturn.setIds(ids);
                break;
            case UPDATE:
                dataReturn.setNb(this.dao.update(entities));
                break;
            default:
                break;
        }
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

    public static class DataReturn {
        int nb;
        TypeTask typeTask;
        Long[] ids;

        public int getNb() {
            return nb;
        }

        public void setNb(int nb) {
            this.nb = nb;
        }

        public TypeTask getTypeTask() {
            return typeTask;
        }

        public void setTypeTask(TypeTask typeTask) {
            this.typeTask = typeTask;
        }

        public Long[] getIds() {
            return ids;
        }

        public void setIds(Long[] ids) {
            this.ids = ids;
        }
    }
}
