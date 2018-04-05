package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;

public class MyChatsActivityViewModel extends AndroidViewModel {

    public enum TypeRecherche {
        PAR_ANNONCE,
        PAR_UTILISATEUR,
        PAR_CHAT
    }

    private boolean twoPane;
    private String selectedUidChat;
    private AnnonceEntity selectedAnnonce;
    private TypeRecherche typeRecherche;

    public MyChatsActivityViewModel(@NonNull Application application) {
        super(application);
        typeRecherche = TypeRecherche.PAR_UTILISATEUR;
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

    private void setSelectedUidChat(String selectedUidChat) {
        this.selectedUidChat = selectedUidChat;
    }

    public AnnonceEntity getSelectedAnnonce() {
        return selectedAnnonce;
    }

    private void setSelectedAnnonce(AnnonceEntity selectedUidAnnonce) {
        this.selectedAnnonce = selectedUidAnnonce;
    }

    public TypeRecherche getTypeRecherche() {
        return typeRecherche;
    }

    public void rechercheByUidUtilisateur() {
        typeRecherche = TypeRecherche.PAR_UTILISATEUR;
    }

    public void rechercheByUidAnnonce(AnnonceEntity annonce) {
        typeRecherche = TypeRecherche.PAR_ANNONCE;
        selectedAnnonce = annonce;
    }

    public void rechercheByUidChat(String uidChat) {
        typeRecherche = TypeRecherche.PAR_CHAT;
        selectedUidChat = uidChat;
    }

    public ChatFirebase createNewFirebaseChat(AnnonceEntity annonce) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_CHATS_REF).push();

        HashMap<String, Boolean> hash = new HashMap<>();
        hash.put(FirebaseAuth.getInstance().getUid(), true);
        hash.put(annonce.getUuidUtilisateur(), true);

        ChatFirebase chatFirebase = new ChatFirebase();
        chatFirebase.setUid(ref.getKey());
        chatFirebase.setUidAnnonce(annonce.getUUID());
        chatFirebase.setMembers(hash);
        chatFirebase.setUidBuyer(FirebaseAuth.getInstance().getUid());
        chatFirebase.setUidSeller(annonce.getUuidUtilisateur());

        return chatFirebase;
    }
}
