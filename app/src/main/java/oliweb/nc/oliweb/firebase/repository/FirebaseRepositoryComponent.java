package oliweb.nc.oliweb.firebase.repository;

import dagger.Component;

@Component(modules = {FirebaseRepositoryModule.class})
public interface FirebaseRepositoryComponent {
    FirebaseUserRepository getFirebaseUserRepository();
    FirebaseChatRepository getFirebaseChatRepository();
    FirebaseAnnonceRepository getFirebaseAnnonceRepository();
    FirebaseMessageRepository getFirebaseMessageRepository();
    FirebasePhotoRepository getFirebasePhotoRepository();
}
