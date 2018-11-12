package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Scheduler;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;
import oliweb.nc.oliweb.ui.activity.business.MyChatsActivityBusiness;
import oliweb.nc.oliweb.utility.CustomLiveData;
import oliweb.nc.oliweb.utility.LiveDataOnce;
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
    private AnnonceEntity annonce;
    private TypeRechercheChat typeRechercheChat;
    private TypeRechercheMessage typeRechercheMessage;
    private ChatEntity currentChat;
    private String firebaseUserUid;
    private MutableLiveData<Map<String, UserEntity>> liveDataPhotoUrlUsers;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Inject
    ChatRepository chatRepository;

    @Inject
    MessageRepository messageRepository;

    @Inject
    MyChatsActivityBusiness business;

    @Inject
    FirebaseChatService fbChatService;

    @Inject
    @Named("processScheduler")
    Scheduler processScheduler;

    public MyChatsActivityViewModel(@NonNull Application application) {
        super(application);
        ((App) getApplication()).getDatabaseRepositoriesComponent().inject(this);
        ((App) getApplication()).getFirebaseServicesComponent().inject(this);
        ((App) getApplication()).getBusinessComponent().inject(this);

        this.liveDataPhotoUrlUsers = new MutableLiveData<>();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(application);
    }

    public String getFirebaseUserUid() {
        return firebaseUserUid;
    }

    public void setFirebaseUserUid(String firebaseUserUid) {
        this.firebaseUserUid = firebaseUserUid;
    }

    public LiveData<List<ChatEntity>> getChatsByUidUserWithOrderByTitreAnnonce() {
        return chatRepository.findByUidUserAndStatusNotInWithOrderByTitreAnnonce(firebaseUserUid, Utility.allStatusToAvoid());
    }

    public LiveData<List<ChatEntity>> getChatsByUidAnnonceWithOrderByTitreAnnonce() {
        return chatRepository.findByUidAnnonceAndStatusNotInWithOrderByTitreAnnonce(annonce.getUid(), Utility.allStatusToAvoid());
    }

    public LiveDataOnce<AnnonceFirebase> findLiveFirebaseByUidAnnonce(String uidAnnonce) {
        return business.findLiveFirebaseByUidAnnonce(uidAnnonce);
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

    public AnnonceEntity getAnnonce() {
        return annonce;
    }

    public TypeRechercheChat getTypeRechercheChat() {
        return typeRechercheChat;
    }

    public TypeRechercheMessage getTypeRechercheMessage() {
        return typeRechercheMessage;
    }

    public ChatEntity getCurrentChat() {
        return currentChat;
    }

    public void setCurrentChat(ChatEntity currentChat) {
        this.currentChat = currentChat;
    }

    public void setTypeRechercheChat(TypeRechercheChat typeRechercheChat) {
        this.typeRechercheChat = typeRechercheChat;
    }

    public void rechercheMessageByChat(ChatEntity chatEntity) {
        typeRechercheMessage = TypeRechercheMessage.PAR_CHAT;
        selectedIdChat = chatEntity.getIdChat();
        currentChat = chatEntity;
    }

    public void rechercheMessageByUidChat(String uidChat) {
        typeRechercheMessage = TypeRechercheMessage.PAR_CHAT;
        chatRepository.findByUid(uidChat)
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .doOnSuccess(chatEntity -> {
                    currentChat = chatEntity;
                    selectedIdChat = chatEntity.getIdChat();
                })
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }

    public void rechercheMessageByAnnonce(AnnonceEntity annonceEntity) {
        typeRechercheMessage = TypeRechercheMessage.PAR_ANNONCE;
        annonce = annonceEntity;
    }

    public LiveDataOnce<ChatEntity> findLiveChatByUidUserAndUidAnnonce() {
        return business.findLiveChatByUidUserAndUidAnnonce(firebaseUserUid, annonce.getUid());
    }

    /**
     * Search in the local DB if ChatEntity for this uidUser and this uidAnnonce exist otherwise create a new one
     *
     * @return
     */
    public LiveDataOnce<ChatEntity> findOrCreateLiveNewChat() {
        CustomLiveData<ChatEntity> chatEntityCustomLiveData = new CustomLiveData<>();
        business.findOrCreateLiveNewChat(firebaseUserUid, annonce)
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .doOnSuccess(chatFound -> {
                    currentChat = chatFound;
                    selectedIdChat = chatFound.getIdChat();
                    chatEntityCustomLiveData.postValue(currentChat);
                })
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
        return chatEntityCustomLiveData;
    }

    public LiveData<List<MessageEntity>> findAllMessageByIdChat(Long idChat) {
        return messageRepository.findAllByIdChat(idChat);
    }

    /**
     * Insert new message into local DB and update the last message in the chat
     *
     * @param message String to insert as a new message in the loca DB
     * @return True if insertion is successful, false otherwise
     */
    public LiveDataOnce<AtomicBoolean> saveLiveMessage(String message) {
        CustomLiveData<AtomicBoolean> atomicBooleanCustomLiveData = new CustomLiveData<>();

        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setMessage(message);
        messageEntity.setStatusRemote(StatusRemote.TO_SEND);
        messageEntity.setIdChat(selectedIdChat);
        messageEntity.setUidChat(currentChat.getUidChat());
        messageEntity.setUidAuthor(firebaseUserUid);
        messageEntity.setTimestamp(Long.MAX_VALUE);

        messageRepository.singleSave(messageEntity)
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .doOnSuccess(atomicBoolean -> {
                    atomicBooleanCustomLiveData.postValue(new AtomicBoolean(true));
                    sendNewEvent();
                })
                .doOnError(e -> {
                    atomicBooleanCustomLiveData.postValue(new AtomicBoolean(false));
                    Log.e(TAG, e.getLocalizedMessage(), e);
                })
                .subscribe();

        return atomicBooleanCustomLiveData;
    }

    private void sendNewEvent() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "message");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public LiveData<Map<String, UserEntity>> getLiveDataPhotoUrlUsers() {
        return liveDataPhotoUrlUsers;
    }

    /**
     * Va lire tous les chats pour l'uid user, puis pour tout ces chats va
     * récupérer tous les membres et pour tous ces membres va récupérer leur photo URL
     */
    public void getPhotoUrlsByUidUser() {
        this.fbChatService.getPhotoUrlsByUidUser(firebaseUserUid)
                .observeOn(processScheduler).subscribeOn(processScheduler)
                .doOnSuccess(map -> liveDataPhotoUrlUsers.postValue(map))
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }
}
