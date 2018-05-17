package oliweb.nc.oliweb.service.sync;

import android.content.Context;
import android.util.Log;

import io.reactivex.Observable;
import oliweb.nc.oliweb.database.converter.ChatConverter;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;

// TODO tester cette méthode
public class ChatFirebaseSender {

    private static final String TAG = ChatFirebaseSender.class.getName();

    private static ChatFirebaseSender instance;

    private FirebaseChatRepository firebaseChatRepository;
    private ChatRepository chatRepository;
    private MessageRepository messageRepository;

    private ChatFirebaseSender() {
    }

    public static ChatFirebaseSender getInstance(Context context) {
        if (instance == null) {
            instance = new ChatFirebaseSender();
            instance.chatRepository = ChatRepository.getInstance(context);
            instance.messageRepository = MessageRepository.getInstance(context);
            instance.firebaseChatRepository = FirebaseChatRepository.getInstance();
        }
        return instance;
    }

    public void sendNewChat(ChatEntity chatEntity) {
        Log.d(TAG, "sendNewChat chatEntity : " + chatEntity);
        if (chatEntity.getUidChat() == null) {
            firebaseChatRepository.getUidAndTimestampFromFirebase(chatEntity)
                    .toObservable()
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .switchMap(this::markChatAsSending)
                    .switchMap(this::sendChatToFirebase)
                    .switchMap(this::markChatAsSend)
                    .switchMap(this::updateAllMessages)
                    .subscribe();
        }
    }

    private Observable<ChatEntity> markChatAsSending(ChatEntity chatEntity) {
        Log.d(TAG, "markChatAsSending chatEntity : " + chatEntity);
        chatEntity.setStatusRemote(StatusRemote.SENDING);
        return chatRepository.saveIfNotExist(chatEntity).toObservable();
    }

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

    private Observable<ChatEntity> markChatAsSend(ChatEntity chatEntity) {
        Log.d(TAG, "markChatAsSend chatEntity : " + chatEntity);
        chatEntity.setStatusRemote(StatusRemote.SEND);
        return chatRepository.saveIfNotExist(chatEntity).toObservable();
    }

    private Observable<MessageEntity> updateAllMessages(ChatEntity chatEntity) {
        Log.d(TAG, "Update all the messages to retreive the UID Chat : " + chatEntity);
        return messageRepository.getSingleByIdChat(chatEntity.getIdChat())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .flattenAsObservable(list -> list)
                .doOnNext(messageEntity -> {
                    messageEntity.setUidChat(chatEntity.getUidChat());
                    messageRepository.saveWithSingle(messageEntity)
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
        chatRepository.saveWithSingle(chatEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }
}
