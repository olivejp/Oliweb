package oliweb.nc.oliweb.database.repository;

import android.content.Context;

import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AnnonceDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;

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
}
