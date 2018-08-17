package oliweb.nc.oliweb.dagger.module;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.local.CategorieRepository;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.database.repository.local.UserRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;
import oliweb.nc.oliweb.service.sync.ScheduleSync;
import oliweb.nc.oliweb.service.sync.sender.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.ChatFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.MessageFirebaseSender;

@Module(includes = {ContextModule.class, TestFirebaseRepositoriesModule.class})
public class TestDatabaseRepositoriesModule {

    @Provides
    @Singleton
    public UserRepository userRepository(Context context, FirebaseUserRepository firebaseUserRepository) {
        return new UserRepository(context, firebaseUserRepository);
    }

    @Provides
    @Singleton
    public AnnonceRepository annonceRepository(Context context, PhotoRepository photoRepository, ChatRepository chatRepository, FirebasePhotoStorage firebasePhotoStorage) {
        return new AnnonceRepository(context, photoRepository, chatRepository, firebasePhotoStorage);
    }

    @Provides
    @Singleton
    public ChatRepository chatRepository(Context context) {
        return new ChatRepository(context);
    }

    @Provides
    @Singleton
    public MessageRepository messageRepository(Context context, ChatRepository chatRepository) {
        return new MessageRepository(context, chatRepository);
    }

    @Provides
    @Singleton
    public CategorieRepository categorieRepository(Context context) {
        return new CategorieRepository(context);
    }

    @Provides
    @Singleton
    public AnnonceWithPhotosRepository annonceWithPhotosRepository(Context context) {
        return new AnnonceWithPhotosRepository(context);
    }

    @Provides
    @Singleton
    public AnnonceFullRepository annonceFullRepository(Context context) {
        return new AnnonceFullRepository(context);
    }

    @Provides
    @Singleton
    public PhotoRepository photoRepository(Context context) {
        return new PhotoRepository(context);
    }

    @Provides
    @Singleton
    public ScheduleSync scheduleSync(FirebaseUserRepository firebaseUserRepository,
                                     UserRepository userRepository,
                                     AnnonceRepository annonceRepository,
                                     AnnonceFirebaseSender annonceFirebaseSender,
                                     ChatRepository chatRepository,
                                     MessageRepository messageRepository,
                                     MessageFirebaseSender messageFirebaseSender,
                                     ChatFirebaseSender chatFirebaseSender) {
        return new ScheduleSync(firebaseUserRepository,
                userRepository, annonceRepository,
                annonceFirebaseSender, chatRepository,
                messageRepository, messageFirebaseSender,
                chatFirebaseSender);
    }
}
