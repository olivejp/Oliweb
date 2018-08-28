package oliweb.nc.oliweb.system.broadcast.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;

@Module
public class FirebaseChatRepositoryModule {

    @Provides
    @Singleton
    public FirebaseChatRepository firebaseChatRepository() {
        return new FirebaseChatRepository();
    }
}
