package oliweb.nc.oliweb.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;

@Module
public class FirebaseChatRepositoryModule {

    @Provides
    @Singleton
    public FirebaseChatRepository firebaseChatRepository() {
        return new FirebaseChatRepository();
    }
}
