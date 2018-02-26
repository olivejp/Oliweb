package oliweb.nc.oliweb.job;

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

    public static final String ARG_ID_ANNONCE = "ARG_ID_ANNONCE";
    public static final String ARG_ACTION = "ARG_ACTION";
    public static final String ARG_ACTION_SYNC_ANNONCE = "ARG_ACTION_SYNC_ANNONCE";
    public static final String ARG_ACTION_SYNC_ALL = "ARG_ACTION_SYNC_ALL";
    public static final String ARG_ACTION_SYNC_ALL_FROM_SCHEDULER = "ARG_ACTION_SYNC_ALL_FROM_SCHEDULER";

    public SyncService() {
        super("SyncService");
    }

    /**
     * Launch the sync service for all the annonce
     *
     * @param context
     */
    public static void launchSynchroForAll(@NonNull Context context) {
        Intent syncService = new Intent(context, SyncService.class);
        syncService.putExtra(SyncService.ARG_ACTION, SyncService.ARG_ACTION_SYNC_ALL);
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

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            Bundle bundle = intent.getExtras();
            if (bundle != null && bundle.containsKey(ARG_ACTION)) {
                String action = bundle.getString(ARG_ACTION);
                if (action != null) {
                    switch (action) {
                        case ARG_ACTION_SYNC_ALL:
                            Log.d(TAG, "Lancement du batch interactif pour toutes les annonces");
                            handleActionSyncAll();
                            break;
                        case ARG_ACTION_SYNC_ALL_FROM_SCHEDULER:
                            Log.d(TAG, "Lancement du batch par le Scheduler");
                            handleActionSyncAll();
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }
}
