package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.system.dagger.module.FirebaseServicesModule;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.service.sync.ScheduleSync;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.ChatFirebaseSender;
import oliweb.nc.oliweb.service.firebase.MessageFirebaseSender;
import oliweb.nc.oliweb.service.firebase.PhotoFirebaseSender;

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
