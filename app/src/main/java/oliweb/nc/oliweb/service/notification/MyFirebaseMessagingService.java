package oliweb.nc.oliweb.service.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Map;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.dagger.module.ContextModule;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.ui.activity.MainActivity;
import oliweb.nc.oliweb.ui.activity.MyChatsActivity;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.MediaUtility;

import static oliweb.nc.oliweb.service.sync.SyncService.ARG_ACTION_SEND_DIRECT_MESSAGE_UID_CHAT;
import static oliweb.nc.oliweb.service.sync.SyncService.ARG_ACTION_SEND_DIRECT_UID_USER;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.ARG_ACTION_SEND_DIRECT_MESSAGE;
import static oliweb.nc.oliweb.utility.Constants.notificationSyncAnnonceId;

/**
 * Created by 2761oli on 22/03/2018.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String KEY_ORIGIN_CHAT = "KEY_CHAT_ORIGIN";
    private static final String KEY_CHAT_UID = "KEY_CHAT_UID";
    private static final String KEY_CHAT_AUTHOR = "KEY_CHAT_AUTHOR";
    public static final String KEY_TEXT_TO_SEND = "KEY_TEXT_TO_SEND";
    public static final String KEY_CHAT_RECEIVER = "KEY_CHAT_RECEIVER";

    private MessageRepository messageRepository;
    private ChatRepository chatRepository;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(new ContextModule(this)).build();
        messageRepository = component.getMessageRepository();
        chatRepository = component.getChatRepository();

        Map<String, String> datas = remoteMessage.getData();
        if (remoteMessage.getNotification() != null && datas.containsKey(KEY_ORIGIN_CHAT) && datas.containsKey(KEY_CHAT_UID) && datas.containsKey(KEY_CHAT_AUTHOR)) {
            Gson gson = new Gson();
            UserEntity userEntity = gson.fromJson(datas.get(KEY_CHAT_AUTHOR), UserEntity.class);
            String message = remoteMessage.getNotification().getBody();
            String uidUser = datas.get(KEY_CHAT_RECEIVER);
            String uidChat = datas.get(KEY_CHAT_UID);
            String titreAnnonce = remoteMessage.getNotification().getTitle();
            createChatDirectReplyNotification(uidChat, titreAnnonce, userEntity, message, uidUser);
        } else {
            if (remoteMessage.getNotification() != null) {
                Intent resultIntent = new Intent(this, MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addNextIntentWithParentStack(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
                builder.setContentTitle(remoteMessage.getNotification().getTitle());
                builder.setContentText(remoteMessage.getNotification().getBody());
                builder.setSmallIcon(R.drawable.ic_launcher_round_splash);
                builder.setContentIntent(resultPendingIntent);
                NotificationManagerCompat.from(this).notify(notificationSyncAnnonceId, builder.build());
            }
        }
        super.onMessageReceived(remoteMessage);
    }

    private void createChatDirectReplyNotification(String chatUid, String annonceTitre, UserEntity authorEntity, String message, String uidReceiver) {

        // On va appeler un service pour enregistrer la réponse à la notification reçue
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(this, SyncService.class);
            intent.setAction(ARG_ACTION_SEND_DIRECT_MESSAGE);
            intent.putExtra(ARG_ACTION_SEND_DIRECT_MESSAGE_UID_CHAT, chatUid);
            intent.putExtra(SyncService.ARG_ACTION_SEND_DIRECT_MESSAGE, message);
            intent.putExtra(ARG_ACTION_SEND_DIRECT_UID_USER, uidReceiver);
            pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            Intent intent = new Intent(this, MyChatsActivity.class);
            intent.setAction(ARG_ACTION_SEND_DIRECT_MESSAGE);
            intent.putExtra(ARG_ACTION_SEND_DIRECT_MESSAGE_UID_CHAT, chatUid);
            intent.putExtra(ARG_ACTION_SEND_DIRECT_MESSAGE, message);
            intent.putExtra(ARG_ACTION_SEND_DIRECT_UID_USER, uidReceiver);
            pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // Création du Remote Input
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_TO_SEND).setLabel("Répondre").build();

        // Création de l'action
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_send_red_300_48dp, "Répondre", pendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        // Récupération d'un builder de notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
        builder.addAction(action);
        builder.setSmallIcon(R.drawable.ic_message_white_48dp);
        builder.setLargeIcon(MediaUtility.getBitmapFromURL(authorEntity.getPhotoUrl()));
        builder.setAutoCancel(true);

        // Création du style de notification
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle("Moi").setConversationTitle(annonceTitre);

        // Lecture du chat et de ses messages
        chatRepository.findByUid(chatUid)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnSuccess(chatEntity ->
                        messageRepository.getSingleByIdChat(chatEntity.getIdChat())
                                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                .doOnSuccess(messageEntities -> {
                                    // Récupération de tous les messages du chat
                                    if (!messageEntities.isEmpty()) {
                                        for (MessageEntity messageEntity : messageEntities) {
                                            String auteur = messageEntity.getUidAuthor().equals(authorEntity.getUid()) ? authorEntity.getProfile() : "Moi";
                                            messagingStyle.addMessage(messageEntity.getMessage(), messageEntity.getTimestamp(), auteur);
                                        }
                                    }
                                    sendNotification(builder, messagingStyle);
                                })
                                .subscribe()
                )
                .doOnComplete(() -> sendNotification(builder, messagingStyle))
                .subscribe();
    }

    private void sendNotification(NotificationCompat.Builder builder, NotificationCompat.MessagingStyle messagingStyle) {
        builder.setStyle(messagingStyle);
        NotificationManagerCompat.from(this).notify(notificationSyncAnnonceId, builder.build());
    }
}
