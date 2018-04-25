package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AnnonceWithPhotosDao;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class AnnonceWithPhotosRepository {
    private static AnnonceWithPhotosRepository INSTANCE;
    private AnnonceWithPhotosDao annonceWithPhotosDao;

    private AnnonceWithPhotosRepository(Context context) {
        OliwebDatabase db = OliwebDatabase.getInstance(context);
        this.annonceWithPhotosDao = db.getAnnonceWithPhotosDao();
    }

    public static synchronized AnnonceWithPhotosRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnnonceWithPhotosRepository(context);
        }
        return INSTANCE;
    }

    public LiveData<List<AnnoncePhotos>> findActiveAnnonceByUidUser(String uuidUtilisateur) {
        return this.annonceWithPhotosDao.findActiveAnnonceByUidUser(uuidUtilisateur);
    }

    public LiveData<List<AnnoncePhotos>> findFavoritesByUidUser(String uuidUtilisateur) {
        return this.annonceWithPhotosDao.findFavoritesByUidUser(uuidUtilisateur);
    }

    public LiveData<AnnoncePhotos> findAnnonceByUidAnnonce(String uidAnnonce) {
        return this.annonceWithPhotosDao.findAnnonceByUidAnnonce(uidAnnonce);
    }
}
