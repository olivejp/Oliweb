package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;

public class MyChatsActivityViewModel extends AndroidViewModel {

    private boolean twoPane;
    private String selectedUidChat;

    public MyChatsActivityViewModel(@NonNull Application application) {
        super(application);
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

    public String getFirebaseUidUser() {
        return FirebaseAuth.getInstance().getUid();
    }
}
