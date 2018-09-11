package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;

@Module(includes = FirebaseUtilityModule.class)
public class FirebaseMessageRepositoryModule {

    @Provides
    @Singleton
    public FirebaseMessageRepository firebaseMessageRepository(FirebaseUtilityService firebaseUtilityService) {
        return new FirebaseMessageRepository(firebaseUtilityService);
    }
}
