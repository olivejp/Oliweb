package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@Module
public class SchedulerModule {

    @Provides
    @Named("processScheduler")
    public Scheduler getProcessScheduler() {
        return Schedulers.io();
    }

    @Provides
    @Named("androidScheduler")
    public Scheduler getAndroidScheduler() {
        return AndroidSchedulers.mainThread();
    }
}
