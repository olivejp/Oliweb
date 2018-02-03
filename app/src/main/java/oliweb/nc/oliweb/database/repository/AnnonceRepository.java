package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AnnonceDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class AnnonceRepository extends AbstractRepository<AnnonceEntity> {
    private static AnnonceRepository INSTANCE;
    private AnnonceDao annonceDao;

    private AnnonceRepository(Context context) {
        OliwebDatabase db = OliwebDatabase.getInstance(context);
        this.annonceDao = db.AnnonceDao();
        setDao(annonceDao);
    }

    public static synchronized AnnonceRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnnonceRepository(context);
        }
        return INSTANCE;
    }

    public boolean exist(AnnonceEntity annonceEntity) {
        return (this.annonceDao.findById(annonceEntity.getIdAnnonce()) != null);
    }

    public void save(AnnonceEntity annonceEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        if (exist(annonceEntity)) {
            update(onRespositoryPostExecute, annonceEntity);
        } else {
            insert(onRespositoryPostExecute, annonceEntity);
        }
    }

    public LiveData<AnnonceEntity> findById(long idAnnonce) {
        return this.annonceDao.findById(idAnnonce);
    }
}
