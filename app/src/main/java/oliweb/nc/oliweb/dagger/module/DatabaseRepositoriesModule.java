package oliweb.nc.oliweb.dagger.module;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.database.repository.local.CategorieRepository;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.database.repository.local.UserRepository;
import oliweb.nc.oliweb.service.sync.ScheduleSync;

@Module(includes = ContextModule.class)
public class DatabaseRepositoriesModule {

    @Provides
    @Singleton
    public UserRepository userRepository(Context context) {
        return new UserRepository(context);
    }

    @Provides
    @Singleton
    public AnnonceRepository annonceRepository(Context context) {
        return new AnnonceRepository(context);
    }

    @Provides
    @Singleton
    public ChatRepository chatRepository(Context context) {
        return new ChatRepository(context);
    }

    @Provides
    @Singleton
    public MessageRepository messageRepository(Context context) {
        return new MessageRepository(context);
    }

    @Provides
    @Singleton
    public CategorieRepository categorieRepository(Context context) {
        return new CategorieRepository(context);
    }

    @Provides
    @Singleton
    public AnnonceWithPhotosRepository annonceWithPhotosRepository(Context context) {
        return new AnnonceWithPhotosRepository(context);
    }

    @Provides
    @Singleton
    public AnnonceFullRepository annonceFullRepository(Context context) {
        return new AnnonceFullRepository(context);
    }

    @Provides
    @Singleton
    public PhotoRepository photoRepository(Context context) {
        return new PhotoRepository(context);
    }

    @Provides
    @Singleton
    public ScheduleSync scheduleSync() {
        return new ScheduleSync();
    }
}
