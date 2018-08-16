package oliweb.nc.oliweb.dagger.module;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseMessageRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;
import oliweb.nc.oliweb.service.sync.FirebaseRetrieverService;
import oliweb.nc.oliweb.service.sync.deleter.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.sync.sender.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.ChatFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.MessageFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.PhotoFirebaseSender;

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

    @Provides
    @Singleton
    public AnnonceFirebaseDeleter annonceFirebaseDeleter(Context context,
                                                         FirebaseAnnonceRepository firebaseAnnonceRepository,
                                                         AnnonceRepository annonceRepository,
                                                         PhotoRepository photoRepository,
                                                         AnnonceFullRepository annonceFullRepository,
                                                         FirebasePhotoStorage firebasePhotoStorage) {
        return new AnnonceFirebaseDeleter(context, firebaseAnnonceRepository, annonceRepository, photoRepository, annonceFullRepository, firebasePhotoStorage);
    }

    @Provides
    @Singleton
    public FirebaseRetrieverService firebaseRetrieverService(FirebaseAnnonceRepository firebaseAnnonceRepository) {
        return new FirebaseRetrieverService(firebaseAnnonceRepository);
    }

    @Provides
    @Singleton
    public PhotoFirebaseSender photoFirebaseSender(FirebasePhotoStorage storage, PhotoRepository photoRepository) {
        return new PhotoFirebaseSender(storage, photoRepository);
    }

    @Provides
    @Singleton
    public ChatFirebaseSender chatFirebaseSender(FirebaseChatRepository firebaseChatRepository, ChatRepository chatRepository, MessageRepository messageRepository) {
        return new ChatFirebaseSender(firebaseChatRepository, chatRepository, messageRepository);
    }

    @Provides
    @Singleton
    public AnnonceFirebaseSender annonceFirebaseSender(FirebaseAnnonceRepository firebaseAnnonceRepository,
                                                       AnnonceRepository annonceRepository,
                                                       PhotoFirebaseSender photoFirebaseSender,
                                                       AnnonceFullRepository annonceFullRepository) {
        return new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository);
    }

    @Provides
    @Singleton
    public MessageFirebaseSender messageFirebaseSender(FirebaseMessageRepository firebaseMessageRepository, FirebaseChatRepository firebaseChatRepository, MessageRepository messageRepository) {
        return new MessageFirebaseSender(firebaseMessageRepository, firebaseChatRepository, messageRepository);
    }
}
