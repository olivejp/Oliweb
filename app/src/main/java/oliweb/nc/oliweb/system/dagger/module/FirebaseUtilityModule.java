package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;

@Module
public class FirebaseUtilityModule {
    @Provides
    @Singleton
    public FirebaseUtilityService firebaseUtilityService() {
        return new FirebaseUtilityService();
    }
}
