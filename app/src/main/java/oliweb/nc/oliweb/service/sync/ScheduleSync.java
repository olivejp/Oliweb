package oliweb.nc.oliweb.service.sync;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.dto.firebase.ChatFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;

import static oliweb.nc.oliweb.utility.Utility.allStatusToDelete;
import static oliweb.nc.oliweb.utility.Utility.allStatusToSend;

/**
 * Created by orlanth23 on 18/12/2017.
 */
@Singleton
public class ScheduleSync {
    private static final String TAG = ScheduleSync.class.getName();

    private FirebaseUserRepository firebaseUserRepository;
    private UserRepository userRepository;
    private AnnonceRepository annonceRepository;
    private AnnonceFirebaseSender annonceFirebaseSender;
    private ChatRepository chatRepository;
    private PhotoRepository photoRepository;
    private MessageRepository messageRepository;
    private FirebaseMessageService firebaseMessageService;
    private FirebaseChatService firebaseChatService;
    private Scheduler processScheduler;
    private AnnonceFirebaseDeleter annonceFirebaseDeleter;

    @Inject
    public ScheduleSync(FirebaseUserRepository firebaseUserRepository,
                        UserRepository userRepository,
                        AnnonceRepository annonceRepository,
                        AnnonceFirebaseSender annonceFirebaseSender,
                        AnnonceFirebaseDeleter annonceFirebaseDeleter,
                        ChatRepository chatRepository,
                        PhotoRepository photoRepository,
                        MessageRepository messageRepository,
                        FirebaseMessageService firebaseMessageService,
                        FirebaseChatService firebaseChatService,
                        @Named("processScheduler") Scheduler processScheduler) {
        this.firebaseUserRepository = firebaseUserRepository;
        this.annonceFirebaseSender = annonceFirebaseSender;
        this.userRepository = userRepository;
        this.annonceRepository = annonceRepository;
        this.photoRepository = photoRepository;
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.firebaseChatService = firebaseChatService;
        this.firebaseMessageService = firebaseMessageService;
        this.processScheduler = processScheduler;
        this.annonceFirebaseDeleter = annonceFirebaseDeleter;
    }

    public void synchronize(String uidUser) {
        sendAnnonces(uidUser);
        sendMessages();
        sendChats(uidUser);
        sendUtilisateur(uidUser);
    }

    public Flowable<AnnonceEntity> getFlowableAnnonceToSend(String uidUser) {
        return annonceRepository.findFlowableByUidUserAndStatusIn(uidUser, allStatusToSend())
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .distinct()
                .doOnNext(annonceFirebaseSender::processToSendAnnonceToFirebase)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e));
    }

    public Flowable<ChatEntity> getFlowableChat(String uidUser) {
        return chatRepository.findFlowableByUidUserAndStatusIn(uidUser, allStatusToSend())
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .distinct()
                .doOnNext(firebaseChatService::sendNewChat)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e));
    }

    public Observable<ChatFirebase> getFlowableMessageToSend() {
        return messageRepository.findFlowableByStatusAndUidChatNotNull(allStatusToSend())
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .distinct()
                .toObservable()
                .flatMapIterable(list -> list)
                .switchMap(firebaseMessageService::sendMessage)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e));
    }

    public Flowable<UserEntity> getFlowableUserToSend(String uidUser) {
        return userRepository.findFlowableByUid(uidUser)
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .filter(userEntity -> allStatusToSend().contains(userEntity.getStatut().getValue()))
                .flatMapSingle(firebaseUserRepository::insertUserIntoFirebase)
                .flatMapSingle(userRepository::markAsSend)
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception));
    }

    // TODO le retour de cette méthode est bizarre, voir pour faire un concatMap ou un switchMap plutot qu'un map
    public Observable<Disposable> getPhotoToDelete() {
        return photoRepository.getAllPhotosByStatus(allStatusToDelete())
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .toObservable()
                .map(photoEntity ->
                        annonceFirebaseDeleter.deleteOnePhoto(photoEntity).toObservable()
                                .subscribeOn(processScheduler).observeOn(processScheduler)
                                .switchMap(atomicBoolean -> annonceRepository.findById(photoEntity.getIdAnnonce()).toObservable())
                                .filter(annonceEntity -> annonceEntity.getStatut() == StatusRemote.SEND)
                                .switchMap(annonceFirebaseSender::convertToFullAndSendToFirebase)
                                .subscribe()
                )
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e));
    }

    public Flowable<AnnonceEntity> getFlowableAnnonceToDelete(String uidUser) {
        return annonceRepository.findFlowableByUidUserAndStatusIn(uidUser, allStatusToDelete())
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .doOnNext(annonceFirebaseDeleter::processToDeleteAnnonce)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e));
    }


    private Observable<AnnonceEntity> getObservableAnnonceToSend(String uidUser) {
        return annonceRepository.findSingleByUidUserAndStatusIn(uidUser, allStatusToSend())
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .flattenAsObservable(annonceEntities -> annonceEntities)
                .doOnNext(annonceFirebaseSender::processToSendAnnonceToFirebase)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e));
    }

    private Observable<ChatEntity> getObservableChat(String uidUser) {
        return chatRepository.findObservableByUidUserAndStatusIn(uidUser, allStatusToSend())
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .flattenAsObservable(chatEntities -> chatEntities)
                .doOnNext(firebaseChatService::sendNewChat)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e));
    }

    private Observable<ChatFirebase> getObservableMessageToSend() {
        return messageRepository.findSingleByStatusAndUidChatNotNull(allStatusToSend())
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .flattenAsObservable(messageEntities -> messageEntities)
                .switchMap(firebaseMessageService::sendMessage)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e));
    }

    private Single<UserEntity> getSingleUserToSend(String uidUser) {
        return userRepository.findSingleByUid(uidUser)
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .filter(userEntity -> allStatusToSend().contains(userEntity.getStatut().getValue()))
                .flatMapSingle(firebaseUserRepository::insertUserIntoFirebase)
                .flatMap(userRepository::markAsSend)
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception));
    }

    private void sendAnnonces(String uidUser) {
        getObservableAnnonceToSend(uidUser).subscribe();
    }

    private void sendChats(String uidUser) {
        getObservableChat(uidUser).subscribe();
    }

    private void sendMessages() {
        getObservableMessageToSend().subscribe();
    }

    private void sendUtilisateur(String uidUser) {
        getSingleUserToSend(uidUser).subscribe();
    }
}
