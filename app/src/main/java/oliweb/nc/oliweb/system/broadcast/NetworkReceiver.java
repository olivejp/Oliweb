package oliweb.nc.oliweb.system.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by orlanth23 on 09/10/2017
 *
 * Cette classe permet d'écouter les changements de connexion de l'appareil.
 * Il suffit d'appeler une instance du NetworkReceiver et d'ajouter un NetworkChangeListener
 * grâce à la méthode listen()
 */
public class NetworkReceiver extends BroadcastReceiver {

    public static final IntentFilter CONNECTIVITY_CHANGE_INTENT_FILTER = new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");

    private static NetworkReceiver mInstance;
    private static List<NetworkChangeListener> mNetworkChangeListener = new ArrayList<>();

    private NetworkReceiver() {
        super();
    }

    public static synchronized NetworkReceiver getInstance() {
        if (mInstance == null) {
            mInstance = new NetworkReceiver();
        }
        return mInstance;
    }

    public void listen(NetworkChangeListener networkChangeListener) {
        if (mNetworkChangeListener.contains(networkChangeListener)) {
            mNetworkChangeListener.indexOf(networkChangeListener);
        } else {
            if (mNetworkChangeListener.add(networkChangeListener)) {
                mNetworkChangeListener.indexOf(networkChangeListener);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        notifyListener(context);
    }

    private static void notifyListener(Context context) {
        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn != null ? conn.getActiveNetworkInfo() : null;

        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected()) {

            for (NetworkChangeListener networkChangeListener :
                    mNetworkChangeListener) {
                networkChangeListener.onNetworkEnable();
            }
        } else if (networkInfo == null || !networkInfo.isAvailable() || !networkInfo.isConnected()) {
            for (NetworkChangeListener networkChangeListener :
                    mNetworkChangeListener) {
                networkChangeListener.onNetworkDisable();
            }
        }
    }

    public static boolean checkConnection(Context context) {
        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo networkInfo = conn != null ? conn.getActiveNetworkInfo() : null;

        if (networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected() && !networkInfo.isFailover() && networkInfo.getDetailedState().equals(NetworkInfo.DetailedState.CONNECTED)) {
            return true;
        } else if (networkInfo == null || !networkInfo.isAvailable() || !networkInfo.isConnected()) {
            return false;
        }
        return false;
    }

    public interface NetworkChangeListener {
        void onNetworkEnable();

        void onNetworkDisable();
    }
}
