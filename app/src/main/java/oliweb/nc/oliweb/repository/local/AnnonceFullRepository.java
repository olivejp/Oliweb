package oliweb.nc.oliweb.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AnnonceFullDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;

/**
 * Created by 2761oli on 20/02/2018.
 */
@Singleton
public class AnnonceFullRepository {
    private AnnonceFullDao annonceFullDao;

    @Inject
    public AnnonceFullRepository(Context context) {
        OliwebDatabase db = OliwebDatabase.getInstance(context);
        this.annonceFullDao = db.getAnnonceFullDao();
    }

    public Single<AnnonceFull> findAnnoncesByIdAnnonce(long idAnnonce) {
        return this.annonceFullDao.findSingleByIdAnnonce(idAnnonce);
    }

    public Observable<AnnonceFull> findAnnonceFullByAnnonceEntity(AnnonceEntity annonceEntity) {
        return this.annonceFullDao.findSingleByIdAnnonce(annonceEntity.getIdAnnonce()).toObservable();
    }

    public LiveData<List<AnnonceFull>> findFavoritesByUidUser(String uuidUtilisateur) {
        return this.annonceFullDao.findFavoritesByUidUser(uuidUtilisateur);
    }


    public LiveData<Integer> findFavoritesByUidUserAndByUidAnnonce(String uuidUtilisateur, String uidAnnonce) {
        return this.annonceFullDao.findFavoritesByUidUserAndByUidAnnonce(uuidUtilisateur, uidAnnonce);
    }


}
