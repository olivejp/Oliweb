package oliweb.nc.oliweb.system.broadcast.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;

@Module
public class FirebaseMessageRepositoryModule {

    @Provides
    @Singleton
    public FirebaseMessageRepository firebaseMessageRepository() {
        return new FirebaseMessageRepository();
    }
}
