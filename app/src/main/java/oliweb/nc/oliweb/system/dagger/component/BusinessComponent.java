package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.system.dagger.module.BusinessModule;
import oliweb.nc.oliweb.ui.activity.business.MyChatsActivityBusiness;

@Singleton
@Component(modules = {BusinessModule.class})
public interface BusinessComponent {
    MyChatsActivityBusiness getMyChatsActivityBusiness();
}
