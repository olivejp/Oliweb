package oliweb.nc.oliweb.system.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;

@Module(includes = {ContextModule.class})
public class FirebaseRepositoriesModule {

    @Provides
    @Singleton
    public FirebaseAnnonceRepository firebaseAnnonceRepository(FirebaseUtilityService firebaseUtilityService) {
        return new FirebaseAnnonceRepository(firebaseUtilityService);
    }

    @Provides
    @Singleton
    public FirebaseMessageRepository firebaseMessageRepository(FirebaseUtilityService firebaseUtilityService) {
        return new FirebaseMessageRepository(firebaseUtilityService);
    }

    @Provides
    @Singleton
    public FirebaseChatRepository firebaseChatRepository(FirebaseUtilityService firebaseUtilityService) {
        return new FirebaseChatRepository(firebaseUtilityService);
    }

    @Provides
    @Singleton
    public FirebaseUserRepository firebaseUserRepository() {
        return new FirebaseUserRepository();
    }

    @Provides
    @Singleton
    public FirebaseUtilityService firebaseUtilityService() {
        return new FirebaseUtilityService();
    }
}
