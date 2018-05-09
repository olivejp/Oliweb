package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.utility.Utility;

public class MyChatsActivityViewModel extends AndroidViewModel {

    private static final String TAG = MyChatsActivityViewModel.class.getName();

    public enum TypeRechercheChat {
        PAR_ANNONCE,
        PAR_UTILISATEUR
    }

    public enum TypeRechercheMessage {
        PAR_ANNONCE,
        PAR_CHAT
    }

    private boolean twoPane;
    private Long selectedIdChat;
    private String selectedUidUtilisateur;
    private AnnonceEntity selectedAnnonce;
    private TypeRechercheChat typeRechercheChat;
    private TypeRechercheMessage typeRechercheMessage;
    private ChatRepository chatRepository;
    private MessageRepository messageRepository;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private ChatEntity currentChat;

    public MyChatsActivityViewModel(@NonNull Application application) {
        super(application);
        this.chatRepository = ChatRepository.getInstance(application);
        this.messageRepository = MessageRepository.getInstance(application);
        this.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(application);
    }

    public LiveData<List<ChatEntity>> getChatsByUidUser() {
        return chatRepository.findByUidUserAndStatusNotIn(selectedUidUtilisateur, Utility.allStatusToAvoid());
    }

    public LiveData<List<ChatEntity>> getChatsByUidAnnonce() {
        return chatRepository.findByUidAnnonceAndStatusNotIn(selectedAnnonce.getUid(), Utility.allStatusToAvoid());
    }

    public Maybe<AnnonceDto> findFirebaseByUidAnnonce(String uidAnnonce) {
        return this.firebaseAnnonceRepository.findByUidAnnonce(uidAnnonce);
    }

    public boolean isTwoPane() {
        return twoPane;
    }

    public void setTwoPane(boolean twoPane) {
        this.twoPane = twoPane;
    }

    public Long getSearchedIdChat() {
        return selectedIdChat;
    }

    public AnnonceEntity getSelectedAnnonce() {
        return selectedAnnonce;
    }

    public TypeRechercheChat getTypeRechercheChat() {
        return typeRechercheChat;
    }

    public TypeRechercheMessage getTypeRechercheMessage() {
        return typeRechercheMessage;
    }

    public void rechercheChatByUidUtilisateur(String uidUtilisateur) {
        typeRechercheChat = TypeRechercheChat.PAR_UTILISATEUR;
        selectedUidUtilisateur = uidUtilisateur;
    }

    public void rechercheMessageByUidChat(Long idChat) {
        typeRechercheMessage = TypeRechercheMessage.PAR_CHAT;
        selectedIdChat = idChat;
    }

    public void rechercheMessageByAnnonce(AnnonceEntity annonceEntity) {
        typeRechercheMessage = TypeRechercheMessage.PAR_ANNONCE;
        selectedAnnonce = annonceEntity;
    }

    private ChatEntity createChatEntity(String uidBuyer, AnnonceEntity annonce) {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setStatusRemote(StatusRemote.TO_SEND);
        chatEntity.setUidBuyer(uidBuyer);
        chatEntity.setUidAnnonce(annonce.getUid());
        chatEntity.setUidSeller(annonce.getUidUser());
        chatEntity.setTitreAnnonce(annonce.getTitre());
        return chatEntity;
    }

    /**
     * @param uidUser
     * @param annonce
     * @return
     */
    public Maybe<ChatEntity> findChatByUidUserAndUidAnnonce(String uidUser, AnnonceEntity annonce) {
        Log.d(TAG, "Starting findChatByUidUserAndUidAnnonce uidUser : " + uidUser + " annonce : " + annonce);
        return chatRepository.findByUidUserAndUidAnnonce(uidUser, annonce.getUid());
    }

    /**
     * Search in the local DB if ChatEntity for this uidUser and this uidAnnonce exist otherwise create a new one
     *
     * @param uidUser
     * @param annonce
     * @return
     */
    public Single<ChatEntity> findOrCreateNewChat(String uidUser, AnnonceEntity annonce) {
        Log.d(TAG, "Starting findOrCreateNewChat uidUser : " + uidUser + " annonce : " + annonce);
        return Single.create(emitter ->
                chatRepository.findByUidUserAndUidAnnonce(uidUser, annonce.getUid())
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .doOnSuccess(chatFound -> {
                            Log.d(TAG, "findOrCreateNewChat.doOnSuccess chatFound :" + chatFound);
                            currentChat = chatFound;
                            selectedIdChat = chatFound.getIdChat();
                            emitter.onSuccess(currentChat);
                        })
                        .doOnComplete(() -> {
                                    Log.d(TAG, "findOrCreateNewChat.doOnComplete");
                                    chatRepository.saveWithSingle(createChatEntity(uidUser, annonce))
                                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                            .doOnSuccess(chatCreated -> {
                                                Log.d(TAG, "findOrCreateNewChat.doOnComplete.saveWithSingle.doOnSuccess chatCreated : " + chatCreated);
                                                currentChat = chatCreated;
                                                selectedIdChat = chatCreated.getIdChat();
                                                emitter.onSuccess(currentChat);
                                            })
                                            .subscribe();
                                }
                        )
                        .subscribe()
        );
    }

    public LiveData<List<MessageEntity>> findAllMessageByIdChat(Long idChat) {
        return messageRepository.findAllByIdChat(idChat);
    }

    /**
     * Insert new message into local DB
     * if network is available, we call SyncService
     *
     * @param message
     * @return
     */
    public Single<AtomicBoolean> saveMessage(String message) {
        Log.d(TAG, "saveMessage message : " + message);
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setMessage(message);
        messageEntity.setStatusRemote(StatusRemote.TO_SEND);
        messageEntity.setIdChat(selectedIdChat);
        messageEntity.setUidAuthor(FirebaseAuth.getInstance().getUid());

        return Single.create(emitter ->
                messageRepository.saveWithSingle(messageEntity)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(entity -> {
                            if (NetworkReceiver.checkConnection(getApplication())) {
                                SyncService.launchSynchroForMessage(getApplication());
                            }
                            emitter.onSuccess(new AtomicBoolean(true));
                        })
                        .subscribe()
        );
    }
}
