package oliweb.nc.oliweb.system.dagger.module;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;
import oliweb.nc.oliweb.service.sync.ScheduleSync;

@Module(includes = {ContextModule.class, FirebaseRepositoriesModule.class})
public class DatabaseRepositoriesModule {

    @Provides
    @Singleton
    public UserRepository userRepository(Context context, FirebaseUserRepository firebaseUserRepository) {
        return new UserRepository(context, firebaseUserRepository);
    }

    @Provides
    @Singleton
    public AnnonceRepository annonceRepository(Context context, PhotoRepository photoRepository, ChatRepository chatRepository) {
        return new AnnonceRepository(context, photoRepository, chatRepository);
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
                                     FirebaseMessageService firebaseMessageService,
                                     FirebaseChatService firebaseChatService) {
        return new ScheduleSync(firebaseUserRepository,
                userRepository, annonceRepository,
                annonceFirebaseSender, chatRepository,
                messageRepository, firebaseMessageService,
                firebaseChatService);
    }
}
