package oliweb.nc.oliweb;

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
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.CategorieRepository;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.database.repository.local.UserRepository;

class UtilityTest {

    static final String UID_USER = "123456";
    static final String CATEGORIE_NAME = "CATEGORIE_NAME";

    static void waitTerminalEvent(TestObserver testObserver, int countDown) {
        if (!testObserver.awaitTerminalEvent(countDown, TimeUnit.SECONDS)) {
            Assert.assertTrue(false);
        }
    }

    static void waitTerminalEvent(TestSubscriber testSubscriber, int countDown) {
        if (!testSubscriber.awaitTerminalEvent(countDown, TimeUnit.SECONDS)) {
            Assert.assertTrue(false);
        }
    }

    static UtilisateurEntity initUtilisateur(@NonNull String uidUser, @NonNull String profile, @NonNull String email) {
        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setProfile((profile.isEmpty()) ? "orlanth23" : profile);
        utilisateurEntity.setUid((uidUser.isEmpty()) ? UID_USER : uidUser);
        utilisateurEntity.setEmail((email.isEmpty()) ? "orlanth23@hotmail.com" : email);
        return utilisateurEntity;
    }

    static AnnonceEntity initAnnonce(String UUID, String uidUser, StatusRemote statusRemote, String titre, String description, Long idCategorie) {
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setUid(UUID);
        annonceEntity.setUidUser(uidUser);
        annonceEntity.setStatut(statusRemote);
        annonceEntity.setTitre(titre);
        annonceEntity.setDescription(description);
        annonceEntity.setIdCategorie(idCategorie);
        return annonceEntity;
    }

    static void checkCount(Integer countExpected, Single<Integer> countSingle) {
        TestObserver<Integer> subscriberCount = new TestObserver<>();
        countSingle
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .subscribe(subscriberCount);
        waitTerminalEvent(subscriberCount, 5);
        subscriberCount.assertNoErrors();
        subscriberCount.assertValueAt(0, count -> count.equals(countExpected));
        subscriberCount.dispose();
    }

    static void cleanBase(Context context) {
        AnnonceRepository annonceRepository = AnnonceRepository.getInstance(context);
        CategorieRepository categorieRepository = CategorieRepository.getInstance(context);
        ChatRepository chatRepository = ChatRepository.getInstance(context);
        MessageRepository messageRepository = MessageRepository.getInstance(context);
        UserRepository userRepository = UserRepository.getInstance(context);
        PhotoRepository photoRepository = PhotoRepository.getInstance(context);

        TestObserver testAnnonce = annonceRepository.deleteAll().test();
        TestObserver testCategorie = categorieRepository.deleteAll().test();
        TestObserver testChat = chatRepository.deleteAll().test();
        TestObserver testUtilisateur = userRepository.deleteAll().test();
        TestObserver testMessage = messageRepository.deleteAll().test();
        TestObserver testPhoto = photoRepository.deleteAll().test();

        waitTerminalEvent(testAnnonce,5);
        waitTerminalEvent(testCategorie,5);
        waitTerminalEvent(testChat,5);
        waitTerminalEvent(testUtilisateur,5);
        waitTerminalEvent(testMessage,5);
        waitTerminalEvent(testPhoto,5);

        // Remplissage par défaut
        initCategories(categorieRepository);
        initUsers(userRepository);
    }

    private static void initCategories(CategorieRepository categorieRepository) {
        CategorieEntity categorieEntity = new CategorieEntity();
        categorieEntity.setName(CATEGORIE_NAME);
        categorieEntity.setCouleur("123456");

        TestObserver<CategorieEntity> subscriberInsertCategorie = new TestObserver<>();
        categorieRepository.singleSave(categorieEntity).subscribe(subscriberInsertCategorie);
        waitTerminalEvent(subscriberInsertCategorie, 5);

        subscriberInsertCategorie.dispose();
    }

    private static void initUsers(UserRepository userRepository) {
        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setUid(UID_USER);

        TestObserver<UtilisateurEntity> subscriberInsertUtilisateur = new TestObserver<>();
        userRepository.singleSave(utilisateurEntity).subscribe(subscriberInsertUtilisateur);
        waitTerminalEvent(subscriberInsertUtilisateur, 5);

        subscriberInsertUtilisateur.dispose();
    }
}
