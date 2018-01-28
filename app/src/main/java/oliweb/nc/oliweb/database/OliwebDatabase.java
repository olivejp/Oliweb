package oliweb.nc.oliweb.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import oliweb.nc.oliweb.database.dao.AnnonceDao;
import oliweb.nc.oliweb.database.dao.CategorieDao;
import oliweb.nc.oliweb.database.dao.PhotoDao;
import oliweb.nc.oliweb.database.dao.UtilisateurDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Database(version = 1, entities = {UtilisateurEntity.class, CategorieEntity.class, AnnonceEntity.class, PhotoEntity.class})
public abstract class OliwebDatabase extends RoomDatabase {
    private static OliwebDatabase INSTANCE;

    public static synchronized OliwebDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(), OliwebDatabase.class, "oliweb-database").fallbackToDestructiveMigration().build();
        }
        return INSTANCE;
    }

    public abstract UtilisateurDao utilisateurDao();

    public abstract CategorieDao categorieDao();

    public abstract AnnonceDao AnnonceDao();

    public abstract PhotoDao PhotoDao();
}
