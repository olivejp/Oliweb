package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
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
}
