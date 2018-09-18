package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.service.firebase.PhotoFirebaseSender;
import oliweb.nc.oliweb.service.sync.DatabaseSyncListenerService;
import oliweb.nc.oliweb.service.sync.ScheduleSync;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.system.dagger.module.FirebaseServicesModule;
import oliweb.nc.oliweb.system.dagger.module.SchedulerModule;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyAnnoncesViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.ProfilViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;

@Component(modules = {FirebaseServicesModule.class, SchedulerModule.class})
@Singleton
public interface FirebaseServicesComponent {

    AnnonceFirebaseDeleter getAnnonceFirebaseDeleter();

    FirebaseRetrieverService getFirebaseRetrieverService();

    PhotoFirebaseSender getPhotoFirebaseSender();

    FirebaseChatService getFirebaseChatService();

    AnnonceFirebaseSender getAnnonceFirebaseSender();

    FirebaseMessageService getFirebaseMessageService();

    FirebaseUtilityService getFirebaseUtilityService();

    ScheduleSync getScheduleSync();

    void inject(DatabaseSyncListenerService databaseSyncListenerService);

    void inject(MyAnnoncesViewModel myAnnoncesViewModel);

    void inject(ProfilViewModel profilViewModel);

    void inject(SearchActivityViewModel searchActivityViewModel);

    void inject(MainActivityViewModel mainActivityViewModel);

    void inject(SyncService syncService);
}
