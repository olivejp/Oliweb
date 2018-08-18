package oliweb.nc.oliweb.service.firebase;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.ChatConverter;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;

/**
 * Cette classe décompose toutes les étapes nécessaires pour l'envoi d'un chat
 * sur Firebase.
 */
@Singleton
public class ChatFirebaseSender {

    private static final String TAG = ChatFirebaseSender.class.getName();

    private FirebaseChatRepository firebaseChatRepository;
    private ChatRepository chatRepository;
    private MessageRepository messageRepository;

    @Inject
    public ChatFirebaseSender(FirebaseChatRepository firebaseChatRepository, ChatRepository chatRepository, MessageRepository messageRepository) {
        this.firebaseChatRepository = firebaseChatRepository;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Main method
     *
     * @param chatEntity
     */
    public void sendNewChat(ChatEntity chatEntity) {
        Log.d(TAG, "sendNewChat chatEntity : " + chatEntity);
        if (chatEntity.getUidChat() == null) {
            firebaseChatRepository.getUidAndTimestampFromFirebase(chatEntity)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .toObservable()
                    .switchMap(this::markChatAsSending)
                    .switchMap(this::sendChatToFirebase)
                    .switchMap(this::markChatAsSend)
                    .switchMap(this::updateAllMessages)
                    .subscribe();
        }
    }

    /**
     * 1 - Marque le chat comme étant en train d'être envoyé
     *
     * @param chatEntity
     * @return
     */
    private Observable<ChatEntity> markChatAsSending(ChatEntity chatEntity) {
        Log.d(TAG, "markChatAsSending chatEntity : " + chatEntity);
        chatEntity.setStatusRemote(StatusRemote.SENDING);
        return chatRepository.singleSave(chatEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    /**
     * 2 - Tente d'envoyer le chat sur firebase
     *
     * @param chatEntity
     * @return
     */
    private Observable<ChatEntity> sendChatToFirebase(ChatEntity chatEntity) {
        Log.d(TAG, "sendChatToFirebase chatEntity : " + chatEntity);
        return firebaseChatRepository
                .saveChat(ChatConverter.convertEntityToDto(chatEntity))
                .doOnError(e -> {
                    Log.e(TAG, e.getLocalizedMessage(), e);
                    markChatAsFailedToSend(chatEntity);
                })
                .map(chatFirebase -> chatEntity)
                .toObservable();
    }

    /**
     * 3 - Marque le chat comme étant envoyé
     *
     * @param chatEntity
     * @return
     */
    private Observable<ChatEntity> markChatAsSend(ChatEntity chatEntity) {
        Log.d(TAG, "markChatAsSend chatEntity : " + chatEntity);
        chatEntity.setStatusRemote(StatusRemote.SEND);
        return chatRepository.singleSave(chatEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    /**
     * 4 - Met à jour tous les messages attachés à ce chat pour leur attribuer l'UID du chat
     *
     * @param chatEntity
     * @return
     */
    private Observable<MessageEntity> updateAllMessages(ChatEntity chatEntity) {
        Log.d(TAG, "Update all the messages to retreive the UID Chat : " + chatEntity);
        return messageRepository.getSingleByIdChat(chatEntity.getIdChat())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .flattenAsObservable(list -> list)
                .doOnNext(messageEntity -> {
                    messageEntity.setUidChat(chatEntity.getUidChat());
                    messageRepository.singleSave(messageEntity)
                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                            .subscribe();
                });
    }

    /**
     * Cas d'une erreur dans l'envoi, on passe le chat en statut Failed To Send.
     *
     * @param chatEntity
     */
    private void markChatAsFailedToSend(final ChatEntity chatEntity) {
        Log.d(TAG, "Mark chat Failed To Send chatEntity : " + chatEntity);
        chatEntity.setStatusRemote(StatusRemote.FAILED_TO_SEND);
        chatRepository.singleSave(chatEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }
}
