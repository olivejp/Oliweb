package oliweb.nc.oliweb.service.sync;

import android.app.IntentService;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import oliweb.nc.oliweb.dagger.component.DaggerFirebaseServicesComponent;
import oliweb.nc.oliweb.dagger.component.FirebaseServicesComponent;
import oliweb.nc.oliweb.dagger.module.ContextModule;

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

    public static final String ARG_ACTION_SEND_DIRECT_MESSAGE_UID_CHAT = "ARG_ACTION_SEND_DIRECT_MESSAGE_UID_CHAT";
    public static final String ARG_ACTION_SEND_DIRECT_MESSAGE = "ARG_ACTION_SEND_DIRECT_MESSAGE";
    public static final String ARG_ACTION_SEND_DIRECT_UID_USER = "ARG_ACTION_SEND_DIRECT_UID_USER";

    private FirebaseServicesComponent componentFbServices;

    public SyncService() {
        super("SyncService");
    }

    /**
     * Launch the sync service for all the user
     *
     * @param context
     */
    public static void launchSynchroForUser(@NonNull Context context) {
        Intent syncService = new Intent(context, SyncService.class);
        syncService.putExtra(SyncService.ARG_ACTION, SyncService.ARG_ACTION_SYNC_USER);
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

    private void handleActionSyncAll() {
        ScheduleSync scheduleSync = componentFbServices.getScheduleSync();
        scheduleSync.synchronize();
    }

    private void handleActionSyncFromFirebase(String uidUtilisateur) {
        FirebaseRetrieverService firebaseRetrieverService = componentFbServices.getFirebaseRetrieverService();
        firebaseRetrieverService.synchronize(this, uidUtilisateur);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;

        ContextModule contextModule = new ContextModule(this);
        componentFbServices = DaggerFirebaseServicesComponent.builder().contextModule(contextModule).build();

        Bundle bundle = intent.getExtras();
        if (ARG_ACTION_SEND_DIRECT_MESSAGE.equals(intent.getAction())) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT_WATCH) {
                Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
                if (remoteInput != null && remoteInput.getCharSequence(KEY_TEXT_TO_SEND) != null) {
                    String messageToSend = remoteInput.getCharSequence(KEY_TEXT_TO_SEND).toString();

                    // TODO enregistrer le message en base et l'envoyer sur Firebase

                }
            }
        } else if (bundle != null && bundle.containsKey(ARG_ACTION)) {
            String action = bundle.getString(ARG_ACTION);
            if (action != null) {
                switch (action) {
                    case ARG_ACTION_SYNC_ALL_FROM_SCHEDULER:
                        Log.d(TAG, "Lancement du batch par le Scheduler");
                        handleActionSyncAll();
                        break;
                    case ARG_ACTION_SYNC_FROM_FIREBASE:
                        Log.d(TAG, "Lancement du batch pour récupérer les données sur Firebase et les importer en local");
                        String uidUtilisateur = bundle.getString(ARG_UID_UTILISATEUR);
                        handleActionSyncFromFirebase(uidUtilisateur);
                        break;
                    case ARG_ACTION_SYNC_USER:
                        Log.d(TAG, "Lancement du batch pour envoyer les informations des utilisateurs sur Firebase");
                        handleActionSyncAll();
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
