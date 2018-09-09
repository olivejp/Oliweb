package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.service.AnnonceService;
import oliweb.nc.oliweb.service.PhotoService;
import oliweb.nc.oliweb.service.UserService;
import oliweb.nc.oliweb.service.sync.ScheduleSync;
import oliweb.nc.oliweb.system.dagger.module.SchedulerModule;
import oliweb.nc.oliweb.system.dagger.module.ServicesModule;

@Component(modules = {ServicesModule.class, SchedulerModule.class})
@Singleton
public interface ServicesComponent {
    AnnonceService getAnnonceService();

    PhotoService getPhotoService();

    UserService getUserService();

    ScheduleSync getScheduleSync();
}
