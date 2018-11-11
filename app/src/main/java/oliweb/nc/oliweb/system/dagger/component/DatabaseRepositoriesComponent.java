package oliweb.nc.oliweb.system.dagger.component;

import javax.inject.Singleton;

import dagger.Component;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.sync.DatabaseSyncListenerService;
import oliweb.nc.oliweb.system.dagger.module.DatabaseRepositoriesModule;
import oliweb.nc.oliweb.system.dagger.module.UtilityModule;
import oliweb.nc.oliweb.ui.activity.viewmodel.AnnonceDetailActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.FavoriteActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyAnnoncesViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.ProfilViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel;
import oliweb.nc.oliweb.ui.activity.viewmodel.ShootingActivityViewModel;

@Component(modules = {DatabaseRepositoriesModule.class, UtilityModule.class})
@Singleton
public interface DatabaseRepositoriesComponent {
    UserRepository getUserRepository();

    AnnonceRepository getAnnonceRepository();

    ChatRepository getChatRepository();

    MessageRepository getMessageRepository();

    CategorieRepository getCategorieRepository();

    AnnonceFullRepository getAnnonceFullRepository();

    PhotoRepository getPhotoRepository();

    void inject(DatabaseSyncListenerService databaseSyncListenerService);

    void inject(SearchActivityViewModel searchActivityViewModel);

    void inject(MyAnnoncesViewModel myAnnoncesViewModel);

    void inject(FavoriteActivityViewModel favoriteActivityViewModel);

    void inject(PostAnnonceActivityViewModel postAnnonceActivityViewModel);

    void inject(ProfilViewModel profilViewModel);

    void inject(MainActivityViewModel mainActivityViewModel);

    void inject(MyChatsActivityViewModel myChatsActivityViewModel);

    void inject(AnnonceDetailActivityViewModel annonceDetailActivityViewModel);

    void inject(ShootingActivityViewModel shootingActivityViewModel);
}
