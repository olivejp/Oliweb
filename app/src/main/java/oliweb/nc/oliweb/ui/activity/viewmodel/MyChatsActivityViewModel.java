package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

public class MyChatsActivityViewModel extends AndroidViewModel {

    public enum TypeRecherche {
        PAR_ANNONCE,
        PAR_UTILISATEUR
    }

    private boolean twoPane;
    private String selectedUidChat;
    private String selectedUidAnnonce;
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

    public void setSelectedUidChat(String selectedUidChat) {
        this.selectedUidChat = selectedUidChat;
    }

    public String getSelectedUidAnnonce() {
        return selectedUidAnnonce;
    }

    public void setSelectedUidAnnonce(String selectedUidAnnonce) {
        this.selectedUidAnnonce = selectedUidAnnonce;
    }

    public TypeRecherche getTypeRecherche() {
        return typeRecherche;
    }

    public void setTypeRecherche(TypeRecherche typeRecherche) {
        this.typeRecherche = typeRecherche;
    }

    public void rechercheByUidUtilisateur() {
        typeRecherche = TypeRecherche.PAR_UTILISATEUR;
    }

    public void rechercheByUidAnnonce(String uidAnnonce) {
        typeRecherche = TypeRecherche.PAR_ANNONCE;
        selectedUidAnnonce = uidAnnonce;
    }
}
