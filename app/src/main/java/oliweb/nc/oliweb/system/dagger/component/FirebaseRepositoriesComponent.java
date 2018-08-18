package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.system.dagger.module.FirebaseRepositoriesModule;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;

@Component(modules = {FirebaseRepositoriesModule.class})
@Singleton
public interface FirebaseRepositoriesComponent {
    FirebaseUserRepository getFirebaseUserRepository();

    FirebaseChatRepository getFirebaseChatRepository();

    FirebaseAnnonceRepository getFirebaseAnnonceRepository();

    FirebaseMessageRepository getFirebaseMessageRepository();
}
