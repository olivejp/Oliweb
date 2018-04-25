package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.List;

import oliweb.nc.oliweb.database.dao.MessageDao;
import oliweb.nc.oliweb.database.entity.MessageEntity;

/**
 * Created by 2761oli on 19/04/2018.
 */

public class MessageRepository extends AbstractRepository<MessageEntity> {
    private static MessageRepository INSTANCE;
    private MessageDao chatDao;

    private MessageRepository(Context context) {
        super(context);
        this.dao = this.db.getMessageDao();
        this.chatDao = (MessageDao) this.dao;
    }

    public static synchronized MessageRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new MessageRepository(context);
        }
        return INSTANCE;
    }

    public LiveData<List<MessageEntity>> findByUidChat(String uidChat) {
        return this.chatDao.findByUidChat(uidChat);
    }
}
