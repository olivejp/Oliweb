package oliweb.nc.oliweb.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.dagger.module.FirebaseRepositoriesModule;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseMessageRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;

@Component(modules = {FirebaseRepositoriesModule.class})
@Singleton
public interface FirebaseRepositoriesComponent {
    FirebaseUserRepository getFirebaseUserRepository();

    FirebaseChatRepository getFirebaseChatRepository();

    FirebaseAnnonceRepository getFirebaseAnnonceRepository();

    FirebaseMessageRepository getFirebaseMessageRepository();
}
