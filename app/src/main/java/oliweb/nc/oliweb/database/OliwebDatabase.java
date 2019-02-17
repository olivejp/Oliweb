package oliweb.nc.oliweb.database;

import android.content.Context;

import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
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
                new CategorieEntity(1L,"Mobilier", null),
                new CategorieEntity(2L,"Immobilier", null),
                new CategorieEntity(3L,"Enfant", null),
                new CategorieEntity(4L,"Automobile", null),
                new CategorieEntity(5L,"Sport", null),
                new CategorieEntity(6L,"Technologie", null),
                new CategorieEntity(7L,"Vêtement", null),
                new CategorieEntity(8L,"Jardin", null),
                new CategorieEntity(9L,"Animal", null),
                new CategorieEntity(10L,"Musique", null)
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
