package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;

@Module
public class FirebaseAnnonceRepositoryModule {

    @Provides
    @Singleton
    public FirebaseAnnonceRepository firebaseAnnonceRepository() {
        return new FirebaseAnnonceRepository();
    }
}
