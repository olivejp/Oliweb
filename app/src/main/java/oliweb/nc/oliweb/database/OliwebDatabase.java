package oliweb.nc.oliweb.database;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
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
import oliweb.nc.oliweb.database.entity.UserEntity;

import static oliweb.nc.oliweb.utility.Constants.OLIWEB_DATABASE;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Database(version = 28, entities = {UserEntity.class, CategorieEntity.class, AnnonceEntity.class, PhotoEntity.class, ChatEntity.class, MessageEntity.class})
public abstract class OliwebDatabase extends RoomDatabase {

    private static OliwebDatabase instance;

    public static synchronized OliwebDatabase getInstance(Context context) {
        if (instance == null) {
            instance = buildDatabase(context);
        }
        return instance;
    }

    private static OliwebDatabase buildDatabase(final Context context) {
        return Room.databaseBuilder(context, OliwebDatabase.class, OLIWEB_DATABASE)
                .fallbackToDestructiveMigration()
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        Executors.newSingleThreadExecutor().execute(() -> {
                            getInstance(context).getCategorieDao().deleteAll();
                            getInstance(context).getCategorieDao().insert(listDefaultCategories());
                        });
                    }
                })
                .build();
    }

    private static CategorieEntity[] listDefaultCategories() {
        return new CategorieEntity[]{
                new CategorieEntity("Automobile", null),
                new CategorieEntity("Mobilier", null),
                new CategorieEntity("Enfant", null),
                new CategorieEntity("Sport", null),
                new CategorieEntity("Immobilier", null),
                new CategorieEntity("Technologie", null)
        };
    }

    public abstract UtilisateurDao getUtilisateurDao();

    public abstract CategorieDao getCategorieDao();

    public abstract AnnonceDao getAnnonceDao();

    public abstract PhotoDao getPhotoDao();

    public abstract AnnonceWithPhotosDao getAnnonceWithPhotosDao();

    public abstract AnnonceFullDao getAnnonceFullDao();

    public abstract ChatDao getChatDao();

    public abstract MessageDao getMessageDao();
}
