package oliweb.nc.oliweb.service.firebase;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.MessageConverter;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.dto.firebase.ChatFirebase;
import oliweb.nc.oliweb.dto.firebase.MessageFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;

/**
 * Cette classe découpe toutes les étapes nécessaires pour l'envoi d'un message sur Firebase
 */
@Singleton
public class MessageFirebaseSender {

    private static final String TAG = MessageFirebaseSender.class.getName();

    private FirebaseMessageRepository firebaseMessageRepository;
    private FirebaseChatRepository firebaseChatRepository;
    private MessageRepository messageRepository;

    @Inject
    public MessageFirebaseSender(FirebaseMessageRepository firebaseMessageRepository, FirebaseChatRepository firebaseChatRepository, MessageRepository messageRepository) {
        this.firebaseChatRepository = firebaseChatRepository;
        this.firebaseMessageRepository = firebaseMessageRepository;
        this.messageRepository = messageRepository;
    }


    /**
     * Envoi d'un message sur Firebase Database
     *
     * @param messageEntity
     */
    public Observable<ChatFirebase> sendMessage(final MessageEntity messageEntity) {
        Log.d(TAG, "SendMessage messageEntity : " + messageEntity);
        if (messageEntity.getUidMessage() == null) {
            // Je n'ai pas encore de UID Message, je vais en chercher un
            return firebaseMessageRepository.getUidAndTimestampFromFirebase(messageEntity.getUidChat(), messageEntity)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> markMessageAsFailedToSend(messageEntity))
                    .toObservable()
                    .switchMap(this::markMessageIsSending)
                    .switchMap(this::sendMessageToFirebase)
                    .switchMap(firebaseChatRepository::updateLastMessageChat);
        } else {
            // J'ai déjà un UID Message, je vais directement à l'étape 2
            return markMessageIsSending(messageEntity)
                    .doOnError(e -> markMessageAsFailedToSend(messageEntity))
                    .switchMap(this::sendMessageToFirebase)
                    .switchMap(firebaseChatRepository::updateLastMessageChat);
        }
    }

    /**
     * 3eme étape : On tente d'envoyer le message sur Firebase
     *
     * @param messageRead
     */
    private Observable<MessageFirebase> sendMessageToFirebase(final MessageEntity messageRead) {
        Log.d(TAG, "Send message to Firebase message to send : " + messageRead);
        return firebaseMessageRepository.saveMessage(MessageConverter.convertEntityToDto(messageRead))
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> markMessageAsFailedToSend(messageRead))
                .doOnSuccess(messageFirebase -> markMessageHasBeenSend(messageRead))
                .toObservable();
    }

    /**
     * On enregistre en local le message avec l'UID et le Timestamp récupéré dans la 1ère étape
     * On passe également le statut à SENDING pour gar
     *
     * @param messageSaved
     */
    private Observable<MessageEntity> markMessageIsSending(final MessageEntity messageSaved) {
        Log.d(TAG, "Mark message as Sending message to mark : " + messageSaved);
        messageSaved.setStatusRemote(StatusRemote.SENDING);
        return messageRepository.singleUpdate(messageSaved)
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
    private Observable<MessageEntity> markMessageHasBeenSend(final MessageEntity messageEntity) {
        Log.d(TAG, "Mark message as has been SEND messageEntity :" + messageEntity);
        messageEntity.setStatusRemote(StatusRemote.SEND);
        return messageRepository.singleUpdate(messageEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    /**
     * Cas d'une erreur dans l'envoi, on passe le message en statut Failed To Send.
     *
     * @param messageFailedToSend
     */
    private void markMessageAsFailedToSend(final MessageEntity messageFailedToSend) {
        Log.d(TAG, "Mark message Failed To Send message : " + messageFailedToSend);
        messageFailedToSend.setStatusRemote(StatusRemote.FAILED_TO_SEND);
        messageRepository.singleUpdate(messageFailedToSend)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }
}
