package oliweb.nc.oliweb.service.firebase;

import android.util.Log;

import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.converter.ChatConverter;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;

/**
 * Cette classe décompose toutes les étapes nécessaires pour l'envoi d'un chat
 * sur Firebase.
 */
@Singleton
public class FirebaseChatService {

    private static final String TAG = FirebaseChatService.class.getName();

    private FirebaseChatRepository firebaseChatRepository;
    private FirebaseUserRepository firebaseUserRepository;
    private ChatRepository chatRepository;
    private MessageRepository messageRepository;
    private Scheduler scheduler;

    @Inject
    public FirebaseChatService(FirebaseChatRepository firebaseChatRepository,
                               ChatRepository chatRepository,
                               MessageRepository messageRepository,
                               FirebaseUserRepository firebaseUserRepository,
                               Scheduler scheduler) {
        this.firebaseChatRepository = firebaseChatRepository;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.firebaseUserRepository = firebaseUserRepository;
        this.scheduler = scheduler;
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
                    .subscribeOn(scheduler).observeOn(scheduler)
                    .toObservable()
                    .switchMap(chatRepository::markChatAsSending)
                    .switchMap(this::sendChatToFirebase)
                    .switchMap(chatRepository::markChatAsSend)
                    .switchMap(this::updateAllMessages)
                    .doOnError(throwable -> chatRepository.markChatAsFailedToSend(chatEntity))
                    .subscribe();
        }
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
                .map(chatFirebase -> chatEntity)
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
                .flattenAsObservable(list -> list)
                .doOnNext(messageEntity -> {
                    messageEntity.setUidChat(chatEntity.getUidChat());
                    messageRepository.singleSave(messageEntity)
                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                            .subscribe();
                });
    }

    /**
     * Va lire tous les chats pour l'uid user, puis pour tout ces chats va
     * récupérer tous les membres et pour tous ces membres va récupérer leur photo URL
     */
    public Single<HashMap<String, UserEntity>> getPhotoUrlsByUidUser(String uidUser) {
        HashMap<String, UserEntity> map = new HashMap<>();
        return Single.create(emitter -> firebaseChatRepository.getByUidUser(uidUser)
                .observeOn(scheduler).subscribeOn(scheduler)
                .flattenAsObservable(chatFirebases -> chatFirebases)
                .map(chatFirebase -> chatFirebase.getMembers().keySet())
                .flatMapIterable(uidsUserFromChats -> uidsUserFromChats)
                .flatMap(foreignUidUserFromChat -> firebaseUserRepository.getUtilisateurByUid(foreignUidUserFromChat).toObservable())
                .distinct()
                .map(utilisateurEntity -> {
                    map.put(utilisateurEntity.getUid(), utilisateurEntity);
                    return map;
                })
                .doOnComplete(() -> emitter.onSuccess(map))
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe()
        );
    }
}
