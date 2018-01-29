package oliweb.nc.oliweb.database.repository;

import android.content.Context;

import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.PhotoDao;
import oliweb.nc.oliweb.database.entity.PhotoEntity;

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
}
