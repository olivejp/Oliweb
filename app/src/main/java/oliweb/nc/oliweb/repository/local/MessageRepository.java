package oliweb.nc.oliweb.repository.local;

import androidx.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.MessageConverter;
import oliweb.nc.oliweb.database.dao.MessageDao;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.dto.firebase.MessageFirebase;

/**
 * Created by 2761oli on 19/04/2018.
 */
@Singleton
public class MessageRepository extends AbstractRepository<MessageEntity, Long> {
    private static final String TAG = MessageRepository.class.getName();
    private MessageDao messageDao;

    private ChatRepository chatRepository;

    @Inject
    public MessageRepository(Context context, ChatRepository chatRepository) {
        super(context);
        this.dao = this.db.getMessageDao();
        this.messageDao = (MessageDao) this.dao;
        this.chatRepository = chatRepository;
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
        return this.messageDao.findFlowableByStatusAndUidChatNotNull(status);
    }

    public Single<List<MessageEntity>> findSingleByStatusAndUidChatNotNull(List<String> status) {
        return this.messageDao.findSingleByStatusAndUidChatNotNull(status);
    }

    public void saveMessageIfNotExist(MessageFirebase messageFirebase) {
        Log.d(TAG, "Starting saveMessageIfNotExist messageFirebase : " + messageFirebase);
        messageDao.findSingleByUid(messageFirebase.getUidMessage())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(messageEntity -> Log.d(TAG, "Message trouvé, inutile de le recréer"))
                .doOnComplete(() -> saveNewMessageUpdateChat(messageFirebase))
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }

    private void saveNewMessageUpdateChat(MessageFirebase messageFirebase) {
        Log.d(TAG, "Message non trouvé, tentative d'insertion");
        chatRepository.findByUid(messageFirebase.getUidChat())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnComplete(() -> Log.e(TAG, "Le chat " + messageFirebase.getUidChat() + "n'existe pas."))
                .map(chatEntity -> MessageConverter.convertDtoToEntity(chatEntity.getIdChat(), messageFirebase))
                .flatMapSingle(this::singleSave)
                .flatMapMaybe(messageEntity -> chatRepository.findByUid(messageEntity.getUidChat()))
                .flatMapSingle(chatEntity1 -> {
                    if (messageFirebase.getTimestamp() >= chatEntity1.getUpdateTimestamp()) {
                        chatEntity1.setLastMessage(messageFirebase.getMessage());
                        chatEntity1.setUpdateTimestamp(messageFirebase.getTimestamp());
                        return chatRepository.singleSave(chatEntity1);
                    } else {
                        return Single.just(chatEntity1);
                    }
                })
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }


    /**
     * On enregistre en local le message avec l'UID et le Timestamp récupéré dans la 1ère étape
     * On passe également le statut à SENDING pour gar
     *
     * @param messageSaved
     */
    public Observable<MessageEntity> markMessageIsSending(final MessageEntity messageSaved) {
        Log.d(TAG, "Mark message as Sending message to mark : " + messageSaved);
        messageSaved.setStatusRemote(StatusRemote.SENDING);
        return singleUpdate(messageSaved)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> markMessageAsFailedToSend(messageSaved))
                .doOnComplete(() -> markMessageAsFailedToSend(messageSaved))
                .toObservable();
    }

    /**
     * Le message a bien été envoyé sur Firebase, je change son statut à SEND
     * dans la DB locale pour ne pas le renvoyer.
     *
     * @param messageEntity
     */
    public Observable<MessageEntity> markMessageHasBeenSend(final MessageEntity messageEntity) {
        Log.d(TAG, "Mark message as has been SEND messageEntity :" + messageEntity);
        messageEntity.setStatusRemote(StatusRemote.SEND);
        return singleUpdate(messageEntity).toObservable();
    }

    /**
     * Cas d'une erreur dans l'envoi, on passe le message en statut Failed To Send.
     *
     * @param messageFailedToSend
     */
    public void markMessageAsFailedToSend(final MessageEntity messageFailedToSend) {
        Log.d(TAG, "Mark message Failed To Send message : " + messageFailedToSend);
        messageFailedToSend.setStatusRemote(StatusRemote.FAILED_TO_SEND);
        singleUpdate(messageFailedToSend).subscribe();
    }
}
