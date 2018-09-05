package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.service.firebase.PhotoFirebaseSender;
import oliweb.nc.oliweb.service.sync.ScheduleSync;
import oliweb.nc.oliweb.system.dagger.module.FirebaseServicesModule;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;

@Component(modules = {FirebaseServicesModule.class})
@Singleton
public interface FirebaseServicesComponent {

    AnnonceFirebaseDeleter getAnnonceFirebaseDeleter();

    FirebaseRetrieverService getFirebaseRetrieverService();

    PhotoFirebaseSender getPhotoFirebaseSender();

    FirebaseChatService getFirebaseChatService();

    AnnonceFirebaseSender getAnnonceFirebaseSender();

    FirebaseMessageService getFirebaseMessageService();

    FirebaseUtilityService getFirebaseUtilityService();

    ScheduleSync getScheduleSync();
}
