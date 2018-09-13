package oliweb.nc.oliweb;

import android.app.Application;

import com.evernote.android.job.JobManager;
import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

import oliweb.nc.oliweb.service.job.SyncJob;
import oliweb.nc.oliweb.service.job.SyncJobCreator;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.system.dagger.component.BusinessComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerBusinessComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.ServicesComponent;
import oliweb.nc.oliweb.system.dagger.module.BusinessModule;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.system.dagger.module.DatabaseRepositoriesModule;
import oliweb.nc.oliweb.system.dagger.module.FirebaseRepositoriesModule;
import oliweb.nc.oliweb.system.dagger.module.FirebaseServicesModule;
import oliweb.nc.oliweb.system.dagger.module.SchedulerModule;
import oliweb.nc.oliweb.system.dagger.module.ServicesModule;


/**
 * Created by orlanth23 on 14/01/2018.
 */

public class App extends Application {

    private FirebaseServicesComponent firebaseServicesComponent;
    private FirebaseRepositoriesComponent firebaseRepositoriesComponent;
    private DatabaseRepositoriesComponent databaseRepositoriesComponent;
    private ServicesComponent servicesComponent;
    private BusinessComponent businessComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        ContextModule contextModule = new ContextModule(this);
        SchedulerModule schedulerModule = new SchedulerModule();
        FirebaseRepositoriesModule firebaseRepositoriesModule = new FirebaseRepositoriesModule();
        FirebaseServicesModule firebaseServicesModule = new FirebaseServicesModule();
        DatabaseRepositoriesModule databaseRepositoriesModule = new DatabaseRepositoriesModule();
        ServicesModule servicesModule = new ServicesModule();
        BusinessModule businessModule = new BusinessModule();

        this.businessComponent = DaggerBusinessComponent.builder()
                .contextModule(contextModule)
                .databaseRepositoriesModule(databaseRepositoriesModule)
                .firebaseRepositoriesModule(firebaseRepositoriesModule)
                .schedulerModule(schedulerModule)
                .businessModule(businessModule)
                .build();

        this.firebaseServicesComponent = DaggerFirebaseServicesComponent.builder()
                .contextModule(contextModule)
                .schedulerModule(schedulerModule)
                .firebaseRepositoriesModule(firebaseRepositoriesModule)
                .firebaseServicesModule(firebaseServicesModule)
                .build();

        this.firebaseRepositoriesComponent = DaggerFirebaseRepositoriesComponent.builder()
                .contextModule(contextModule)
                .firebaseRepositoriesModule(firebaseRepositoriesModule)
                .build();

        this.servicesComponent = DaggerServicesComponent.builder().contextModule(contextModule)
                .schedulerModule(schedulerModule)
                .firebaseRepositoriesModule(firebaseRepositoriesModule)
                .firebaseServicesModule(firebaseServicesModule)
                .databaseRepositoriesModule(databaseRepositoriesModule)
                .servicesModule(servicesModule)
                .build();

        this.databaseRepositoriesComponent = DaggerDatabaseRepositoriesComponent.builder()
                .contextModule(contextModule)
                .databaseRepositoriesModule(databaseRepositoriesModule)
                .build();

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

    public FirebaseServicesComponent getFirebaseServicesComponent() {
        return firebaseServicesComponent;
    }

    public FirebaseRepositoriesComponent getFirebaseRepositoriesComponent() {
        return firebaseRepositoriesComponent;
    }

    public DatabaseRepositoriesComponent getDatabaseRepositoriesComponent() {
        return databaseRepositoriesComponent;
    }

    public ServicesComponent getServicesComponent() {
        return servicesComponent;
    }

    public BusinessComponent getBusinessComponent() {
        return businessComponent;
    }
}
