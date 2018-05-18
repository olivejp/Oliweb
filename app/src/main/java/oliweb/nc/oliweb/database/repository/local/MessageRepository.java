package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.MessageConverter;
import oliweb.nc.oliweb.database.dao.MessageDao;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;

/**
 * Created by 2761oli on 19/04/2018.
 */

public class MessageRepository extends AbstractRepository<MessageEntity, Long> {
    private static final String TAG = MessageRepository.class.getName();
    private static MessageRepository instance;
    private MessageDao messageDao;
    private ChatRepository chatRepository;

    private MessageRepository(Context context) {
        super(context);
        this.dao = this.db.getMessageDao();
        this.messageDao = (MessageDao) this.dao;
        this.chatRepository = ChatRepository.getInstance(context);
    }

    public static synchronized MessageRepository getInstance(Context context) {
        if (instance == null) {
            instance = new MessageRepository(context);
        }
        return instance;
    }

    public Maybe<MessageEntity> findSingleByUid(String uidMessage) {
        return this.messageDao.findSingleByUid(uidMessage);
    }

    public LiveData<List<MessageEntity>> findAllByIdChat(Long idChat) {
        return this.messageDao.findAllByIdChat(idChat);
    }

    public Single<List<MessageEntity>> getSingleByIdChat(Long idChat) {
        return this.messageDao.getSingleByIdChat(idChat);
    }

    public Flowable<List<MessageEntity>> findFlowableByStatusAndUidChatNotNull(List<String> status) {
        Log.d(TAG, "Starting findFlowableByStatusAndUidChatNotNull " + status);
        return this.messageDao.findFlowableByStatusAndUidChatNotNull(status);
    }

    public void saveMessageIfNotExist(MessageFirebase messageFirebase) {
        Log.d(TAG, "Starting saveMessageIfNotExist messageFirebase : " + messageFirebase);
        chatRepository.findByUid(messageFirebase.getUidChat())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(chatEntity ->
                        messageDao.findSingleByUid(messageFirebase.getUidMessage())
                                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                .doOnComplete(() -> {
                                            Log.d(TAG, "Message was not found with UID : " + messageFirebase.getUidMessage());
                                            MessageEntity messageEntity = MessageConverter.convertDtoToEntity(chatEntity.getIdChat(), messageFirebase);
                                            saveWithSingle(messageEntity)
                                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                                    .subscribe();
                                        }
                                )
                                .subscribe()
                )
                .subscribe();
    }
}
