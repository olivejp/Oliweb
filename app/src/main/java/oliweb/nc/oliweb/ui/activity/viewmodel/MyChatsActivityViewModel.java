package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.firebase.service.FirebaseChatService;
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
    private FirebaseChatService firebaseChatService;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    public MyChatsActivityViewModel(@NonNull Application application) {
        super(application);
        this.chatRepository = ChatRepository.getInstance(application);
        this.firebaseChatRepository = FirebaseChatRepository.getInstance();
        this.firebaseAnnonceRepository = FirebaseAnnonceRepository.getInstance(application);
        this.firebaseChatService = FirebaseChatService.getInstance(application);
    }

    public LiveData<List<ChatEntity>> getFirebaseChatsByUidUser() {
        firebaseChatService.sync(selectedUidUtilisateur);
        return chatRepository.findByUidUser(selectedUidUtilisateur);
    }

    public LiveData<List<ChatEntity>> getFirebaseChatsByUidAnnonce() {
        return chatRepository.findByUidAnnonce(selectedAnnonce.getUUID());
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

    public void rechercheChatByUidAnnonce(AnnonceEntity annonce) {
        typeRechercheChat = TypeRechercheChat.PAR_ANNONCE;
        selectedAnnonce = annonce;
    }

    public void rechercheMessageByUidChat(String uidChat) {
        typeRechercheMessage = TypeRechercheMessage.PAR_CHAT;
        selectedUidChat = uidChat;
    }

    public void rechercheMessageByAnnonce(AnnonceEntity annonceEntity) {
        typeRechercheMessage = TypeRechercheMessage.PAR_ANNONCE;
        selectedAnnonce = annonceEntity;
    }

    public Single<ChatFirebase> createChat(String uidUserBuyer, AnnonceEntity annonce) {
        return firebaseChatRepository.createChat(uidUserBuyer, annonce);
    }

    public Single<AtomicBoolean> updateChat(String uidChat, MessageFirebase messageFirebase) {
        return firebaseChatRepository.updateChat(uidChat, messageFirebase);
    }
}
