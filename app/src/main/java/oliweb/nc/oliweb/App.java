package oliweb.nc.oliweb;

import android.app.Application;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.leakcanary.LeakCanary;

import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.service.job.SyncJob;
import oliweb.nc.oliweb.service.sync.DatabaseSyncListenerService;
import oliweb.nc.oliweb.service.sync.FirebaseSyncListenerService;


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

        intentLocalDbService = new Intent(getApplicationContext(), DatabaseSyncListenerService.class);
        intentFirebaseDbService = new Intent(getApplicationContext(), FirebaseSyncListenerService.class);

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        // On attache le receiver à notre application
        registerReceiver(NetworkReceiver.getInstance(), NetworkReceiver.CONNECTIVITY_CHANGE_INTENT_FILTER);

        // TODO Réactiver la plannif du job
        // Plannification d'un job
        // JobManager.create(this).addJobCreator(new SyncJobCreator());
        // SyncJob.scheduleJob();

        // On va écouter le Broadcast Listener pour lancer le service de synchro uniquement dans le
        // cas où il y a du réseau.
        NetworkReceiver.getInstance().listen(this);

        // Implémentation de l'auth listener
        // Si l'utilisateur vient de se connecter il faut lancer les services, mais seulement s'il y a une connexion
        // On enregistre l'auth listener
        if (FirebaseAuth.getInstance() != null) {
            FirebaseAuth.getInstance().addAuthStateListener(firebaseAuth -> {
                mFirebaseUser = firebaseAuth.getCurrentUser();
                checkBeforeLaunchServices();
            });
        }

        // Active la persistence des données pour Firebase database
//        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
//        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_MESSAGES_REF).keepSynced(true);
//        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_CHATS_REF).keepSynced(true);
//        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_USER_REF).keepSynced(true);
    }

    /**
     * Vérification qu'il y a bien un utilisateur connecté et qu'l y a une connexion
     * avant de lancer les services de synchro.
     */
    private void checkBeforeLaunchServices() {
        if (mFirebaseUser != null) {
            if (NetworkReceiver.checkConnection(this)) {
                launchServices(mFirebaseUser.getUid());
            }
        } else {
            removeServices();
        }
    }

    /**
     * Lancement des services de synchronisation
     *
     * @param uidUser de l'utilisateur à connecter
     */
    private void launchServices(String uidUser) {
        // Lancement du service pour écouter Firebase
        intentFirebaseDbService.putExtra(FirebaseSyncListenerService.CHAT_SYNC_UID_USER, uidUser);
        startService(intentFirebaseDbService);

        // Lancement du service pour écouter la DB en local
        intentFirebaseDbService.putExtra(FirebaseSyncListenerService.CHAT_SYNC_UID_USER, uidUser);
        startService(intentLocalDbService);

        // Lancement d'une synchro uniquement s'il y a du réseau
        SyncJob.launchImmediateJob();
    }

    private void removeServices() {
        // Stop the Firebase sync service
        stopService(intentFirebaseDbService);

        // Stop the Local DB sync service
        stopService(intentLocalDbService);
    }

    @Override
    public void onNetworkEnable() {
        checkBeforeLaunchServices();
    }

    @Override
    public void onNetworkDisable() {
        removeServices();
    }
}
