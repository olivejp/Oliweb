package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.system.dagger.module.BusinessModule;
import oliweb.nc.oliweb.ui.activity.business.MyChatsActivityBusiness;

@Component(modules = {BusinessModule.class})
@Singleton
public interface BusinessComponent {
    MyChatsActivityBusiness getMyChatsActivityBusiness();
}
