package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;

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
    private String selectedUidChat;
    private String selectedUidUtilisateur;
    private AnnonceEntity selectedAnnonce;
    private TypeRechercheChat typeRechercheChat;
    private TypeRechercheMessage typeRechercheMessage;
    private ChatRepository chatRepository;
    private FirebaseChatRepository firebaseChatRepository;
    private MessageRepository messageRepository;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private MutableLiveData<ChatEntity> liveChat;
    private ChatEntity currentChat;

    public MyChatsActivityViewModel(@NonNull Application application) {
        super(application);
        this.chatRepository = ChatRepository.getInstance(application);
        this.firebaseChatRepository = FirebaseChatRepository.getInstance(application);
        this.messageRepository = MessageRepository.getInstance(application);
        this.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(application);
    }

    public LiveData<List<ChatEntity>> getFirebaseChatsByUidUser() {
        firebaseChatRepository.sync(selectedUidUtilisateur);
        return chatRepository.findByUidUser(selectedUidUtilisateur);
    }

    public LiveData<List<ChatEntity>> getFirebaseChatsByUidAnnonce() {
        return chatRepository.findByUidAnnonce(selectedAnnonce.getUuid());
    }

    public Single<AnnonceDto> findFirebaseByUidAnnonce(String uidAnnonce) {
        return this.firebaseAnnonceRepository.findByUidAnnonce(uidAnnonce);
    }

    public boolean isTwoPane() {
        return twoPane;
    }

    public void setTwoPane(boolean twoPane) {
        this.twoPane = twoPane;
    }

    public String getSelectedUidChat() {
        return selectedUidChat;
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

    public void rechercheMessageByUidChat(String uidChat) {
        typeRechercheMessage = TypeRechercheMessage.PAR_CHAT;
        selectedUidChat = uidChat;
    }

    public void rechercheMessageByAnnonce(AnnonceEntity annonceEntity) {
        typeRechercheMessage = TypeRechercheMessage.PAR_ANNONCE;
        selectedAnnonce = annonceEntity;
    }

    public void updateChat(String uidChat, MessageFirebase messageFirebase) {
        firebaseChatRepository.updateChat(uidChat, messageFirebase)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(atomicBoolean -> Log.d(TAG, "updateChat successful"))
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }

    private ChatEntity createChatEntity(String uidBuyer, AnnonceEntity annonce) {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setStatusRemote(StatusRemote.TO_SEND);
        chatEntity.setUidBuyer(uidBuyer);
        chatEntity.setUidAnnonce(annonce.getUuid());
        chatEntity.setUidSeller(annonce.getUuidUtilisateur());
        return chatEntity;
    }

    // Search in the local DB if ChatEntity for this uidUser and this uidAnnonce exist otherwise create a new one
    public LiveData<ChatEntity> findOrCreateChat(String uidUser, AnnonceEntity annonce) {
        if (liveChat == null) {
            liveChat = new MutableLiveData<>();
        }
        chatRepository.findByUidUserAndUidAnnonce(uidUser, annonce.getUuid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(chatEntity -> {
                    currentChat = chatEntity;
                    liveChat.postValue(currentChat);
                })
                .doOnComplete(() ->
                        chatRepository.saveWithSingle(createChatEntity(uidUser, annonce))
                                .doOnSuccess(chatSaved -> {
                                    currentChat = chatSaved;
                                    liveChat.postValue(currentChat);
                                })
                                .subscribe()
                )
                .subscribe();

        return liveChat;
    }

    /**
     * Insert new message into local DB
     *
     * @param message
     * @return
     */
    public Single<MessageEntity> sendMessage(String message) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setMessage(message);
        messageEntity.setStatusRemote(StatusRemote.TO_SEND);
        messageEntity.setIdChat(currentChat.getIdChat());
        messageEntity.setUidAuthor(FirebaseAuth.getInstance().getUid());
        return messageRepository.saveWithSingle(messageEntity);
    }
}
