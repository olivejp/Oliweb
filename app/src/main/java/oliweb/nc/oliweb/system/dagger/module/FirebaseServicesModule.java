package oliweb.nc.oliweb.system.dagger.module;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;
import oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.ChatFirebaseSender;
import oliweb.nc.oliweb.service.firebase.MessageFirebaseSender;
import oliweb.nc.oliweb.service.firebase.PhotoFirebaseSender;

@Module(includes = {ContextModule.class, FirebaseRepositoriesModule.class})
public class FirebaseServicesModule {

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
    public FirebaseRetrieverService firebaseRetrieverService(FirebaseAnnonceRepository firebaseAnnonceRepository,
                                                             AnnonceRepository annonceRepository,
                                                             FirebasePhotoStorage photoStorage) {
        return new FirebaseRetrieverService(firebaseAnnonceRepository, annonceRepository, photoStorage);
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
