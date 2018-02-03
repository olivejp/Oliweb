package oliweb.nc.oliweb.database.repository;

import android.content.Context;
import android.support.annotation.Nullable;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.PhotoDao;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class PhotoRepository extends AbstractRepository<PhotoEntity> {
    private static PhotoRepository INSTANCE;
    private PhotoDao photoDao;

    private PhotoRepository(Context context) {
        OliwebDatabase db = OliwebDatabase.getInstance(context);
        this.photoDao = db.PhotoDao();
        setDao(photoDao);
    }

    public static synchronized PhotoRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new PhotoRepository(context);
        }
        return INSTANCE;
    }

    public boolean exist(PhotoEntity photoEntity) {
        return (this.photoDao.findById(photoEntity.getIdPhoto()) != null);
    }

    public void save(PhotoEntity photoEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        if (exist(photoEntity)) {
            update(onRespositoryPostExecute, photoEntity);
        } else {
            insert(onRespositoryPostExecute, photoEntity);
        }
    }

    public Single<PhotoEntity> singleById(long idPhoto) {
        return this.photoDao.singleById(idPhoto);
    }
}
