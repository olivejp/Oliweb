package oliweb.nc.oliweb;

import android.app.Application;

import com.evernote.android.job.JobManager;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.leakcanary.LeakCanary;

import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.service.job.SyncJob;
import oliweb.nc.oliweb.service.job.SyncJobCreator;
import oliweb.nc.oliweb.utility.Constants;


/**
 * Created by orlanth23 on 14/01/2018.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);

        // On attache le receiver à notre application
        registerReceiver(NetworkReceiver.getInstance(), NetworkReceiver.CONNECTIVITY_CHANGE_INTENT_FILTER);

        JobManager.create(this).addJobCreator(new SyncJobCreator());

        // Plannification d'un job
        SyncJob.scheduleJob();

        // Lancement d'une synchro dès le début du programme
        SyncJob.launchImmediateJob();

        // Active la persistence des données pour Firebase database
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_MESSAGES_REF).keepSynced(true);
        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_CHATS_REF).keepSynced(true);
        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_USER_REF).keepSynced(true);
    }
}
