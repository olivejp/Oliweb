package oliweb.nc.oliweb;

import android.app.Application;

import com.evernote.android.job.JobManager;
import com.squareup.leakcanary.LeakCanary;

import oliweb.nc.oliweb.job.SyncJob;
import oliweb.nc.oliweb.job.SyncJobCreator;
import oliweb.nc.oliweb.network.NetworkReceiver;


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
    }
}
