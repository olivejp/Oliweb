package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@Module
public class SchedulerModule {

    @Provides
    @Named("processScheduler")
    @Singleton
    public Scheduler getProcessScheduler() {
        return Schedulers.io();
    }

    @Provides
    @Named("androidScheduler")
    @Singleton
    public Scheduler getAndroidScheduler() {
        return AndroidSchedulers.mainThread();
    }
}
