package oliweb.nc.oliweb.database.repository.local;

import android.content.Context;

import java.util.List;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.dao.CategorieDao;
import oliweb.nc.oliweb.database.entity.CategorieEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */

public class CategorieRepository extends AbstractRepository<CategorieEntity, Long> {
    private static final String TAG = CategorieRepository.class.getName();
    private static CategorieRepository instance;
    private CategorieDao categorieDao;

    private CategorieRepository(Context context) {
        super(context);
        this.categorieDao = db.getCategorieDao();
        this.dao = categorieDao;
    }

    public static synchronized CategorieRepository getInstance(Context context) {
        if (instance == null) {
            instance = new CategorieRepository(context);
        }
        return instance;
    }

    public Single<List<CategorieEntity>> getListCategorie() {
        return this.categorieDao.getListCategorie();
    }

    public Single<CategorieEntity> findSingleById(long idCategorie) {
        return this.categorieDao.findSingleById(idCategorie);
    }
}
