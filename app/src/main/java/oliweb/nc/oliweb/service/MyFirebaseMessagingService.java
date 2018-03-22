package oliweb.nc.oliweb.service;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by 2761oli on 22/03/2018.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO overrider cette méthode pour faire apparaître clairement les notifications.
        super.onMessageReceived(remoteMessage);
    }
}
