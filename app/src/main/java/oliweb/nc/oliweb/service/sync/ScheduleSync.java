package oliweb.nc.oliweb.service.sync;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 18/12/2017.
 * <p>
 * This class contains the series of network calls to make to sync local db with firebase
 */
@Singleton
public class ScheduleSync {
    private static final String TAG = ScheduleSync.class.getName();

    private FirebaseUserRepository firebaseUserRepository;
    private UserRepository userRepository;
    private AnnonceRepository annonceRepository;
    private AnnonceFirebaseSender annonceFirebaseSender;
    private ChatRepository chatRepository;
    private MessageRepository messageRepository;
    private FirebaseMessageService firebaseMessageService;
    private FirebaseChatService firebaseChatService;

    @Inject
    public ScheduleSync(FirebaseUserRepository firebaseUserRepository,
                        UserRepository userRepository,
                        AnnonceRepository annonceRepository,
                        AnnonceFirebaseSender annonceFirebaseSender,
                        ChatRepository chatRepository,
                        MessageRepository messageRepository,
                        FirebaseMessageService firebaseMessageService,
                        FirebaseChatService firebaseChatService) {
        this.firebaseUserRepository = firebaseUserRepository;
        this.annonceFirebaseSender = annonceFirebaseSender;
        this.userRepository = userRepository;
        this.annonceRepository = annonceRepository;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.firebaseChatService = firebaseChatService;
        this.firebaseMessageService = firebaseMessageService;
    }

    public void synchronize() {
        sendAnnonces();
        sendMessages();
        sendChats();
        sendUtilisateurs();
    }

    private void sendAnnonces() {
        annonceRepository.findAllByStatus(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .flattenAsObservable(annonceEntities -> annonceEntities)
                .doOnNext(annonceFirebaseSender::processToSendAnnonceToFirebase)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }


    private void sendChats() {
        chatRepository.findAllByStatusIn(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .flattenAsObservable(chatEntities -> chatEntities)
                .doOnNext(firebaseChatService::sendNewChat)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }

    private void sendMessages() {
        messageRepository.findAllByStatus(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .toObservable()
                .flatMapIterable(list -> list)
                .switchMap(firebaseMessageService::sendMessage)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }

    private void sendUtilisateurs() {
        userRepository.findAllByStatus(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .flattenAsObservable(userEntities -> userEntities)
                .flatMapSingle(utilisateur -> firebaseUserRepository.insertUserIntoFirebase(utilisateur)
                        .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                        .doOnSuccess(success -> {
                            if (success.get()) {
                                utilisateur.setStatut(StatusRemote.SEND);
                                userRepository.singleSave(utilisateur)
                                        .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                                        .subscribe();
                            }
                        })
                )
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .subscribe();
    }
}
