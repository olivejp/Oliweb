package oliweb.nc.oliweb.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.firebase.repository.FirebaseMessageRepository;

@Module
public class FirebaseMessageRepositoryModule {

    @Provides
    @Singleton
    public FirebaseMessageRepository firebaseMessageRepository() {
        return new FirebaseMessageRepository();
    }
}
