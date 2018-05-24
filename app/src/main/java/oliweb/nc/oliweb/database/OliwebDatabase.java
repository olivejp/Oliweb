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
import oliweb.nc.oliweb.database.dao.AnnonceFullDao;
import oliweb.nc.oliweb.database.dao.AnnonceWithPhotosDao;
import oliweb.nc.oliweb.database.dao.CategorieDao;
import oliweb.nc.oliweb.database.dao.ChatDao;
import oliweb.nc.oliweb.database.dao.MessageDao;
import oliweb.nc.oliweb.database.dao.PhotoDao;
import oliweb.nc.oliweb.database.dao.UtilisateurDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Database(version = 23, entities = {UtilisateurEntity.class, CategorieEntity.class, AnnonceEntity.class, PhotoEntity.class, ChatEntity.class, MessageEntity.class})
public abstract class OliwebDatabase extends RoomDatabase {
    private static OliwebDatabase instance;

    public static synchronized OliwebDatabase getInstance(Context context) {
        if (instance == null) {
            instance = buildDatabase(context.getApplicationContext());
        }
        return instance;
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
                        Executors.newSingleThreadScheduledExecutor().execute(() -> getInstance(context).getCategorieDao().insert(populateCategorie()));
                    }

                    @Override
                    public void onOpen(@NonNull SupportSQLiteDatabase db) {
                        super.onOpen(db);

                        // Vérification qu'on a bien la liste des Catégories avant de démarrer l'application
                        Executors.newSingleThreadExecutor().execute(() ->
                                getInstance(context)
                                        .getCategorieDao()
                                        .getListCategorie()
                                        .subscribe(list -> {
                                            if (list.isEmpty()) {
                                                db.execSQL("INSERT INTO categorie (name) VALUES('Automobile'), ('Mobilier'), ('Jouet'), ('Fleur'), ('Sport')");
                                            }
                                        })
                        );
                    }
                })
                .addMigrations(MIGRATION_1_11)
                .build();
    }

    private static CategorieEntity[] populateCategorie() {
        return new CategorieEntity[]{
                new CategorieEntity("Automobile", null),
                new CategorieEntity("Mobilier", null),
                new CategorieEntity("Jouet", null),
                new CategorieEntity("Fleur", null),
                new CategorieEntity("Sport", null)
        };
    }

    private static final Migration MIGRATION_1_11 = new Migration(1, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("INSERT INTO categorie (name) VALUES('Automobile'), ('Mobilier'), ('Jouet'), ('Fleur'), ('Sport')");
        }
    };

    public abstract UtilisateurDao getUtilisateurDao();

    public abstract CategorieDao getCategorieDao();

    public abstract AnnonceDao getAnnonceDao();

    public abstract PhotoDao getPhotoDao();

    public abstract AnnonceWithPhotosDao getAnnonceWithPhotosDao();

    public abstract AnnonceFullDao getAnnonceFullDao();

    public abstract ChatDao getChatDao();

    public abstract MessageDao getMessageDao();
}
