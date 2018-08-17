package oliweb.nc.oliweb.dagger.module;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.storage.FirebasePhotoStorage;

@Module
public class FirebaseAnnonceRepositoryModule {

    @Provides
    @Singleton
    public FirebaseAnnonceRepository firebaseAnnonceRepository(AnnonceRepository annonceRepository, FirebasePhotoStorage firebasePhotoStorage) {
        return new FirebaseAnnonceRepository(annonceRepository, firebasePhotoStorage);
    }
}
