package oliweb.nc.oliweb.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.dagger.module.FirebaseRepositoriesModule;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseMessageRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.service.sync.FirebaseRetrieverService;
import oliweb.nc.oliweb.service.sync.ScheduleSync;
import oliweb.nc.oliweb.service.sync.deleter.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.sync.sender.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.ChatFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.MessageFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.PhotoFirebaseSender;

@Component(modules = {FirebaseRepositoriesModule.class})
@Singleton
public interface FirebaseRepositoriesComponent {
    FirebaseUserRepository getFirebaseUserRepository();

    FirebaseChatRepository getFirebaseChatRepository();

    FirebaseAnnonceRepository getFirebaseAnnonceRepository();

    FirebaseMessageRepository getFirebaseMessageRepository();

    // TODO les cinq méthodes ci dessous devraient être sorties dans un autre component FirebaseServicesComponent
    AnnonceFirebaseDeleter getAnnonceFirebaseDeleter();

    FirebaseRetrieverService getFirebaseRetrieverService();

    PhotoFirebaseSender getPhotoFirebaseSender();

    ChatFirebaseSender getChatFirebaseSender();

    AnnonceFirebaseSender getAnnonceFirebaseSender();

    ScheduleSync getScheduleSync();

    MessageFirebaseSender getMessageFirebaseSender();
}
