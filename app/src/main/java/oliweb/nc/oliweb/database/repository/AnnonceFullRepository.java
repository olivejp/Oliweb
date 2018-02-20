package oliweb.nc.oliweb.database.repository;

import android.content.Context;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AnnonceFullDao;
import oliweb.nc.oliweb.database.entity.AnnonceFull;

/**
 * Created by 2761oli on 20/02/2018.
 */

public class AnnonceFullRepository {
    private static AnnonceFullRepository INSTANCE;
    private AnnonceFullDao annonceFullDao;

    private AnnonceFullRepository(Context context) {
        OliwebDatabase db = OliwebDatabase.getInstance(context);
        this.annonceFullDao = db.AnnonceFullDao();
    }

    public static synchronized AnnonceFullRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnnonceFullRepository(context);
        }
        return INSTANCE;
    }

    public Single<AnnonceFull> findAnnoncesByIdAnnonce(long idAnnonce) {
        return this.annonceFullDao.findSingleByIdAnnonce(idAnnonce);
    }

    public Maybe<List<AnnonceFull>> getAllAnnoncesByStatus(String status) {
        return this.annonceFullDao.getAllAnnonceByStatus(status);
    }
}
