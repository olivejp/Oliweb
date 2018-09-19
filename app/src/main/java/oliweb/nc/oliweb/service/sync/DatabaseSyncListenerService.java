package oliweb.nc.oliweb.service.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import oliweb.nc.oliweb.App;

/**
 * Created by orlanth23 on 14/05/2018.
 * This class will listen to the local database and send items to Firebase Database
 */
public class DatabaseSyncListenerService extends Service {

    private static final String TAG = DatabaseSyncListenerService.class.getName();

    public static final String CHAT_SYNC_UID_USER = "CHAT_SYNC_UID_USER";

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Inject
    ScheduleSync scheduleSync;

    @Inject
    @Named("processScheduler")
    Scheduler processScheduler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Démarrage du service DatabaseSyncListenerService");

        // Condition de garde : Récupération de l'UID de l'utilisateur
        if (intent.getStringExtra(CHAT_SYNC_UID_USER) == null || intent.getStringExtra(CHAT_SYNC_UID_USER).isEmpty()) {
            stopSelf();
        }

        String uidUser = intent.getStringExtra(CHAT_SYNC_UID_USER);

        ((App) getApplication()).getServicesComponent().inject(this);

        // Suppression des listeners
        disposables.clear();

        // SENDERS
        // Envoi toutes les annonces
        disposables.add(scheduleSync.getFlowableAnnonceToSend(uidUser).subscribe());

        // Envoi tous les chats
        disposables.add(scheduleSync.getFlowableChat(uidUser).subscribe());

        // Envoi tous les messages
        disposables.add(scheduleSync.getFlowableMessageToSend().subscribe());

        // Envoi tous les utilisateurs
        disposables.add(scheduleSync.getFlowableUserToSend().subscribe());

        // DELETERS
        // Suppression des annonces
        disposables.add(scheduleSync.getFlowableAnnonceToDelete(uidUser).subscribe());

        // Suppression des photos
        disposables.add(scheduleSync.getPhotoToDelete().subscribe());

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Stop DatabaseSyncListenerService Bye bye");
        disposables.dispose();
        super.onDestroy();
    }
}
