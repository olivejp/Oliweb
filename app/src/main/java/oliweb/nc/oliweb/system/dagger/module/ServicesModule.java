package oliweb.nc.oliweb.system.dagger.module;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.AnnonceService;
import oliweb.nc.oliweb.service.PhotoService;
import oliweb.nc.oliweb.service.UserService;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;
import oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage;
import oliweb.nc.oliweb.service.sync.ScheduleSync;

@Module(includes = {ContextModule.class, DatabaseRepositoriesModule.class, FirebaseServicesModule.class})
public class ServicesModule {

    @Provides
    @Singleton
    public AnnonceService annonceService(Context context,
                                         AnnonceRepository annonceRepository,
                                         AnnonceWithPhotosRepository annonceWithPhotosRepository,
                                         FirebasePhotoStorage firebasePhotoStorage,
                                         PhotoService photoService) {
        return new AnnonceService(context, annonceRepository, annonceWithPhotosRepository, firebasePhotoStorage, photoService);
    }

    @Provides
    @Singleton
    public PhotoService photoService(Context context, PhotoRepository photoRepository) {
        return new PhotoService(context, photoRepository);
    }

    @Provides
    @Singleton
    public UserService userService(UserRepository userRepository, FirebaseUserRepository firebaseUserRepository) {
        return new UserService(userRepository, firebaseUserRepository);
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
