package oliweb.nc.oliweb.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseMessageRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;

@Module(includes = {ContextModule.class})
public class FirebaseRepositoriesModule {

    @Provides
    @Singleton
    public FirebaseUserRepository firebaseUserRepository() {
        return new FirebaseUserRepository();
    }

    @Provides
    @Singleton
    public FirebaseChatRepository firebaseChatRepository() {
        return new FirebaseChatRepository();
    }

    @Provides
    @Singleton
    public FirebaseMessageRepository firebaseMessageRepository() {
        return new FirebaseMessageRepository();
    }

    @Provides
    @Singleton
    public FirebaseAnnonceRepository firebaseAnnonceRepository(AnnonceRepository annonceRepository, FirebasePhotoStorage firebasePhotoStorage) {
        return new FirebaseAnnonceRepository(annonceRepository, firebasePhotoStorage);
    }
}
