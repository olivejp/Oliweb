package oliweb.nc.oliweb.service.sync;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

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
        CoreSync.getInstance(this.getApplicationContext()).synchronize();
    }

    private void handleActionSyncFromFirebase(String uidUtilisateur) {
        FirebaseRetreiverService.getInstance(this).synchronize(this, uidUtilisateur);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null && bundle.containsKey(ARG_ACTION)) {
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
                            CoreSync.getInstance(this).synchronizeUser();
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }
}
