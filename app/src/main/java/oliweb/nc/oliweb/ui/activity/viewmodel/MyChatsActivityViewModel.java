package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
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
    private ChatRepository chatRepository;
    private MessageRepository messageRepository;
    private FirebaseUserRepository firebaseUserRepository;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private FirebaseChatRepository firebaseChatRepository;
    private ChatEntity currentChat;
    private String firebaseUserUid;

    public MyChatsActivityViewModel(@NonNull Application application) {
        super(application);
        this.chatRepository = ChatRepository.getInstance(application);
        this.messageRepository = MessageRepository.getInstance(application);
        this.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(application);
        this.firebaseUserRepository = FirebaseUserRepository.getInstance();
        this.firebaseChatRepository = FirebaseChatRepository.getInstance();
    }

    public String getFirebaseUserUid() {
        return firebaseUserUid;
    }

    public void setFirebaseUserUid(String firebaseUserUid) {
        this.firebaseUserUid = firebaseUserUid;
    }

    public LiveData<List<ChatEntity>> getChatsByUidUser() {
        return chatRepository.findByUidUserAndStatusNotIn(firebaseUserUid, Utility.allStatusToAvoid());
    }

    public LiveData<List<ChatEntity>> getChatsByUidAnnonce() {
        return chatRepository.findByUidAnnonceAndStatusNotIn(annonce.getUid(), Utility.allStatusToAvoid());
    }

    public Maybe<AnnonceDto> findFirebaseByUidAnnonce(String uidAnnonce) {
        return this.firebaseAnnonceRepository.findMaybeByUidAnnonce(uidAnnonce);
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

    public void setTypeRechercheChat(TypeRechercheChat typeRechercheChat) {
        this.typeRechercheChat = typeRechercheChat;
    }

    public void rechercheMessageByIdChat(Long idChat) {
        typeRechercheMessage = TypeRechercheMessage.PAR_CHAT;
        selectedIdChat = idChat;
        chatRepository.findById(idChat)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(chatEntity -> currentChat = chatEntity)
                .subscribe();
    }

    public void rechercheMessageByUidChat(String uidChat) {
        typeRechercheMessage = TypeRechercheMessage.PAR_CHAT;
        chatRepository.findByUid(uidChat)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(chatEntity -> {
                    currentChat = chatEntity;
                    selectedIdChat = chatEntity.getIdChat();
                })
                .subscribe();
    }

    public void rechercheMessageByAnnonce(AnnonceEntity annonceEntity) {
        typeRechercheMessage = TypeRechercheMessage.PAR_ANNONCE;
        annonce = annonceEntity;
    }

    private ChatEntity initializeChatEntity() {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setStatusRemote(StatusRemote.TO_SEND);
        chatEntity.setUidBuyer(firebaseUserUid);
        chatEntity.setUidAnnonce(annonce.getUid());
        chatEntity.setUidSeller(annonce.getUidUser());
        chatEntity.setTitreAnnonce(annonce.getTitre());
        return chatEntity;
    }

    public Maybe<ChatEntity> findChatByUidUserAndUidAnnonce() {
        return chatRepository.findByUidUserAndUidAnnonce(firebaseUserUid, annonce.getUid());
    }

    /**
     * Search in the local DB if ChatEntity for this uidUser and this uidAnnonce exist otherwise create a new one
     *
     * @return
     */
    public Single<ChatEntity> findOrCreateNewChat() {
        Log.d(TAG, "Starting findOrCreateNewChat uidUser : " + firebaseUserUid + " annonce : " + annonce);
        return Single.create(emitter ->
                chatRepository.findByUidUserAndUidAnnonce(firebaseUserUid, annonce.getUid())
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
                                    chatRepository.singleSave(initializeChatEntity())
                                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                            .doOnSuccess(chatCreated -> {
                                                Log.d(TAG, "findOrCreateNewChat.doOnComplete.singleSave.doOnSuccess chatCreated : " + chatCreated);
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
     * Insert new message into local DB and update the last message in the chat
     *
     * @param message String to insert as a new message in the loca DB
     * @return True if insertion is successful, false otherwise
     */
    public Single<AtomicBoolean> saveMessage(String message) {
        Log.d(TAG, "saveMessage message : " + message);
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setMessage(message);
        messageEntity.setStatusRemote(StatusRemote.TO_SEND);
        messageEntity.setIdChat(selectedIdChat);
        messageEntity.setUidChat(currentChat.getUidChat());
        messageEntity.setUidAuthor(firebaseUserUid);

        return messageRepository.singleSave(messageEntity)
                .map(messageEntity1 -> new AtomicBoolean(true));
    }

    public Single<UtilisateurEntity> findFirebaseUserByUid() {
        return this.firebaseUserRepository.getUtilisateurByUid(firebaseUserUid);
    }

    public LiveDataOnce<List<ChatFirebase>> findFirebaseChatByUidUser(String uidUser) {
        CustomLiveData<List<ChatFirebase>> liveDataChats = new CustomLiveData<>();
        this.firebaseChatRepository.getByUidUser(uidUser)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(liveDataChats::postValue)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
        return liveDataChats;
    }

    public LiveDataOnce<UtilisateurEntity> getFirebaseUserByUid(String uidUser) {
        CustomLiveData<UtilisateurEntity> liveDataUser = new CustomLiveData<>();
        this.firebaseUserRepository.getUtilisateurByUid(uidUser)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(liveDataUser::postValue)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
        return liveDataUser;
    }

    public LiveDataOnce<Map<String, UtilisateurEntity>> getPhotoUrlsByUidUser() {
        CustomLiveData<Map<String, UtilisateurEntity>> liveDataPhotoUrlUsers = new CustomLiveData<>();
        this.firebaseChatRepository.getByUidUser(firebaseUserUid)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .flattenAsObservable(chatFirebases -> chatFirebases)
                .map(chatFirebase -> chatFirebase.getMembers().keySet())
                .flatMapIterable(uidsUserFromChats -> uidsUserFromChats)
                .filter(uidUserFromChat -> !uidUserFromChat.equals(firebaseUserUid))
                .switchMap(foreignUidUserFromChat -> firebaseUserRepository.getUtilisateurByUid(foreignUidUserFromChat).toObservable())
                .toMap(UtilisateurEntity::getPhotoUrl)
                .doOnSuccess(liveDataPhotoUrlUsers::postValue)
                .subscribe();
        return liveDataPhotoUrlUsers;
    }
}
