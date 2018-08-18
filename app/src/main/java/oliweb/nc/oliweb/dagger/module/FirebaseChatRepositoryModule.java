package oliweb.nc.oliweb.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseMessageRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;

@Module
public class FirebaseChatRepositoryModule {

    @Provides
    @Singleton
    public FirebaseChatRepository firebaseChatRepository(FirebaseMessageRepository fbMessageRepository,
                                                         FirebaseUserRepository fbUserRepository) {
        return new FirebaseChatRepository(fbMessageRepository, fbUserRepository);
    }
}
