package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
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
        super(context);
        this.dao = this.db.getAnnonceDao();
        this.annonceDao = (AnnonceDao) this.dao;
    }

    public static synchronized AnnonceRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnnonceRepository(context);
        }
        return INSTANCE;
    }

    public void save(AnnonceEntity annonceEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        this.annonceDao.findSingleById(annonceEntity.getIdAnnonce())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe((annonceEntity1, throwable) -> {
                    if (throwable != null) {
                        // This annonce don't exists already, create it
                        insert(onRespositoryPostExecute, annonceEntity);
                    } else {
                        if (annonceEntity1 != null) {
                            // Annonce exists, just update it
                            update(onRespositoryPostExecute, annonceEntity);
                        }
                    }
                });
    }

    public LiveData<AnnonceEntity> findById(long idAnnonce) {
        return this.annonceDao.findById(idAnnonce);
    }

    public Single<AnnonceEntity> findSingleById(long idAnnonce) {
        return this.annonceDao.findSingleById(idAnnonce);
    }

    public Maybe<List<AnnonceEntity>> getAllAnnonceByStatus(String status){
        return this.annonceDao.getAllAnnonceByStatus(status);
    }

    public Single<Integer> countAllAnnoncesByStatus(String status){
        return this.annonceDao.countAllAnnoncesByStatus(status);
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uidUser){
        return this.annonceDao.countAllAnnoncesByUser(uidUser);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uidUser){
        return this.annonceDao.countAllFavoritesByUser(uidUser);
    }

    public Single<Integer> existByUidUtilisateurAndUidAnnonce(String uidUtilisateur, String uidAnnonce){
        return this.annonceDao.existByUidUtilisateurAndUidAnnonce(uidUtilisateur, uidAnnonce);
    }

    public LiveData<Integer> getAllAnnonceByStatusAndByUidUser(String status, String uidUser){
        return this.annonceDao.getAllAnnonceByStatusAndByUidUser(status, uidUser);
    }

    public Single<Integer> isAnnonceFavorite(String uidAnnonce){
        return this.annonceDao.isAnnonceFavorite(uidAnnonce);
    }
}
