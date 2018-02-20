package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.dao.CategorieDao;
import oliweb.nc.oliweb.database.entity.CategorieEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */

public class CategorieRepository extends AbstractRepository<CategorieEntity> {
    private static CategorieRepository INSTANCE;
    private CategorieDao categorieDao;

    private CategorieRepository(Context context) {
        super(context);
        this.categorieDao = db.categorieDao();
        this.dao = categorieDao;
    }

    public static synchronized CategorieRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new CategorieRepository(context);
        }
        return INSTANCE;
    }

    public Maybe<List<CategorieEntity>> getListCategorie() {
        return this.categorieDao.getListCategorie();
    }

    public LiveData<CategorieEntity> findById(Long id) {
        return this.categorieDao.findById(id);
    }
}
