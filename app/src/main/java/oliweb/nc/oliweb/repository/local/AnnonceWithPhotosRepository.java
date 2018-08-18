package oliweb.nc.oliweb.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AnnonceWithPhotosDao;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;

/**
 * Created by 2761oli on 29/01/2018.
 */
@Singleton
public class AnnonceWithPhotosRepository {

    private AnnonceWithPhotosDao annonceWithPhotosDao;

    @Inject
    public AnnonceWithPhotosRepository(Context context) {
        OliwebDatabase db = OliwebDatabase.getInstance(context);
        this.annonceWithPhotosDao = db.getAnnonceWithPhotosDao();
    }

    public LiveData<List<AnnoncePhotos>> findActiveAnnonceByUidUser(String uuidUtilisateur) {
        return this.annonceWithPhotosDao.findActiveAnnonceByUidUser(uuidUtilisateur);
    }

    public LiveData<List<AnnoncePhotos>> findFavoritesByUidUser(String uuidUtilisateur) {
        return this.annonceWithPhotosDao.findFavoritesByUidUser(uuidUtilisateur);
    }

    public Maybe<AnnoncePhotos> findFavoriteAnnonceByUidAnnonce(String uidUser, String uidAnnonce) {
        return this.annonceWithPhotosDao.findFavoriteAnnonceByUidAnnonce(uidUser, uidAnnonce);
    }
}
