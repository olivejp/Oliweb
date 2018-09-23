package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.service.sync.DatabaseSyncListenerService;
import oliweb.nc.oliweb.system.dagger.module.FirebaseRepositoriesModule;
import oliweb.nc.oliweb.system.dagger.module.SchedulerModule;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.ProfilViewModel;

@Component(modules = {FirebaseRepositoriesModule.class, SchedulerModule.class})
@Singleton
public interface FirebaseRepositoriesComponent {
    FirebaseUserRepository getFirebaseUserRepository();

    FirebaseChatRepository getFirebaseChatRepository();

    FirebaseAnnonceRepository getFirebaseAnnonceRepository();

    void inject(ProfilViewModel profilViewModel);

    void inject(MainActivityViewModel mainActivityViewModel);

    void inject(DatabaseSyncListenerService databaseSyncListenerService);
}
