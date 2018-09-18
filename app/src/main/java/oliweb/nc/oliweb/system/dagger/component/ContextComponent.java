package oliweb.nc.oliweb.system.dagger.component;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;

@Singleton
@Component(modules = ContextModule.class)
interface ContextComponent {
    Context getContext();
}
