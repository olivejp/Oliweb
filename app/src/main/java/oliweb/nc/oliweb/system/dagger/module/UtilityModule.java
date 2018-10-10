package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.utility.MediaUtility;

@Module()
public class UtilityModule {
    @Provides
    @Singleton
    public MediaUtility mediaUtility() {
        return new MediaUtility();
    }
}
