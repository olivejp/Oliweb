package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;

@Module
public class FirebaseChatRepositoryModule {

    @Provides
    @Singleton
    public FirebaseChatRepository firebaseChatRepository(FirebaseMessageRepository fbMessageRepository,
                                                         FirebaseUserRepository fbUserRepository) {
        return new FirebaseChatRepository(fbMessageRepository, fbUserRepository);
    }
}
