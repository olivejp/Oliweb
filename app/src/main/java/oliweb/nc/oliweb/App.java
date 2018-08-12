package oliweb.nc.oliweb;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Intent;

import com.evernote.android.job.JobManager;
import com.facebook.stetho.Stetho;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.leakcanary.LeakCanary;

import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.service.job.SyncJob;
import oliweb.nc.oliweb.service.job.SyncJobCreator;
import oliweb.nc.oliweb.service.sync.listener.DatabaseSyncListenerService;
import oliweb.nc.oliweb.service.sync.listener.FirebaseSyncListenerService;


/**
 * Created by orlanth23 on 14/01/2018.
 */

public class App extends Application implements NetworkReceiver.NetworkChangeListener {

    private FirebaseUser mFirebaseUser;
    private Intent intentLocalDbService;
    private Intent intentFirebaseDbService;

    @Override
    public void onCreate() {
        super.onCreate();
        // TODO Supprimer la lib en prod
        Stetho.initializeWithDefaults(this);

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

        // Création des intents pour les services de synchronisation
        intentLocalDbService = new Intent(getApplicationContext(), DatabaseSyncListenerService.class);
        intentFirebaseDbService = new Intent(getApplicationContext(), FirebaseSyncListenerService.class);

        // On attache le receiver à notre application
        registerReceiver(NetworkReceiver.getInstance(), NetworkReceiver.CONNECTIVITY_CHANGE_INTENT_FILTER);

        // On va écouter le Broadcast Listener pour lancer le service de synchro uniquement dans le
        // cas où il y a du réseau.
        NetworkReceiver.getInstance().listen(this);

        // Implémentation de l'auth listener
        // Si l'utilisateur vient de se connecter il faut lancer les services, mais seulement s'il y a une connexion
        // On enregistre l'auth listener
        if (FirebaseAuth.getInstance() != null) {
            FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
                mFirebaseUser = firebaseAuth.getCurrentUser();
                authUpdate();
            });
        }

        // Plannification d'un job
        JobManager.create(this).addJobCreator(new SyncJobCreator());
        SyncJob.scheduleJob();
    }

    /**
     * Vérification qu'il y a bien un utilisateur connecté et qu'l y a une connexion
     * avant de lancer les services de synchro.
     */
    private void authUpdate() {
        stopAllServices();
        if (mFirebaseUser != null && NetworkReceiver.checkConnection(this)) {
            launchServices(mFirebaseUser.getUid());
        }
    }

    /**
     * Lancement des services de synchronisation
     *
     * @param uidUser de l'utilisateur à connecter
     */
    private void launchServices(String uidUser) {

        stopServicesIfRunning();

        // Lancement du service pour écouter la DB en local
        intentLocalDbService.putExtra(DatabaseSyncListenerService.CHAT_SYNC_UID_USER, uidUser);
        startService(intentLocalDbService);

        // Lancement du service pour écouter Firebase
        intentFirebaseDbService.putExtra(FirebaseSyncListenerService.CHAT_SYNC_UID_USER, uidUser);
        startService(intentFirebaseDbService);
    }

    private void stopServicesIfRunning() {
        if (isServiceRunning("oliweb.nc.oliweb.service.sync.listener.DatabaseSyncListenerService")) {
            stopService(intentFirebaseDbService);
        }

        if (isServiceRunning("oliweb.nc.oliweb.service.sync.listener.FirebaseSyncListenerService")) {
            stopService(intentFirebaseDbService);
        }
    }

    /**
     * Stoppe les services de synchronisation
     */
    private void stopAllServices() {
        // Stop the Local DB sync service
        stopService(intentLocalDbService);

        // Stop the Firebase sync service
        stopService(intentFirebaseDbService);
    }

    private boolean isServiceRunning(String packageNameService) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (packageNameService.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNetworkEnable() {
        authUpdate();
    }

    @Override
    public void onNetworkDisable() {
        stopAllServices();
    }
}
