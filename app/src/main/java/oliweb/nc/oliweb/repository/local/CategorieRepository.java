package oliweb.nc.oliweb.repository.local;

import android.content.Context;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.dao.CategorieDao;
import oliweb.nc.oliweb.database.entity.CategorieEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Singleton
public class CategorieRepository extends AbstractRepository<CategorieEntity, Long> {
    private CategorieDao categorieDao;

    @Inject
    public CategorieRepository(Context context) {
        super(context);
        this.categorieDao = db.getCategorieDao();
        this.dao = categorieDao;
    }

    public Single<List<CategorieEntity>> getListCategorie() {
        return this.categorieDao.getListCategorie();
    }


    public Single<List<String>> getListCategorieLibelle() {
        return this.categorieDao.getListCategorieLibelle();
    }
}
