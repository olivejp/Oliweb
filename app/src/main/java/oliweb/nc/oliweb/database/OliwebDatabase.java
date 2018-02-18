package oliweb.nc.oliweb.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.support.annotation.NonNull;

import java.util.concurrent.Executors;

import oliweb.nc.oliweb.database.dao.AnnonceDao;
import oliweb.nc.oliweb.database.dao.AnnonceWithPhotosDao;
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

@Database(version = 7, entities = {UtilisateurEntity.class, CategorieEntity.class, AnnonceEntity.class, PhotoEntity.class})
public abstract class OliwebDatabase extends RoomDatabase {
    private static OliwebDatabase INSTANCE;

    public static synchronized OliwebDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = buildDatabase(context.getApplicationContext());
        }
        return INSTANCE;
    }

    private static OliwebDatabase buildDatabase(final Context context) {
        return Room.databaseBuilder(context,
                OliwebDatabase.class,
                "oliweb-database")
                .fallbackToDestructiveMigration()
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Executors.newSingleThreadScheduledExecutor().execute(() -> getInstance(context).categorieDao().insert(populateCategorie()));
                    }
                })
                .addMigrations(MIGRATION_1_6)
                .build();
    }

    public static CategorieEntity[] populateCategorie() {
        return new CategorieEntity[] {
                new CategorieEntity("Automobile", null),
                new CategorieEntity("Mobilier", null),
                new CategorieEntity("Jouet", null),
                new CategorieEntity("Fleur", null),
                new CategorieEntity("Sport", null)
        };
    }

    static final Migration MIGRATION_1_6 = new Migration(1, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("INSERT INTO categorie (name) VALUES('Automobile'), ('Mobilier'), ('Jouet'), ('Fleur'), ('Sport')");
        }
    };

    public abstract UtilisateurDao utilisateurDao();

    public abstract CategorieDao categorieDao();

    public abstract AnnonceDao AnnonceDao();

    public abstract PhotoDao PhotoDao();

    public abstract AnnonceWithPhotosDao AnnonceWithPhotosDao();
}
