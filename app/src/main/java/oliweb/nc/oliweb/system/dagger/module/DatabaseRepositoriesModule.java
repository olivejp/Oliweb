package oliweb.nc.oliweb.system.dagger.module;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceWithPhotosRepository;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.system.dagger.ApplicationContext;

@Module(includes = {ContextModule.class, SchedulerModule.class})
public class DatabaseRepositoriesModule {

    @Provides
    @Singleton
    public UserRepository userRepository(@ApplicationContext Context context) {
        return new UserRepository(context);
    }

    @Provides
    @Singleton
    public AnnonceRepository annonceRepository(@ApplicationContext Context context, PhotoRepository photoRepository, ChatRepository chatRepository) {
        return new AnnonceRepository(context, photoRepository, chatRepository);
    }

    @Provides
    @Singleton
    public ChatRepository chatRepository(@ApplicationContext Context context) {
        return new ChatRepository(context);
    }

    @Provides
    @Singleton
    public MessageRepository messageRepository(@ApplicationContext Context context,
                                               ChatRepository chatRepository) {
        return new MessageRepository(context, chatRepository);
    }

    @Provides
    @Singleton
    public CategorieRepository categorieRepository(@ApplicationContext Context context) {
        return new CategorieRepository(context);
    }

    @Provides
    @Singleton
    public AnnonceWithPhotosRepository annonceWithPhotosRepository(@ApplicationContext Context context) {
        return new AnnonceWithPhotosRepository(context);
    }

    @Provides
    @Singleton
    public AnnonceFullRepository annonceFullRepository(@ApplicationContext Context context) {
        return new AnnonceFullRepository(context);
    }

    @Provides
    @Singleton
    public PhotoRepository photoRepository(@ApplicationContext Context context) {
        return new PhotoRepository(context);
    }
}
