package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.system.dagger.module.UtilityModule;
import oliweb.nc.oliweb.utility.MediaUtility;

@Component(modules = {UtilityModule.class})
@Singleton
public interface UtilityComponent {
    MediaUtility getMediaUtility();
}
