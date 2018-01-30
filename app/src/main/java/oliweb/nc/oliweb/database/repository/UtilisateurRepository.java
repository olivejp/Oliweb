package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.UtilisateurDao;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;

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

    public LiveData<UtilisateurEntity> findById(Long idUtilisateur){
        return this.utilisateurDao.findById(idUtilisateur);
    }
}
