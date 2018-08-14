package oliweb.nc.oliweb.firebase.repository;

import dagger.Module;
import dagger.Provides;

@Module
public class FirebaseRepositoryModule {

    @Provides
    public static FirebaseUserRepository firebaseUserRepository() {
        return new FirebaseUserRepository();
    }
}
