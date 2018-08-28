package oliweb.nc.oliweb.system.broadcast.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;

@Module
public class FirebaseUserRepositoryModule {

    @Provides
    @Singleton
    public FirebaseUserRepository firebaseUserRepository() {
        return new FirebaseUserRepository();
    }
}
