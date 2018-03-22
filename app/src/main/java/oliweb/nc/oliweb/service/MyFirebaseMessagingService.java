package oliweb.nc.oliweb.service;

import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;

import static oliweb.nc.oliweb.Constants.notificationSyncAnnonceId;

/**
 * Created by 2761oli on 22/03/2018.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO overrider cette méthode pour faire apparaître clairement les notifications.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
        builder.setContentTitle(remoteMessage.getNotification().getTitle());
        builder.setContentText(remoteMessage.getNotification().getBody());
        builder.setSmallIcon(R.drawable.ic_person_white_48dp);
        NotificationManagerCompat.from(this).notify(notificationSyncAnnonceId, builder.build());

        super.onMessageReceived(remoteMessage);
    }
}
