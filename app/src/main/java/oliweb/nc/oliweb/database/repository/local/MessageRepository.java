package oliweb.nc.oliweb.database.repository.local;

import android.content.Context;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.dao.MessageDao;
import oliweb.nc.oliweb.database.entity.MessageEntity;

/**
 * Created by 2761oli on 19/04/2018.
 */

public class MessageRepository extends AbstractRepository<MessageEntity, Long> {
    private static final String TAG = MessageRepository.class.getName();
    private static MessageRepository INSTANCE;
    private MessageDao messageDao;

    private MessageRepository(Context context) {
        super(context);
        this.dao = this.db.getMessageDao();
        this.messageDao = (MessageDao) this.dao;
    }

    public static synchronized MessageRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new MessageRepository(context);
        }
        return INSTANCE;
    }

    public Maybe<MessageEntity> findSingleById(String uidMessage) {
        return this.messageDao.findSingleById(uidMessage);
    }
}
