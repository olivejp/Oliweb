package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.ChatDao;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class ChatRepository extends AbstractRepository<ChatEntity> {
    private static ChatRepository INSTANCE;
    private ChatDao chatDao;

    private ChatRepository(Context context) {
        super(context);
        this.dao = this.db.getChatDao();
        this.chatDao = (ChatDao) this.dao;
    }

    public static synchronized ChatRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ChatRepository(context);
        }
        return INSTANCE;
    }

    public void save(ChatEntity chatEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        this.chatDao.findSingleById(chatEntity.getUidChat())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe((annonceEntity1, throwable) -> {
                    if (throwable != null) {
                        // This annonce don't exists already, create it
                        insert(onRespositoryPostExecute, chatEntity);
                    } else {
                        if (annonceEntity1 != null) {
                            // Annonce exists, just update it
                            update(onRespositoryPostExecute, chatEntity);
                        }
                    }
                });
    }

    public LiveData<ChatEntity> findById(String uidChat) {
        return this.chatDao.findById(uidChat);
    }

    public LiveData<List<ChatEntity>> findByUidSeller(String uidSeller) {
        return this.chatDao.findByUidSeller(uidSeller);
    }
}
