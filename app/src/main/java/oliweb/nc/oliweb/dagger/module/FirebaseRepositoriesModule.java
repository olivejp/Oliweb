package oliweb.nc.oliweb.dagger.module;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseMessageRepository;
import oliweb.nc.oliweb.firebase.repository.FirebasePhotoRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.service.sync.FirebaseRetrieverService;
import oliweb.nc.oliweb.service.sync.deleter.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.sync.sender.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.ChatFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.MessageFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.PhotoFirebaseSender;

@Module(includes = ContextModule.class)
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
    public FirebaseAnnonceRepository firebaseAnnonceRepository() {
        return new FirebaseAnnonceRepository();
    }

    @Provides
    @Singleton
    public FirebasePhotoRepository firebasePhotoRepository() {
        return new FirebasePhotoRepository();
    }

    @Provides
    @Singleton
    public AnnonceFirebaseDeleter annonceFirebaseDeleter(Context context) {
        return new AnnonceFirebaseDeleter(context);
    }


    @Provides
    @Singleton
    public FirebaseRetrieverService firebaseRetrieverService() {
        return new FirebaseRetrieverService();
    }

    @Provides
    @Singleton
    public PhotoFirebaseSender photoFirebaseSender() {
        return new PhotoFirebaseSender();
    }

    @Provides
    @Singleton
    public ChatFirebaseSender chatFirebaseSender() {
        return new ChatFirebaseSender();
    }

    @Provides
    @Singleton
    public AnnonceFirebaseSender annonceFirebaseSender() {
        return new AnnonceFirebaseSender();
    }

    @Provides
    @Singleton
    public MessageFirebaseSender messageFirebaseSender() {
        return new MessageFirebaseSender();
    }
}
