package oliweb.nc.oliweb.network;

import android.support.v7.app.AppCompatActivity;

import com.firebase.ui.auth.AuthUI;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by orlanth23 on 01/02/2018.
 */

public class CallLoginUi {

    private CallLoginUi() {
    }

    public static void callLoginUi(AppCompatActivity activityCaller, int requestCode) {
        List<AuthUI.IdpConfig> listProviders = new ArrayList<>();
        listProviders.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        listProviders.add(new AuthUI.IdpConfig.FacebookBuilder().build());
        listProviders.add(new AuthUI.IdpConfig.EmailBuilder().build());

        activityCaller.startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(listProviders)
                        .build(),
                requestCode);
    }
}
