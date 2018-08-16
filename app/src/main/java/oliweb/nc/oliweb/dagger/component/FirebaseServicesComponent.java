package oliweb.nc.oliweb.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.dagger.module.FirebaseServicesModule;
import oliweb.nc.oliweb.service.sync.FirebaseRetrieverService;
import oliweb.nc.oliweb.service.sync.ScheduleSync;
import oliweb.nc.oliweb.service.sync.deleter.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.sync.sender.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.ChatFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.MessageFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.PhotoFirebaseSender;

@Component(modules = {FirebaseServicesModule.class})
@Singleton
public interface FirebaseServicesComponent {

    AnnonceFirebaseDeleter getAnnonceFirebaseDeleter();

    FirebaseRetrieverService getFirebaseRetrieverService();

    PhotoFirebaseSender getPhotoFirebaseSender();

    ChatFirebaseSender getChatFirebaseSender();

    AnnonceFirebaseSender getAnnonceFirebaseSender();

    MessageFirebaseSender getMessageFirebaseSender();

    ScheduleSync getScheduleSync();
}
