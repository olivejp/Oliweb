package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;

import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.firebase.FirebaseQueryLiveData;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.utility.Constants;

public class MyChatsActivityViewModel extends AndroidViewModel {

    public enum TypeRechercheChat {
        PAR_ANNONCE,
        PAR_UTILISATEUR
    }

    public enum TypeRechercheMessage {
        PAR_ANNONCE,
        PAR_CHAT
    }

    private boolean twoPane;
    private FirebaseQueryLiveData listChatsLiveData;
    private String selectedUidChat;
    private String selectedUidUtilisateur;
    private AnnonceEntity selectedAnnonce;
    private TypeRechercheChat typeRechercheChat;
    private TypeRechercheMessage typeRechercheMessage;
    private DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_CHATS_REF);

    public MyChatsActivityViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<DataSnapshot> getFirebaseChatsByUidUser(String uidUser) {
        Query query = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_CHATS_REF).orderByChild("members/" + uidUser).equalTo(true);
        if (listChatsLiveData == null) {
            listChatsLiveData = new FirebaseQueryLiveData(query, false);
        }
        return listChatsLiveData;
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

    public String getSelectedUidUtilisateur() {
        return selectedUidUtilisateur;
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

    public ChatFirebase createChat(AnnonceEntity annonce) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_CHATS_REF).push();

        HashMap<String, Boolean> hash = new HashMap<>();
        hash.put(FirebaseAuth.getInstance().getCurrentUser().getUid(), true);
        hash.put(annonce.getUuidUtilisateur(), true);

        ChatFirebase chatFirebase = new ChatFirebase();
        chatFirebase.setUid(ref.getKey());
        chatFirebase.setUidAnnonce(annonce.getUUID());
        chatFirebase.setMembers(hash);
        chatFirebase.setUidBuyer(FirebaseAuth.getInstance().getCurrentUser().getUid());
        chatFirebase.setUidSeller(annonce.getUuidUtilisateur());

        return chatFirebase;
    }

    public void updateChat(String uidChat, MessageFirebase messageFirebase) {
        // Mise à jour du dernier message dans le chat ainsi que la date de mise à jour.
        chatRef.child(uidChat).child("lastMessage")
                .setValue(messageFirebase.getMessage())
                .addOnSuccessListener(aVoid1 -> chatRef.child(uidChat).child("updateTimestamp").setValue(ServerValue.TIMESTAMP));
    }
}
