package oliweb.nc.oliweb.system.dagger.module;

import dagger.Module;

@Module(includes = {ContextModule.class,
        FirebaseAnnonceRepositoryModule.class,
        FirebaseChatRepositoryModule.class,
        FirebaseMessageRepositoryModule.class,
        FirebaseUserRepositoryModule.class})
public class FirebaseRepositoriesModule {
}
