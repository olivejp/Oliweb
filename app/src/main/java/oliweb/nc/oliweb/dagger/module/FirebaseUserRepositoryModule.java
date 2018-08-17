package oliweb.nc.oliweb.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;

@Module
public class FirebaseUserRepositoryModule {

    @Provides
    @Singleton
    public FirebaseUserRepository firebaseUserRepository() {
        return new FirebaseUserRepository();
    }
}
