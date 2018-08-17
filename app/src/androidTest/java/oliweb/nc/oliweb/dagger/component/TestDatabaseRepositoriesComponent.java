package oliweb.nc.oliweb.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.dagger.module.TestDatabaseRepositoriesModule;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.local.CategorieRepository;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.database.repository.local.UserRepository;

@Component(modules = {TestDatabaseRepositoriesModule.class})
@Singleton
public interface TestDatabaseRepositoriesComponent {
    UserRepository getUserRepository();

    AnnonceRepository getAnnonceRepository();

    ChatRepository getChatRepository();

    MessageRepository getMessageRepository();

    CategorieRepository getCategorieRepository();

    AnnonceWithPhotosRepository getAnnonceWithPhotosRepository();

    AnnonceFullRepository getAnnonceFullRepository();

    PhotoRepository getPhotoRepository();
}
