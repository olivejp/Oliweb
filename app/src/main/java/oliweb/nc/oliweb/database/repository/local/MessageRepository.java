package oliweb.nc.oliweb.database.repository.local;

import android.content.Context;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.MessageDao;
import oliweb.nc.oliweb.database.entity.MessageEntity;

/**
 * Created by 2761oli on 19/04/2018.
 */

public class MessageRepository extends AbstractRepository<MessageEntity> {
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

    private Single<MessageEntity> insertSingle(MessageEntity messageEntity) {
        return Single.create(e -> {
            try {
                Long[] ids = dao.insert(messageEntity);
                if (ids.length == 1) {
                    findSingleById(messageEntity.getUidMessage())
                            .doOnSuccess(e::onSuccess)
                            .subscribe();
                } else {
                    e.onError(new RuntimeException("Failed to insert into MessageRepository"));
                }
            } catch (Exception exception) {
                Log.e(TAG, exception.getMessage());
                e.onError(exception);
            }
        });
    }

    /**
     * You have to subscribe to this Single on a background thread
     * because it queries the Database which only accept background queries.
     */
    private Single<MessageEntity> updateSingle(MessageEntity messageEntity) {
        return Single.create(e -> {
            try {
                int updatedCount = dao.update(messageEntity);
                if (updatedCount == 1) {
                    findSingleById(messageEntity.getUidMessage())
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(e::onSuccess)
                            .subscribe();
                } else {
                    e.onError(new RuntimeException("Failed to update into MessageRepository"));
                }
            } catch (Exception exception) {
                Log.e(TAG, exception.getMessage());
                e.onError(exception);
            }
        });
    }

    private Single<AtomicBoolean> existById(String uidMessage) {
        return Single.create(e -> messageDao.countById(uidMessage)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }

    public Single<MessageEntity> saveWithSingle(MessageEntity messageEntity) {
        return Single.create(emitter -> existById(messageEntity.getUidMessage())
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(emitter::onError)
                .doOnSuccess(atomicBoolean -> {
                    if (atomicBoolean.get()) {
                        updateSingle(messageEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    } else {
                        insertSingle(messageEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    }
                })
                .subscribe());
    }
}
