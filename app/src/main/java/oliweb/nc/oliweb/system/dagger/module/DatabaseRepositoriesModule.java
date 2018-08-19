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

@Module(includes = {ContextModule.class})
public class DatabaseRepositoriesModule {

    @Provides
    @Singleton
    public UserRepository userRepository(Context context) {
        return new UserRepository(context);
    }

    @Provides
    @Singleton
    public AnnonceRepository annonceRepository(Context context, PhotoRepository photoRepository, ChatRepository chatRepository) {
        return new AnnonceRepository(context, photoRepository, chatRepository);
    }

    @Provides
    @Singleton
    public ChatRepository chatRepository(Context context) {
        return new ChatRepository(context);
    }

    @Provides
    @Singleton
    public MessageRepository messageRepository(Context context, ChatRepository chatRepository) {
        return new MessageRepository(context, chatRepository);
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
}
