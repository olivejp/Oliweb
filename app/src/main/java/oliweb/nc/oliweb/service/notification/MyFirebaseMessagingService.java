package oliweb.nc.oliweb.service.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.app.TaskStackBuilder;
import androidx.core.graphics.drawable.IconCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.List;
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
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static oliweb.nc.oliweb.service.sync.SyncService.ARG_UID_CHAT;
import static oliweb.nc.oliweb.service.sync.SyncService.ARG_UID_USER;
import static oliweb.nc.oliweb.service.sync.SyncService.ARG_UID_USER_SENDER;
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

    public static final String KEY_ACTION = "KEY_ACTION";
    public static final String KEY_UID_AUTHOR = "KEY_UID_AUTHOR";
    public static final String KEY_UID_ANNONCE = "KEY_UID_ANNONCE";

    public static final String ACTION_NOTIF_ADD_PHOTO = "NOTIF_TO_ADD_PHOTO";

    private MessageRepository messageRepository;
    private ChatRepository chatRepository;
    private MediaUtility mediaUtility;
    private NotificationManager mNotificationManager;
    private UserRepository userRepository;

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Utilisation de Dagger pour récupérer les composants nécessaires
        getComponents();

        // Si aucune notification présente, on sort tout de suite.
        if (remoteMessage.getNotification() == null) {
            super.onMessageReceived(remoteMessage);
            return;
        }

        Map<String, String> datas = remoteMessage.getData();
        Log.d(TAG, "Contenu des datas : " + datas.toString());
        if (datas.containsKey(KEY_ACTION) && ACTION_NOTIF_ADD_PHOTO.equals(datas.get(KEY_ACTION))) {
            Bundle bundle = new Bundle();
            bundle.putString(KEY_UID_ANNONCE, datas.get(KEY_UID_ANNONCE));
            bundle.putString(KEY_UID_AUTHOR, datas.get(KEY_UID_AUTHOR));
            notify(remoteMessage, getPendingIntent(bundle));
        } else if (datas.containsKey(KEY_ORIGIN_CHAT) && datas.containsKey(KEY_CHAT_UID) && datas.containsKey(KEY_CHAT_AUTHOR)) {
            Gson gson = new Gson();
            UserEntity userEntity = gson.fromJson(datas.get(KEY_CHAT_AUTHOR), UserEntity.class);
            String message = remoteMessage.getNotification().getBody();
            String titreAnnonce = remoteMessage.getNotification().getTitle();
            String uidUserSender = datas.get(KEY_CHAT_AUTHOR);
            String receiverUid = datas.get(KEY_CHAT_RECEIVER);
            String uidChat = datas.get(KEY_CHAT_UID);

            // Recherche de l'utilisateur dans la base, puis création du direct reply
            userRepository.findMaybeByUid(receiverUid)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnSuccess(userReceiver -> createChatDirectReplyNotification(uidChat, uidUserSender, titreAnnonce, userEntity, message, userReceiver))
                    .doOnComplete(() -> Log.e(TAG, "L'utilisateur receiver n'a pas été trouvé dans la base."))
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .subscribe();
        } else {
            // Mode par défaut dans le cas, où on reçoit une notification, on va ouvrir le chat
            Bundle bundle = new Bundle();
            bundle.putString(MainActivity.ACTION_REDIRECT, MainActivity.ACTION_CHAT);
            notify(remoteMessage, getPendingIntent(bundle));
        }
        super.onMessageReceived(remoteMessage);
    }

    private void getComponents() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(new ContextModule(this)).build();
        UtilityComponent utilityComponent = DaggerUtilityComponent.builder().utilityModule(new UtilityModule()).build();
        messageRepository = component.getMessageRepository();
        chatRepository = component.getChatRepository();
        mediaUtility = utilityComponent.getMediaUtility();
        userRepository = component.getUserRepository();
    }

    private void notify(RemoteMessage remoteMessage, PendingIntent resultPendingIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
        if (remoteMessage.getNotification() != null) {
            builder.setContentTitle(remoteMessage.getNotification().getTitle());
            builder.setContentText(remoteMessage.getNotification().getBody());
            builder.setSmallIcon(R.mipmap.ic_banana_launcher_round);
            builder.setContentIntent(resultPendingIntent);
            NotificationManagerCompat.from(this).notify(NOTIFICATION_SYNC_ANNONCE_ID, builder.build());
        }
    }

    private PendingIntent getPendingIntent(Bundle bundle) {
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtras(bundle);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
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
    private void createChatDirectReplyNotification(String chatUid, String uidUserSender, String annonceTitre, UserEntity authorEntity, String message, UserEntity userReceiver) {

        // On va appeler un service pour enregistrer la réponse à la notification reçue
        PendingIntent pendingIntent;
        pendingIntent = buildPendingIntent(chatUid, uidUserSender, message, userReceiver.getUid());

        // Création du Remote Input
        RemoteInput remoteInput = new RemoteInput.Builder(KEY_TEXT_TO_SEND).setLabel(getString(R.string.answer)).build();

        // Création de la personne receuveuse
        Person receiverPerson = buildPersonByUser(userReceiver);
        Person authorPerson = buildPersonByUser(authorEntity);

        // Création du style de notification
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(receiverPerson).setConversationTitle(annonceTitre);

        // Création de l'action pour répondre rapidement
        NotificationCompat.Action actionReply = getAction(pendingIntent, remoteInput);

        // Création d'une channel UNIQUEMENT dans le cas d'une version supérieure a OREO.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID, Constants.OLIWEB_CHANNEL, NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
        }

        // Récupération d'un builder de notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
        builder.addAction(actionReply);
        builder.setSmallIcon(R.mipmap.ic_banana_launcher);
        builder.setLargeIcon(mediaUtility.getBitmapFromURL(authorEntity.getPhotoUrl()));
        builder.setAutoCancel(true);
        builder.setUsesChronometer(true);

        // Si on a activé les notifications dans les paramètres, et qu'on a sélectionné une sonnette spéciale
        boolean notificationMessage = SharedPreferencesHelper.getInstance(getApplicationContext()).getNotificationsMessage();
        if (notificationMessage) {
            Uri ringtone = SharedPreferencesHelper.getInstance(getApplicationContext()).getNotificationsMessageRingtone();
            builder.setSound(ringtone);
        }

        // Lecture du chat et de ses messages
        chatRepository.findByUid(chatUid)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnComplete(() -> sendNotification(builder, messagingStyle))
                .map(ChatEntity::getIdChat)
                .flatMapSingle(messageRepository::getSingleByIdChat)
                .doOnSuccess(messageEntities -> feedMessagingStyleWithMessages(authorEntity.getUid(), receiverPerson, authorPerson,builder, messagingStyle, messageEntities))
                .subscribe();
    }

    private void feedMessagingStyleWithMessages(String authorUid, Person receiverPerson, Person authorPerson, NotificationCompat.Builder builder, NotificationCompat.MessagingStyle messagingStyle, List<MessageEntity> messageEntities) {
        if (!messageEntities.isEmpty()) {
            for (MessageEntity msg : messageEntities) {
                Person author = (msg.getUidAuthor().equals(authorUid)) ? authorPerson : receiverPerson;
                messagingStyle.addMessage(msg.getMessage(), msg.getTimestamp(), author);
            }
        }
        sendNotification(builder, messagingStyle);
    }

    private NotificationCompat.Action getAction(PendingIntent pendingIntent, RemoteInput remoteInput) {
        return new NotificationCompat.Action.Builder(R.drawable.ic_send_red_300_48dp, getString(R.string.answer), pendingIntent)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .addRemoteInput(remoteInput)
                .setAllowGeneratedReplies(true)
                .build();
    }

    private Person buildPersonByUser(UserEntity user) {
        // Recherche de l'image du user
        IconCompat icon = null;
        Bitmap bitmap = mediaUtility.getBitmapFromURL(user.getPhotoUrl());
        if (bitmap != null) {
            icon = IconCompat.createWithBitmap(bitmap);
        }

        // Création de la personne
        Person.Builder builder = new Person.Builder()
                .setName(user.getProfile())
                .setBot(false)
                .setIcon(icon)
                .setImportant(false);

        return builder.build();
    }

    private PendingIntent buildPendingIntent(String chatUid, String uidUserSender, String message, String receiverUid) {
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(this, SyncService.class);
            intent.setAction(ARG_ACTION_SEND_DIRECT_MESSAGE);
            intent.putExtra(ARG_UID_CHAT, chatUid);
            intent.putExtra(ARG_UID_USER_SENDER, uidUserSender);
            intent.putExtra(SyncService.ARG_ACTION_SEND_DIRECT_MESSAGE, message);
            intent.putExtra(ARG_UID_USER, receiverUid);
            pendingIntent = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        } else {
            Intent intent = new Intent(this, MyChatsActivity.class);
            intent.setAction(ARG_ACTION_SEND_DIRECT_MESSAGE);
            intent.putExtra(ARG_UID_CHAT, chatUid);
            intent.putExtra(ARG_UID_USER_SENDER, uidUserSender);
            intent.putExtra(ARG_ACTION_SEND_DIRECT_MESSAGE, message);
            intent.putExtra(ARG_UID_USER, receiverUid);
            pendingIntent = PendingIntent.getActivity(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
        return pendingIntent;
    }

    private void sendNotification(NotificationCompat.Builder builder, NotificationCompat.MessagingStyle messagingStyle) {
        builder.setSmallIcon(R.mipmap.ic_banana_launcher);
        builder.setContentTitle("Your Title");
        builder.setContentText("Your text");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
        } else {
            builder.setPriority(Notification.PRIORITY_MAX);
        }
        builder.setStyle(messagingStyle);
        Notification notif = builder.build();
        mNotificationManager.notify(NOTIFICATION_SYNC_ANNONCE_ID, notif);
    }
}
