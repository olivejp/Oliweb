package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AnnonceWithChatsDao;
import oliweb.nc.oliweb.database.entity.AnnonceWithChats;

/**
 * Created by 2761oli on 12/08/2018.
 */
public class AnnonceWithChatRepository {
    private static AnnonceWithChatRepository instance;
    private AnnonceWithChatsDao annonceWithChatsDao;

    private AnnonceWithChatRepository(Context context) {
        OliwebDatabase db = OliwebDatabase.getInstance(context);
        this.annonceWithChatsDao = db.getAnnonceWithChatsDao();
    }

    public static synchronized AnnonceWithChatRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AnnonceWithChatRepository(context);
        }
        return instance;
    }

    public LiveData<List<AnnonceWithChats>> findByUidUser(String uidUser) {
        return this.annonceWithChatsDao.findByUidUser(uidUser);
    }
}
