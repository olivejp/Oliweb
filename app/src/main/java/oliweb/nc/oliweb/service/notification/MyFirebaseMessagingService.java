package oliweb.nc.oliweb.service.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.utility.Constants;

import static oliweb.nc.oliweb.service.sync.SyncService.ARG_ACTION_SEND_DIRECT_MESSAGE;
import static oliweb.nc.oliweb.utility.Constants.notificationSyncAnnonceId;

/**
 * Created by 2761oli on 22/03/2018.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String KEY_ORIGIN_CHAT = "KEY_CHAT_ORIGIN";
    private static final String KEY_CHAT_UID = "KEY_CHAT_UID";
    public static final String KEY_TEXT_TO_SEND = "KEY_TEXT_TO_SEND";

    private MessageRepository messageRepository;
    private ChatRepository chatRepository;

    public MyFirebaseMessagingService() {
        messageRepository = MessageRepository.getInstance(getApplicationContext());
        chatRepository = ChatRepository.getInstance(getApplicationContext());
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> datas = remoteMessage.getData();
        if (datas.containsKey(KEY_ORIGIN_CHAT) && datas.containsKey(KEY_CHAT_UID)) {
            createChatDirectReplyNotification(datas.get(KEY_CHAT_UID));
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
            builder.setContentTitle(remoteMessage.getNotification().getTitle());
            builder.setContentText(remoteMessage.getNotification().getBody());
            builder.setSmallIcon(R.drawable.ic_person_white_48dp);
            NotificationManagerCompat.from(this).notify(notificationSyncAnnonceId, builder.build());
        }
        super.onMessageReceived(remoteMessage);
    }

    private void createChatDirectReplyNotification(String chatUid) {

        // On va appeler un service pour enregistrer le message en DB et l'envoyer ensuite sur Firebase
        Intent intent = new Intent(this, SyncService.class);
        intent.setAction(ARG_ACTION_SEND_DIRECT_MESSAGE);
        PendingIntent pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Création du Remote Input
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_TO_SEND).setLabel("Répondre").build();

        // Création de l'action
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_send_red_300_48dp, "Répondre", pendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        // Récupération d'un builder de notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);

        // Lecture du chat et de ses messages
        chatRepository.findByUid(chatUid)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(chatEntity -> {
                    messageRepository.getSingleByIdChat(chatEntity.getIdChat())
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(messageEntities -> {

                                // Création du style de notification
                                NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle("Moi")
                                        .setConversationTitle(chatEntity.getTitreAnnonce());

                                // TODO limiter aux 4 derniers messages + aller récupérer le nom de l'auteur du message dans Firebase
                                // Récupération de tous les messages du chat
                                if (!messageEntities.isEmpty()) {
                                    for (MessageEntity message : messageEntities) {
                                        messagingStyle.addMessage(message.getMessage(), message.getTimestamp(), message.getUidAuthor());
                                    }
                                }

                                builder.setStyle(messagingStyle);
                            })
                            .subscribe();
                })
                .subscribe();

        builder.addAction(action);
        NotificationManagerCompat.from(this).notify(notificationSyncAnnonceId, builder.build());
    }
}
