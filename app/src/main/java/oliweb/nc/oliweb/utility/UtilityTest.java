package oliweb.nc.oliweb.utility;

import android.content.Context;
import android.support.annotation.NonNull;

import junit.framework.Assert;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;

public class UtilityTest {

    static final String UID_USER = "123456";
    static final String CATEGORIE_NAME = "CATEGORIE_NAME";
    public static final String DEFAULT_PROFILE = "orlanth23";
    public static final String DEFAULT_EMAIL = "orlanth23@hotmail.com";
    public static final String DEFAULT_COLOR = "123456";

    public static void waitTerminalEvent(TestObserver testObserver, int countDown) {
        if (!testObserver.awaitTerminalEvent(countDown, TimeUnit.SECONDS)) {
            Assert.assertTrue(false);
        }
    }

    public static void waitTerminalEvent(TestSubscriber testSubscriber, int countDown) {
        if (!testSubscriber.awaitTerminalEvent(countDown, TimeUnit.SECONDS)) {
            Assert.assertTrue(false);
        }
    }

    public static UserEntity initUtilisateur(@NonNull String uidUser, @NonNull String profile, @NonNull String email) {
        UserEntity userEntity = new UserEntity();
        userEntity.setProfile((profile.isEmpty()) ? DEFAULT_PROFILE : profile);
        userEntity.setUid((uidUser.isEmpty()) ? UID_USER : uidUser);
        userEntity.setEmail((email.isEmpty()) ? DEFAULT_EMAIL : email);
        return userEntity;
    }

    public static AnnonceEntity initAnnonce(String UUID, String uidUser, StatusRemote statusRemote, String titre, String description, Long idCategorie) {
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setUid(UUID);
        annonceEntity.setUidUser(uidUser);
        annonceEntity.setStatut(statusRemote);
        annonceEntity.setTitre(titre);
        annonceEntity.setDescription(description);
        annonceEntity.setIdCategorie(idCategorie);
        return annonceEntity;
    }

    public static void checkCount(Integer countExpected, Single<Integer> countSingle) {
        TestObserver<Integer> subscriberCount = new TestObserver<>();
        countSingle
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .subscribe(subscriberCount);
        waitTerminalEvent(subscriberCount, 5);
        subscriberCount.assertNoErrors();
        subscriberCount.assertValueAt(0, count -> count.equals(countExpected));
        subscriberCount.dispose();
    }

    public static void cleanBase(Context context) {
        ContextModule contextModule = new ContextModule(context);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();

        AnnonceRepository annonceRepository = component.getAnnonceRepository();
        CategorieRepository categorieRepository = component.getCategorieRepository();
        ChatRepository chatRepository = component.getChatRepository();
        MessageRepository messageRepository = component.getMessageRepository();
        UserRepository userRepository = component.getUserRepository();
        PhotoRepository photoRepository = component.getPhotoRepository();

        annonceRepository.deleteAll().subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe();
        categorieRepository.deleteAll().subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe();
        chatRepository.deleteAll().subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe();
        userRepository.deleteAll().subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe();
        messageRepository.deleteAll().subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe();
        photoRepository.deleteAll().subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe();

        initCategories(categorieRepository);
    }

    private static void initCategories(CategorieRepository categorieRepository) {
        CategorieEntity categorieEntity = new CategorieEntity();
        categorieEntity.setName(CATEGORIE_NAME);
        categorieEntity.setCouleur(DEFAULT_COLOR);

        TestObserver<CategorieEntity> subscriberInsertCategorie = new TestObserver<>();
        categorieRepository.singleSave(categorieEntity).subscribe(subscriberInsertCategorie);
        waitTerminalEvent(subscriberInsertCategorie, 5);

        subscriberInsertCategorie.dispose();
    }

    private static void initUsers(UserRepository userRepository) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUid(UID_USER);

        TestObserver<UserEntity> subscriberInsertUtilisateur = new TestObserver<>();
        userRepository.singleSave(userEntity).subscribe(subscriberInsertUtilisateur);
        waitTerminalEvent(subscriberInsertUtilisateur, 5);

        subscriberInsertUtilisateur.dispose();
    }
}
