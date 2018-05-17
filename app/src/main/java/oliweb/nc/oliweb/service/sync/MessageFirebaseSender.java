package oliweb.nc.oliweb.service.sync;

import android.content.Context;
import android.util.Log;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.MessageConverter;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseMessageRepository;

/**
 * Cette classe découpe toutes les étapes nécessaires pour l'envoi d'un message sur Firebase
 */
public class MessageFirebaseSender {

    private static final String TAG = MessageFirebaseSender.class.getName();

    private static MessageFirebaseSender instance;

    private FirebaseMessageRepository firebaseMessageRepository;
    private MessageRepository messageRepository;

    private MessageFirebaseSender() {
    }

    public static MessageFirebaseSender getInstance(Context context) {
        if (instance == null) {
            instance = new MessageFirebaseSender();
            instance.messageRepository = MessageRepository.getInstance(context);
            instance.firebaseMessageRepository = FirebaseMessageRepository.getInstance();
        }
        return instance;
    }

    /**
     * 1ere étape : Récupération dans Firebase d'un UID et du Timestamp de création
     *
     * @param messageEntity
     */
    public void sendMessage(final MessageEntity messageEntity) {
        Log.d(TAG, "SendMessage messageEntity : " + messageEntity);
        if (messageEntity.getUidMessage() == null) {
            // Je n'ai pas encore de UID Message, je vais en chercher un
            firebaseMessageRepository.getUidAndTimestampFromFirebase(messageEntity.getUidChat(), messageEntity)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> {
                        markMessageAsFailedToSend(messageEntity);
                        Log.e(TAG, e.getLocalizedMessage(), e);
                    })
                    .doOnSuccess(this::markMessageIsSending)
                    .subscribe();
        } else {
            // J'ai déjà un UID Message, je vais directement à l'étape 2
            markMessageIsSending(messageEntity);
        }
    }

    /**
     * 2nd étape : On enregistre en local le message avec l'UID et le Timestamp récupéré dans la 1ère étape
     * On passe également le statut à SENDING pour gar
     *
     * @param messageSaved
     */
    private void markMessageIsSending(final MessageEntity messageSaved) {
        Log.d(TAG, "Mark message as Sending message to mark : " + messageSaved);
        messageSaved.setStatusRemote(StatusRemote.SENDING);
        messageRepository.saveWithSingle(messageSaved)
                .doOnError(e -> {
                    markMessageAsFailedToSend(messageSaved);
                    Log.e(TAG, e.getLocalizedMessage(), e);
                })
                .doOnSuccess(this::sendMessageToFirebase)
                .subscribe();
    }

    /**
     * 3eme étape : On tente d'envoyer le message sur Firebase
     *
     * @param messageRead
     */
    private void sendMessageToFirebase(final MessageEntity messageRead) {
        Log.d(TAG, "Send message to Firebase message to send : " + messageRead);
        firebaseMessageRepository.saveMessage(MessageConverter.convertEntityToDto(messageRead))
                .doOnError(e -> {
                    markMessageAsFailedToSend(messageRead);
                    Log.e(TAG, e.getLocalizedMessage(), e);
                })
                .doOnSuccess(messageFirebase -> markMessageHasBeenSend(messageRead))
                .subscribe();
    }

    /**
     * 4eme étape : Le message a bien été envoyé sur Firebase, je change son statut à SEND
     * dans la DB locale pour ne pas le renvoyer.
     *
     * @param messageEntity
     */
    private void markMessageHasBeenSend(final MessageEntity messageEntity) {
        Log.d(TAG, "Mark message as has been SEND messageEntity :" + messageEntity);
        messageEntity.setStatusRemote(StatusRemote.SEND);
        messageRepository.saveWithSingle(messageEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(messageEntity1 -> Log.d(TAG, "Message has been marked as SEND messageEntity : " + messageEntity1))
                .subscribe();
    }

    /**
     * Cas d'une erreur dans l'envoi, on passe le message en statut Failed To Send.
     *
     * @param messageFailedToSend
     */
    private void markMessageAsFailedToSend(final MessageEntity messageFailedToSend) {
        Log.d(TAG, "Mark message Failed To Send message : " + messageFailedToSend);
        messageFailedToSend.setStatusRemote(StatusRemote.FAILED_TO_SEND);
        messageRepository.saveWithSingle(messageFailedToSend)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }
}
