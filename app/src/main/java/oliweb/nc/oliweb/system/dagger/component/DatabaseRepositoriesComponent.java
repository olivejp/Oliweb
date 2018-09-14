package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.sync.DatabaseSyncListenerService;
import oliweb.nc.oliweb.system.dagger.module.DatabaseRepositoriesModule;
import oliweb.nc.oliweb.ui.activity.viewmodel.FavoriteActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyAnnoncesViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.ProfilViewModel;

@Component(modules = {DatabaseRepositoriesModule.class})
@Singleton
public interface DatabaseRepositoriesComponent {
    UserRepository getUserRepository();

    AnnonceRepository getAnnonceRepository();

    ChatRepository getChatRepository();

    MessageRepository getMessageRepository();

    CategorieRepository getCategorieRepository();

    AnnonceWithPhotosRepository getAnnonceWithPhotosRepository();

    AnnonceFullRepository getAnnonceFullRepository();

    PhotoRepository getPhotoRepository();

    void inject(DatabaseSyncListenerService databaseSyncListenerService);

    void inject(MyAnnoncesViewModel myAnnoncesViewModel);

    void inject(FavoriteActivityViewModel favoriteActivityViewModel);

    void inject(PostAnnonceActivityViewModel postAnnonceActivityViewModel);

    void inject(ProfilViewModel profilViewModel);

    void inject(MainActivityViewModel mainActivityViewModel);
}
