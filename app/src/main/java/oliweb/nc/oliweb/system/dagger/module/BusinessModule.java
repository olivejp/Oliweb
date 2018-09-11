package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.ui.activity.business.MyChatsActivityBusiness;

@Module(includes = {FirebaseRepositoriesModule.class, DatabaseRepositoriesModule.class, SchedulerModule.class})
public class BusinessModule {

    @Provides
    @Singleton
    public MyChatsActivityBusiness myChatsActivityBusiness(FirebaseAnnonceRepository firebaseAnnonceRepository,
                                                           ChatRepository chatRepository,
                                                           @Named("processScheduler") Scheduler processScheduler,
                                                           @Named("androidScheduler") Scheduler androidScheduler) {
        return new MyChatsActivityBusiness(firebaseAnnonceRepository, chatRepository, processScheduler, androidScheduler);
    }
}
