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
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.MessageConverter;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.dto.firebase.ChatFirebase;
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

    @Inject
    public FirebaseMessageService(FirebaseMessageRepository firebaseMessageRepository,
                                  FirebaseChatRepository firebaseChatRepository,
                                  MessageRepository messageRepository) {
        this.firebaseChatRepository = firebaseChatRepository;
        this.firebaseMessageRepository = firebaseMessageRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * If we already have an Uid and a timestamp, we return directly a markAsSendingAndTryToSendToFirebase()
     * Otherwise we try to get a new uid and a timestamp.
     *
     * @param messageEntity
     * @return
     */
    public Observable<ChatFirebase> sendMessage(final MessageEntity messageEntity) {
        Log.d(TAG, "SendMessage messageEntity : " + messageEntity);
        if (messageEntity.getUidMessage() == null) {
            return firebaseMessageRepository.getUidAndTimestampFromFirebase(messageEntity.getUidChat(), messageEntity)
                    .toObservable()
                    .switchMap(this::markAsSendingAndTryToSendToFirebase)
                    .doOnError(e -> messageRepository.markMessageAsFailedToSend(messageEntity));
        } else {
            return markAsSendingAndTryToSendToFirebase(messageEntity)
                    .doOnError(e -> messageRepository.markMessageAsFailedToSend(messageEntity));
        }
    }

    private Observable<ChatFirebase> markAsSendingAndTryToSendToFirebase(MessageEntity messageEntity) {
        return messageRepository.markMessageIsSending(messageEntity)
                .map(MessageConverter::convertEntityToDto)
                .switchMapSingle(firebaseMessageRepository::saveMessage)
                .switchMap(messageFirebase -> messageRepository.markMessageHasBeenSend(messageEntity))
                .switchMap(firebaseChatRepository::updateLastMessageChat);
    }

    public LiveData<Long> getCountMessageByUidUser(String uidUser) {
        return new LiveData<Long>() {
            @Override
            public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<Long> observer) {
                super.observe(owner, observer);
                getCount(uidUser).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess(integer -> observer.onChanged(integer.longValue()))
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .subscribe();
            }
        };
    }

    public Single<Integer> getCount(String uidUser) {
        return Single.create(emitter -> {
            List<Integer> countTotal = new ArrayList<>();
            firebaseChatRepository.getByUidUser(uidUser)
                    .flattenAsObservable(chatFirebases -> chatFirebases)
                    .flatMapSingle(chatFirebase -> firebaseMessageRepository.getCountMessageByUidUserAndUidChat(uidUser, chatFirebase.getUid()))
                    .doOnNext(count -> countTotal.add(count.intValue()))
                    .doOnComplete(() -> {
                        Integer total = 0;
                        for (Integer count : countTotal) {
                            total = total + count;
                        }
                        emitter.onSuccess(total);
                    })
                    .doOnError(emitter::onError)
                    .subscribe();
        });
    }
}
