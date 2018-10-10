package oliweb.nc.oliweb.service.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.Person;
import android.support.v4.app.RemoteInput;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.graphics.drawable.IconCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Map;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerUtilityComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.UtilityComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.system.dagger.module.UtilityModule;
import oliweb.nc.oliweb.ui.activity.MainActivity;
import oliweb.nc.oliweb.ui.activity.MyChatsActivity;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.MediaUtility;

import static oliweb.nc.oliweb.service.sync.SyncService.ARG_UID_CHAT;
import static oliweb.nc.oliweb.service.sync.SyncService.ARG_UID_USER;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.ARG_ACTION_SEND_DIRECT_MESSAGE;
import static oliweb.nc.oliweb.utility.Constants.NOTIFICATION_SYNC_ANNONCE_ID;

/**
 * Created by 2761oli on 22/03/2018.
 * <a href="https://medium.com/google-developer-experts/exploring-android-p-enhanced-notifications-a9adb8d78387"></a>
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = MyFirebaseMessagingService.class.getCanonicalName();

    private static final String KEY_ORIGIN_CHAT = "KEY_CHAT_ORIGIN";
    private static final String KEY_CHAT_UID = "KEY_CHAT_UID";
    private static final String KEY_CHAT_AUTHOR = "KEY_CHAT_AUTHOR";
    public static final String KEY_TEXT_TO_SEND = "KEY_TEXT_TO_SEND";
    public static final String KEY_CHAT_RECEIVER = "KEY_CHAT_RECEIVER";

    private MessageRepository messageRepository;
    private ChatRepository chatRepository;
    private MediaUtility mediaUtility;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(new ContextModule(this)).build();
        UtilityComponent utilityComponent = DaggerUtilityComponent.builder().utilityModule(new UtilityModule()).build();

        messageRepository = component.getMessageRepository();
        chatRepository = component.getChatRepository();
        mediaUtility = utilityComponent.getMediaUtility();
        UserRepository userRepository = component.getUserRepository();

        if (remoteMessage.getNotification() != null) {
            Map<String, String> datas = remoteMessage.getData();
            if (datas.containsKey(KEY_ORIGIN_CHAT) && datas.containsKey(KEY_CHAT_UID) && datas.containsKey(KEY_CHAT_AUTHOR)) {
                Gson gson = new Gson();
                UserEntity userEntity = gson.fromJson(datas.get(KEY_CHAT_AUTHOR), UserEntity.class);
                String message = remoteMessage.getNotification().getBody();
                String titreAnnonce = remoteMessage.getNotification().getTitle();
                String uidUser = datas.get(KEY_CHAT_RECEIVER);
                String uidChat = datas.get(KEY_CHAT_UID);

                // Recherche de l'utilisateur dans la base, puis création du direct reply
                userRepository.findMaybeByUid(uidUser)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnSuccess(userReceiver -> createChatDirectReplyNotification(uidChat, titreAnnonce, userEntity, message, userReceiver))
                        .doOnComplete(() -> Log.e(TAG, "L'utilisateur receuveur n'a pas été trouvé dans la base."))
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .subscribe();
            } else {
                Intent resultIntent = new Intent(this, MainActivity.class);
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addNextIntentWithParentStack(resultIntent);
                PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
                builder.setContentTitle(remoteMessage.getNotification().getTitle());
                builder.setContentText(remoteMessage.getNotification().getBody());
                builder.setSmallIcon(R.mipmap.ic_banana_launcher_round);
                builder.setContentIntent(resultPendingIntent);
                NotificationManagerCompat.from(this).notify(NOTIFICATION_SYNC_ANNONCE_ID, builder.build());
            }
        }
        super.onMessageReceived(remoteMessage);
    }

    /**
     * Permet de créer une notification pour une réponse rapide.
     * A partir de la version 24 on va appeler un service.
     * Pour les versions antérieures on va appeler une activité.
     *
     * @param chatUid
     * @param annonceTitre
     * @param authorEntity
     * @param message
     * @param userReceiver
     */
    private void createChatDirectReplyNotification(String chatUid, String
            annonceTitre, UserEntity authorEntity, String message, UserEntity userReceiver) {

        // On va appeler un service pour enregistrer la réponse à la notification reçue
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(this, SyncService.class);
            intent.setAction(ARG_ACTION_SEND_DIRECT_MESSAGE);
            intent.putExtra(ARG_UID_CHAT, chatUid);
            intent.putExtra(SyncService.ARG_ACTION_SEND_DIRECT_MESSAGE, message);
            intent.putExtra(ARG_UID_USER, userReceiver.getUid());
            pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            Intent intent = new Intent(this, MyChatsActivity.class);
            intent.setAction(ARG_ACTION_SEND_DIRECT_MESSAGE);
            intent.putExtra(ARG_UID_CHAT, chatUid);
            intent.putExtra(ARG_ACTION_SEND_DIRECT_MESSAGE, message);
            intent.putExtra(ARG_UID_USER, userReceiver.getUid());
            pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // Création du Remote Input
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_TO_SEND).setLabel(getString(R.string.answer)).build();

        // Création de la personne receuveuse
        Person receiverPerson = new Person.Builder()
                .setName(userReceiver.getProfile())
                .setBot(false)
                .setImportant(false)
                .build();

        // Création de l'action pour répondre rapidement
        NotificationCompat.Action action = new NotificationCompat.Action.Builder(R.drawable.ic_send_red_300_48dp, getString(R.string.answer), pendingIntent)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();

        // Récupération d'un builder de notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
        builder.addAction(action);
        builder.setSmallIcon(R.drawable.ic_message_white_48dp);
        builder.setLargeIcon(mediaUtility.getBitmapFromURL(authorEntity.getPhotoUrl()));
        builder.setAutoCancel(true);
        builder.setUsesChronometer(true);

        // Création du style de notification
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(receiverPerson).setConversationTitle(annonceTitre);

        // Lecture du chat et de ses messages
        chatRepository.findByUid(chatUid)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnComplete(() -> sendNotification(builder, messagingStyle))
                .map(ChatEntity::getIdChat)
                .flatMapSingle(messageRepository::getSingleByIdChat)
                .doOnSuccess(messageEntities -> {
                    if (!messageEntities.isEmpty()) {
                        for (MessageEntity msg : messageEntities) {
                            Person author = (msg.getUidAuthor().equals(authorEntity.getUid())) ? createPersonByName(authorEntity) : receiverPerson;
                            messagingStyle.addMessage(msg.getMessage(), msg.getTimestamp(), author);
                        }
                    }
                    sendNotification(builder, messagingStyle);
                })
                .subscribe();
    }

    private Person createPersonByName(UserEntity user) {

        // Création de la personne
        Person.Builder builder = new Person.Builder()
                .setName(user.getProfile())
                .setBot(false)
                .setImportant(false);

        // Recherche de l'image du user
        Bitmap bitmap = mediaUtility.getBitmapFromURL(user.getPhotoUrl());
        if (bitmap != null) {
            builder.setIcon(IconCompat.createWithBitmap(bitmap));
        }
        return builder.build();
    }

    private void sendNotification(NotificationCompat.Builder builder, NotificationCompat.MessagingStyle messagingStyle) {
        builder.setStyle(messagingStyle);
        NotificationManagerCompat.from(this).notify(NOTIFICATION_SYNC_ANNONCE_ID, builder.build());
    }
}
