package oliweb.nc.oliweb;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.CategorieRepository;
import oliweb.nc.oliweb.database.repository.UtilisateurRepository;

import static oliweb.nc.oliweb.UtilityTest.checkCount;
import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;
import static oliweb.nc.oliweb.UtilityTest.initAnnonce;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AnnonceRepositoryTest {

    private static final String UID_USER = "123456";
    private AnnonceRepository annonceRepository;
    private CategorieRepository categorieRepository;
    private UtilisateurRepository utilisateurRepository;
    private List<CategorieEntity> listCategorie;
    private List<UtilisateurEntity> listUtilisateur;

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        annonceRepository = AnnonceRepository.getInstance(appContext);
        categorieRepository = CategorieRepository.getInstance(appContext);
        utilisateurRepository = UtilisateurRepository.getInstance(appContext);
        initCategories();
        initUsers();
    }

    private void initCategories() {
        CategorieEntity categorieEntity = new CategorieEntity();
        categorieEntity.setName("cat1");
        categorieEntity.setCouleur("123456");

        CategorieEntity categorieEntity1 = new CategorieEntity();
        categorieEntity1.setName("cat2");
        categorieEntity1.setCouleur("123456");

        CategorieEntity categorieEntity2 = new CategorieEntity();
        categorieEntity2.setName("cat3");
        categorieEntity2.setCouleur("123456");

        CategorieEntity categorieEntity3 = new CategorieEntity();
        categorieEntity3.setName("cat4");
        categorieEntity3.setCouleur("123456");

        TestObserver<AtomicBoolean> subscriberInsertCategorie = new TestObserver<>();
        this.categorieRepository.insertSingle(categorieEntity, categorieEntity1, categorieEntity2, categorieEntity3).subscribe(subscriberInsertCategorie);
        waitTerminalEvent(subscriberInsertCategorie, 5);

        TestObserver<List<CategorieEntity>> subscriberGetListCategorie = new TestObserver<>();
        this.categorieRepository.getListCategorie().subscribe(subscriberGetListCategorie);
        waitTerminalEvent(subscriberInsertCategorie, 5);
        if (subscriberGetListCategorie.values() != null && !subscriberGetListCategorie.values().isEmpty()) {
            listCategorie = subscriberGetListCategorie.values().get(0);
        }
    }

    private void initUsers() {
        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setUuidUtilisateur(UID_USER);

        TestObserver<AtomicBoolean> subscriberInsertUtilisateur = new TestObserver<>();
        this.utilisateurRepository.insertSingle(utilisateurEntity).subscribe(subscriberInsertUtilisateur);
        waitTerminalEvent(subscriberInsertUtilisateur, 5);

        TestObserver<List<UtilisateurEntity>> subscriberGetUtilisateur = new TestObserver<>();
        this.utilisateurRepository.getAll().subscribe(subscriberGetUtilisateur);
        waitTerminalEvent(subscriberInsertUtilisateur, 5);
        if (subscriberGetUtilisateur.values() != null && !subscriberGetUtilisateur.values().isEmpty()) {
            listUtilisateur = subscriberGetUtilisateur.values().get(0);
        }
    }

    private void deleteAllTest() {
        TestObserver<Integer> subscriber = new TestObserver<>();
        annonceRepository.deleteAll().subscribe(subscriber);
        waitTerminalEvent(subscriber, 5);
        subscriber.assertNoErrors();
    }

    private void insertSingleTest() {
        AnnonceEntity annonceEntity = initAnnonce("orlanth23", UID_USER, StatusRemote.TO_SEND, "titre", "description", listCategorie.get(0).getIdCategorie());

        TestObserver<AtomicBoolean> subscriberInsert = new TestObserver<>();

        annonceRepository.insertSingle(annonceEntity).subscribe(subscriberInsert);
        waitTerminalEvent(subscriberInsert, 5);
        subscriberInsert.assertNoErrors();
        subscriberInsert.assertValueCount(1);
        subscriberInsert.assertValueAt(0, AtomicBoolean::get);
    }

    /**
     * Delete All users when the table is empty should not throw a exception
     */
    @Test
    public void deleteAllTwice() {
        deleteAllTest();
        deleteAllTest();
    }

    @Test
    public void deleteAllThenCount() {
        // Erase all the database
        deleteAllTest();
        checkCount(0, annonceRepository.count());
    }

    @Test
    public void insertAndCount() {
        // Erase all the database
        deleteAllTest();
        insertSingleTest();
        checkCount(1, annonceRepository.count());
    }
}
