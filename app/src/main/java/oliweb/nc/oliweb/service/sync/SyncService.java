package oliweb.nc.oliweb.service.sync;

import android.app.IntentService;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.ServicesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static oliweb.nc.oliweb.service.notification.MyFirebaseMessagingService.KEY_TEXT_TO_SEND;

/**
 * This class is called by SyncTask or by SyncJobCreator
 */
public class SyncService extends IntentService {

    private static final String TAG = SyncService.class.getName();

    public static final String ARG_UID_UTILISATEUR = "ARG_UID_UTILISATEUR";
    public static final String ARG_ACTION = "ARG_ACTION";
    public static final String ARG_ACTION_SYNC_ALL_FROM_SCHEDULER = "ARG_ACTION_SYNC_ALL_FROM_SCHEDULER";
    public static final String ARG_ACTION_SYNC_FROM_FIREBASE = "ARG_ACTION_SYNC_FROM_FIREBASE";
    public static final String ARG_ACTION_SYNC_USER = "ARG_ACTION_SYNC_USER";

    public static final String ARG_UID_CHAT = "ARG_UID_CHAT";
    public static final String ARG_ACTION_SEND_DIRECT_MESSAGE = "ARG_ACTION_SEND_DIRECT_MESSAGE";
    public static final String ARG_UID_USER = "ARG_UID_USER";
    public static final String ARG_UID_USER_SENDER = "ARG_UID_USER_SENDER";

    private ServicesComponent servicesComponent;
    private FirebaseServicesComponent firebaseServicesComponent;
    private DatabaseRepositoriesComponent databaseRepositoriesComponent;

    public SyncService() {
        super("SyncService");
    }

    /**
     * Launch the sync service for all the user
     *
     * @param context
     */
    public static void launchSynchroForUser(@NonNull Context context, String uidUser) {
        Intent syncService = new Intent(context, SyncService.class);
        syncService.putExtra(SyncService.ARG_ACTION, SyncService.ARG_ACTION_SYNC_USER);
        syncService.putExtra(SyncService.ARG_UID_UTILISATEUR, uidUser);
        context.startService(syncService);
    }

    /**
     * Launch the sync service to retrieve datas from Firebase
     *
     * @param context
     * @param uidUtilisateur
     */
    public static void launchSynchroFromFirebase(@NonNull Context context, String uidUtilisateur) {
        Intent syncService = new Intent(context, SyncService.class);
        syncService.putExtra(SyncService.ARG_ACTION, SyncService.ARG_ACTION_SYNC_FROM_FIREBASE);
        syncService.putExtra(SyncService.ARG_UID_UTILISATEUR, uidUtilisateur);
        context.startService(syncService);
    }

    /**
     * Lancement du service de synchro pour tous les objets mais à partir du scheduler
     *
     * @param context
     */
    public static void launchSynchroFromScheduler(@NonNull Context context) {
        Intent syncService = new Intent(context, SyncService.class);
        syncService.putExtra(SyncService.ARG_ACTION, SyncService.ARG_ACTION_SYNC_ALL_FROM_SCHEDULER);
        context.startService(syncService);
    }

    private void handleActionSyncAll(String uidUser) {
        ScheduleSync scheduleSync = servicesComponent.getScheduleSync();
        scheduleSync.synchronize(uidUser);
    }

    private void handleActionSyncFromFirebase(String uidUtilisateur) {
        FirebaseRetrieverService firebaseRetrieverService = firebaseServicesComponent.getFirebaseRetrieverService();
        firebaseRetrieverService.synchronize(this, uidUtilisateur)
                .doOnNext(annonceFirebase -> Log.w(TAG, "Enregistrement de l'annonce " + annonceFirebase.getUuid() + " réussi !"))
                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage()))
                .doOnComplete(() -> Log.w(TAG, "Toutes les annonces ont été récupérées !"))
                .subscribe();
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;

        ContextModule contextModule = new ContextModule(this);
        servicesComponent = DaggerServicesComponent.builder().contextModule(contextModule).build();
        firebaseServicesComponent = DaggerFirebaseServicesComponent.builder().contextModule(contextModule).build();
        databaseRepositoriesComponent = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();

        Bundle bundle = intent.getExtras();
        if (ARG_ACTION_SEND_DIRECT_MESSAGE.equals(intent.getAction())) {
            saveMessage(intent);
        } else if (bundle != null && bundle.containsKey(ARG_ACTION)) {
            String action = bundle.getString(ARG_ACTION);
            String uidUser;
            if (action != null) {
                switch (action) {
                    case ARG_ACTION_SYNC_ALL_FROM_SCHEDULER:
                        uidUser = SharedPreferencesHelper.getInstance(this).getUidFirebaseUser();
                        Log.d(TAG, "Lancement du batch par le Scheduler pour l'utilisateur " + uidUser);
                        handleActionSyncAll(uidUser);
                        break;
                    case ARG_ACTION_SYNC_FROM_FIREBASE:
                        uidUser = bundle.getString(ARG_UID_UTILISATEUR);
                        Log.d(TAG, "Lancement du batch pour récupérer les données sur Firebase et les importer en local pour l'utilisateur " + uidUser);
                        handleActionSyncFromFirebase(uidUser);
                        break;
                    case ARG_ACTION_SYNC_USER:
                        uidUser = bundle.getString(ARG_UID_UTILISATEUR);
                        Log.d(TAG, "Lancement du batch pour envoyer sur Firebase les informations de l'utilisateur " + uidUser);
                        handleActionSyncAll(uidUser);
                        break;
                    default:
                        break;
                }
            }
        }

    }

    private void saveMessage(@Nullable Intent intent) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
            Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
            if (remoteInput != null && remoteInput.getCharSequence(KEY_TEXT_TO_SEND) != null) {

                // Récupération du texte à envoyer
                String messageToSend = remoteInput.getCharSequence(KEY_TEXT_TO_SEND).toString();

                // Récupération des données pour créer un message
                String uidChat = intent.getStringExtra(ARG_UID_CHAT);
                String uidUser = intent.getStringExtra(ARG_UID_USER);

                // Recherche du chat dans notre DB locale
                ChatRepository chatRepository = databaseRepositoriesComponent.getChatRepository();
                chatRepository.findByUid(uidChat)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnSuccess(chatEntity -> {
                            // Création d'un message
                            MessageEntity messageEntity = new MessageEntity();
                            messageEntity.setIdChat(chatEntity.getIdChat());
                            messageEntity.setMessage(messageToSend);
                            messageEntity.setStatusRemote(StatusRemote.TO_SEND);
                            messageEntity.setUidChat(uidChat);
                            messageEntity.setUidAuthor(uidUser);
                            messageEntity.setTimestamp(Long.MAX_VALUE);

                            MessageRepository messageRepository = databaseRepositoriesComponent.getMessageRepository();
                            messageRepository.singleSave(messageEntity)
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .subscribe();
                        })
                        .subscribe();
            }
        }
    }
}
