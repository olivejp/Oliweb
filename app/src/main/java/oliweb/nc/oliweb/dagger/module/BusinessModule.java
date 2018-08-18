package oliweb.nc.oliweb.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.ui.activity.business.MyChatsActivityBusiness;

@Module(includes = {FirebaseRepositoriesModule.class, DatabaseRepositoriesModule.class})
public class BusinessModule {

    @Provides
    @Singleton
    public MyChatsActivityBusiness myChatsActivityBusiness(FirebaseAnnonceRepository firebaseAnnonceRepository, ChatRepository chatRepository) {
        return new MyChatsActivityBusiness(firebaseAnnonceRepository, chatRepository);
    }
}
