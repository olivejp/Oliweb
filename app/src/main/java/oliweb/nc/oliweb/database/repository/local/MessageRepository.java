package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.MessageDao;
import oliweb.nc.oliweb.database.entity.MessageEntity;

/**
 * Created by 2761oli on 19/04/2018.
 */

public class MessageRepository extends AbstractRepository<MessageEntity, Long> {
    private static final String TAG = MessageRepository.class.getName();
    private static MessageRepository instance;
    private MessageDao messageDao;

    private MessageRepository(Context context) {
        super(context);
        this.dao = this.db.getMessageDao();
        this.messageDao = (MessageDao) this.dao;
    }

    public static synchronized MessageRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MessageRepository(context);
        }
        return instance;
    }

    public Maybe<MessageEntity> findSingleById(String uidMessage) {
        return this.messageDao.findSingleById(uidMessage);
    }

    public LiveData<List<MessageEntity>> findAllByIdChat(Long idChat) {
        return this.messageDao.findAllByIdChat(idChat);
    }

    public Observable<MessageEntity> getAllMessageByStatusByIdChat(Long idChat, List<String> status) {
        Log.d(TAG, "Starting getAllMessageByStatusByIdChat " + status);
        return Observable.create(emitter ->
                this.messageDao.getAllMessageByStatusByIdChat(idChat, status)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(messageEntities -> {
                            for (MessageEntity message : messageEntities) {
                                emitter.onNext(message);
                            }
                            emitter.onComplete();
                        })
                        .subscribe()
        );
    }

    public Observable<MessageEntity> getAllMessageByStatus(List<String> status) {
        Log.d(TAG, "Starting getAllMessageByStatus " + status);
        return Observable.create(emitter ->
                this.messageDao.getAllMessageByStatus(status)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(messageEntities -> {
                            for (MessageEntity message : messageEntities) {
                                emitter.onNext(message);
                            }
                            emitter.onComplete();
                        })
                        .subscribe()
        );
    }
}
