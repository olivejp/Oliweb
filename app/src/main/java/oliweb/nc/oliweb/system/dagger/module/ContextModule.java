package oliweb.nc.oliweb.system.dagger.module;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.system.dagger.ApplicationContext;

@Module
public class ContextModule {

    private Context context;

    public ContextModule(Context context) {
        this.context = context;
    }

    @Provides
    @Singleton
    Context providesApp() {
        return context;
    }

    @ApplicationContext
    @Provides
    @Singleton
    Context providesContext() {
        return context.getApplicationContext();
    }
}
