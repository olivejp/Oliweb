package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import java.util.List;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.CategorieDao;
import oliweb.nc.oliweb.database.entity.CategorieEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */

public class CategorieRepository {
    private static CategorieRepository INSTANCE;
    private CategorieDao categorieDao;

    private CategorieRepository(Context contex) {
        OliwebDatabase db = OliwebDatabase.getInstance(contex);
        this.categorieDao = db.categorieDao();
    }

    public static synchronized CategorieRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new CategorieRepository(context);
        }
        return INSTANCE;
    }

    public void insert(@Nullable AbstractRepositoryTask.OnRespositoryPostExecute onRespositoryPostExecute, CategorieEntity... categorieEntities) {
        AbstractRepositoryTask<CategorieEntity> repositoryTask = new AbstractRepositoryTask<>(this.categorieDao, TypeTask.INSERT, onRespositoryPostExecute);
        repositoryTask.execute(categorieEntities);
    }

    public void update(@Nullable AbstractRepositoryTask.OnRespositoryPostExecute onRespositoryPostExecute, CategorieEntity... categorieEntities) {
        AbstractRepositoryTask<CategorieEntity> repositoryTask = new AbstractRepositoryTask<>(this.categorieDao, TypeTask.UPDATE, onRespositoryPostExecute);
        repositoryTask.execute(categorieEntities);
    }

    public void delete(@Nullable AbstractRepositoryTask.OnRespositoryPostExecute onRespositoryPostExecute, CategorieEntity... categorieEntities) {
        AbstractRepositoryTask<CategorieEntity> repositoryTask = new AbstractRepositoryTask<>(this.categorieDao, TypeTask.DELETE, onRespositoryPostExecute);
        repositoryTask.execute(categorieEntities);
    }

    public Maybe<List<CategorieEntity>> getListCategorie(){
        return this.categorieDao.getListCategorie();
    }

    public LiveData<CategorieEntity> findCategorieById(Long id){
        return this.categorieDao.findById(id);
    }
}
