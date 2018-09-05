package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;

@Module(includes = FirebaseUtilityModule.class)
public class FirebaseAnnonceRepositoryModule {

    @Provides
    @Singleton
    public FirebaseAnnonceRepository firebaseAnnonceRepository(FirebaseUtilityService firebaseUtilityService) {
        return new FirebaseAnnonceRepository(firebaseUtilityService);
    }
}