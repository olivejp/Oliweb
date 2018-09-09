package oliweb.nc.oliweb.service.firebase;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import oliweb.nc.oliweb.database.converter.MessageConverter;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.dto.firebase.ChatFirebase;
import oliweb.nc.oliweb.dto.firebase.MessageFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;

/**
 * Cette classe découpe toutes les étapes nécessaires pour l'envoi d'un message sur Firebase
 */
@Singleton
public class FirebaseMessageService {

    private static final String TAG = FirebaseMessageService.class.getName();

    private FirebaseMessageRepository firebaseMessageRepository;
    private FirebaseChatRepository firebaseChatRepository;
    private MessageRepository messageRepository;
    private Scheduler scheduler;

    @Inject
    public FirebaseMessageService(FirebaseMessageRepository firebaseMessageRepository,
                                  FirebaseChatRepository firebaseChatRepository,
                                  MessageRepository messageRepository,
                                  Scheduler scheduler) {
        this.firebaseChatRepository = firebaseChatRepository;
        this.firebaseMessageRepository = firebaseMessageRepository;
        this.messageRepository = messageRepository;
        this.scheduler = scheduler;
    }

    public Observable<ChatFirebase> sendMessage(final MessageEntity messageEntity) {
        Log.d(TAG, "SendMessage messageEntity : " + messageEntity);
        if (messageEntity.getUidMessage() == null) {
            // Je n'ai pas encore de UID Message, je vais en chercher un
            return firebaseMessageRepository.getUidAndTimestampFromFirebase(messageEntity.getUidChat(), messageEntity)
                    .subscribeOn(scheduler).observeOn(scheduler)
                    .toObservable()
                    .switchMap(messageEntity1 -> messageRepository.markMessageIsSending(messageEntity1))
                    .switchMap(this::sendMessageToFirebase)
                    .switchMap(firebaseChatRepository::updateLastMessageChat)
                    .doOnError(e -> messageRepository.markMessageAsFailedToSend(messageEntity));
        } else {
            // J'ai déjà un UID Message, je vais directement à l'étape 2
            return messageRepository.markMessageIsSending(messageEntity)
                    .switchMap(this::sendMessageToFirebase)
                    .switchMap(firebaseChatRepository::updateLastMessageChat)
                    .doOnError(e -> messageRepository.markMessageAsFailedToSend(messageEntity));
        }
    }

    private Observable<MessageFirebase> sendMessageToFirebase(final MessageEntity messageRead) {
        Log.d(TAG, "Send message to Firebase message to send : " + messageRead);
        return firebaseMessageRepository.saveMessage(MessageConverter.convertEntityToDto(messageRead))
                .subscribeOn(scheduler).observeOn(scheduler)
                .doOnSuccess(messageFirebase -> messageRepository.markMessageHasBeenSend(messageRead))
                .toObservable();
    }

    public LiveData<Long> getCountMessageByUidUser(String uidUser) {
        return new LiveData<Long>() {
            @Override
            public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<Long> observer) {
                super.observe(owner, observer);
                List<Long> countTotal = new ArrayList<>();
                firebaseChatRepository.getByUidUser(uidUser)
                        .flattenAsObservable(chatFirebases -> chatFirebases)
                        .flatMapSingle(chatFirebase -> firebaseMessageRepository.getCountMessageByUidUserAndUidChat(uidUser, chatFirebase.getUid()))
                        .doOnNext(countTotal::add)
                        .doOnComplete(() -> {
                            Long total = 0L;
                            for (Long count : countTotal) {
                                total = total + count;
                            }
                            observer.onChanged(total);
                        })
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .subscribe();
            }
        };
    }
}
