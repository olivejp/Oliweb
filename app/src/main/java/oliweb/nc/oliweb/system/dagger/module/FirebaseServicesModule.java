package oliweb.nc.oliweb.system.dagger.module;

import android.content.Context;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.reactivex.Scheduler;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;
import oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.service.firebase.PhotoFirebaseSender;
import oliweb.nc.oliweb.system.dagger.ApplicationContext;

@Module(includes = {ContextModule.class, SchedulerModule.class, FirebaseRepositoriesModule.class})
public class FirebaseServicesModule {

    @Provides
    @Singleton
    public AnnonceFirebaseDeleter annonceFirebaseDeleter(@ApplicationContext Context context,
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
                                                             FirebasePhotoStorage photoStorage,
                                                             @Named("processScheduler") Scheduler processScheduler,
                                                             @Named("androidScheduler") Scheduler androidScheduler) {
        return new FirebaseRetrieverService(firebaseAnnonceRepository, annonceRepository, photoStorage, processScheduler, androidScheduler);
    }

    @Provides
    @Singleton
    public PhotoFirebaseSender photoFirebaseSender(FirebasePhotoStorage storage, PhotoRepository photoRepository) {
        return new PhotoFirebaseSender(storage, photoRepository);
    }

    @Provides
    @Singleton
    public FirebaseChatService firebaseChatService(FirebaseChatRepository firebaseChatRepository,
                                                   ChatRepository chatRepository,
                                                   MessageRepository messageRepository,
                                                   FirebaseUserRepository firebaseUserRepository,
                                                   @Named("processScheduler") Scheduler processScheduler) {
        return new FirebaseChatService(firebaseChatRepository, chatRepository, messageRepository, firebaseUserRepository, processScheduler);
    }

    @Provides
    @Singleton
    public AnnonceFirebaseSender annonceFirebaseSender(FirebaseAnnonceRepository firebaseAnnonceRepository,
                                                       AnnonceRepository annonceRepository,
                                                       PhotoFirebaseSender photoFirebaseSender,
                                                       AnnonceFullRepository annonceFullRepository,
                                                       @Named("processScheduler") Scheduler processScheduler) {
        return new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, processScheduler);
    }

    @Provides
    @Singleton
    public FirebaseMessageService firebaseMessageService(FirebaseMessageRepository firebaseMessageRepository,
                                                         FirebaseChatRepository firebaseChatRepository,
                                                         MessageRepository messageRepository) {
        return new FirebaseMessageService(firebaseMessageRepository, firebaseChatRepository, messageRepository);
    }

    @Provides
    @Singleton
    public FirebasePhotoStorage firebasePhotoStorage(PhotoRepository photoRepository, @Named("processScheduler") Scheduler processScheduler) {
        return new FirebasePhotoStorage(photoRepository, processScheduler);
    }
}
