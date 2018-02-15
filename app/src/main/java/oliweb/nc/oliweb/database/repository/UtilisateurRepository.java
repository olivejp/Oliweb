package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.UtilisateurDao;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class UtilisateurRepository extends AbstractRepository<UtilisateurEntity> {
    private static UtilisateurRepository INSTANCE;
    private UtilisateurDao utilisateurDao;

    private UtilisateurRepository(Context context) {
        OliwebDatabase db = OliwebDatabase.getInstance(context);
        this.utilisateurDao = db.utilisateurDao();
        setDao(utilisateurDao);
    }

    public static synchronized UtilisateurRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new UtilisateurRepository(context);
        }
        return INSTANCE;
    }

    public LiveData<UtilisateurEntity> findById(String UuidUtilisateur){
        return this.utilisateurDao.findByUuid(UuidUtilisateur);
    }

    public void save(UtilisateurEntity utilisateurEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        this.utilisateurDao.findSingleByUuid(utilisateurEntity.getUuidUtilisateur())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe((utilisateurEntity1, throwable) -> {
                    if (throwable != null) {
                        // This user don't exists already, create it
                        insert(onRespositoryPostExecute, utilisateurEntity);
                    } else {
                        if (utilisateurEntity1 != null) {
                            // User exists, just update it
                            update(onRespositoryPostExecute, utilisateurEntity);
                        }
                    }
                });
    }
}
