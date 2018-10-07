package oliweb.nc.oliweb.system.dagger.module;

import android.content.Context;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
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
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;
import oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage;
import oliweb.nc.oliweb.service.sync.ScheduleSync;
import oliweb.nc.oliweb.system.dagger.ApplicationContext;

@Module(includes = {ContextModule.class,
        SchedulerModule.class,
        DatabaseRepositoriesModule.class,
        FirebaseServicesModule.class})
public class ServicesModule {

    @Provides
    @Singleton
    public AnnonceService annonceService(@ApplicationContext Context context,
                                         AnnonceRepository annonceRepository,
                                         PhotoRepository photoRepository,
                                         AnnonceWithPhotosRepository annonceWithPhotosRepository,
                                         FirebasePhotoStorage firebasePhotoStorage,
                                         PhotoService photoService,
                                         UserService userService) {
        return new AnnonceService(context, annonceRepository, photoRepository, annonceWithPhotosRepository, firebasePhotoStorage, photoService, userService);
    }

    @Provides
    @Singleton
    public PhotoService photoService(Context context, PhotoRepository photoRepository) {
        return new PhotoService(context, photoRepository);
    }

    @Provides
    @Singleton
    public UserService userService(UserRepository userRepository,
                                   FirebaseUserRepository firebaseUserRepository,
                                   @Named("processScheduler") Scheduler processScheduler,
                                   @Named("androidScheduler") Scheduler androidScheduler) {
        return new UserService(userRepository, firebaseUserRepository, processScheduler, androidScheduler);
    }

    @Provides
    @Singleton
    public ScheduleSync scheduleSync(FirebaseUserRepository firebaseUserRepository,
                                     UserRepository userRepository,
                                     AnnonceRepository annonceRepository,
                                     AnnonceFirebaseSender annonceFirebaseSender,
                                     AnnonceFirebaseDeleter annonceFirebaseDeleter,
                                     ChatRepository chatRepository,
                                     PhotoRepository photoRepository,
                                     MessageRepository messageRepository,
                                     FirebaseMessageService firebaseMessageService,
                                     FirebaseChatService firebaseChatService,
                                     @Named("processScheduler") Scheduler processScheduler) {
        return new ScheduleSync(firebaseUserRepository,
                userRepository, annonceRepository,
                annonceFirebaseSender, annonceFirebaseDeleter, chatRepository, photoRepository,
                messageRepository, firebaseMessageService,
                firebaseChatService,
                processScheduler);
    }
}
