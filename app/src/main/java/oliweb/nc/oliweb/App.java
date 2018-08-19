package oliweb.nc.oliweb;

import android.app.Application;

import com.evernote.android.job.JobManager;
import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

import oliweb.nc.oliweb.service.job.SyncJob;
import oliweb.nc.oliweb.service.job.SyncJobCreator;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;


/**
 * Created by orlanth23 on 14/01/2018.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // TODO Supprimer la lib en prod
        Stetho.initializeWithDefaults(this);

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);

        // On attache le receiver à notre application
        registerReceiver(NetworkReceiver.getInstance(), NetworkReceiver.CONNECTIVITY_CHANGE_INTENT_FILTER);

        // Plannification d'un job
        JobManager.create(this).addJobCreator(new SyncJobCreator());
        SyncJob.scheduleJob();
    }
}
