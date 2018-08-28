package oliweb.nc.oliweb.system.broadcast.dagger.module;

import dagger.Module;

@Module(includes = {ContextModule.class,
        FirebaseAnnonceRepositoryModule.class,
        FirebaseChatRepositoryModule.class,
        FirebaseMessageRepositoryModule.class,
        FirebaseUserRepositoryModule.class})
public class FirebaseRepositoriesModule {
}
