package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;

@Module(includes = FirebaseUtilityModule.class)
public class FirebaseChatRepositoryModule {

    @Provides
    @Singleton
    public FirebaseChatRepository firebaseChatRepository(FirebaseUtilityService firebaseUtilityService) {
        return new FirebaseChatRepository(firebaseUtilityService);
    }
}