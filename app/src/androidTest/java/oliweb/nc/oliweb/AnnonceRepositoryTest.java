package oliweb.nc.oliweb;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.CategorieRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;

import static oliweb.nc.oliweb.UtilityTest.checkCount;
import static oliweb.nc.oliweb.UtilityTest.initAnnonce;
import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;

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

        TestObserver<CategorieEntity> subscriberInsertCategorie = new TestObserver<>();
        this.categorieRepository.saveWithSingle(categorieEntity).subscribe(subscriberInsertCategorie);
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

        TestObserver<UtilisateurEntity> subscriberInsertUtilisateur = new TestObserver<>();
        this.utilisateurRepository.saveWithSingle(utilisateurEntity).subscribe(subscriberInsertUtilisateur);
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

    private AnnonceEntity saveSingleTest(AnnonceEntity annonceEntity) {
        TestObserver<AnnonceEntity> subscriberInsert = new TestObserver<>();
        annonceRepository.saveWithSingle(annonceEntity).subscribe(subscriberInsert);
        waitTerminalEvent(subscriberInsert, 5);
        subscriberInsert.assertNoErrors();
        subscriberInsert.assertValueCount(1);
        return subscriberInsert.values().get(0);
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
    public void insertAndDeleteAndCount() {
        // Erase all the database
        deleteAllTest();
        checkCount(0, annonceRepository.count());

        AnnonceEntity annonceEntity = initAnnonce("uidAnnonce", UID_USER, StatusRemote.TO_SEND, "titre", "description", listCategorie.get(0).getIdCategorie());
        saveSingleTest(annonceEntity);

        checkCount(1, annonceRepository.count());

        deleteAllTest();
        checkCount(0, annonceRepository.count());
    }

    @Test
    public void insertAndCountAndUpdate() {
        // Erase all the database
        deleteAllTest();

        checkCount(0, annonceRepository.count());

        AnnonceEntity annonceEntity = initAnnonce("uidAnnonce", UID_USER, StatusRemote.TO_SEND, "titre", "description", listCategorie.get(0).getIdCategorie());

        AnnonceEntity annonceEntityAfterInsert = saveSingleTest(annonceEntity);

        Long idAnnonceAfterInsert = annonceEntityAfterInsert.getIdAnnonce();
        Assert.assertNotNull(annonceEntityAfterInsert);
        Assert.assertNotNull(annonceEntityAfterInsert.getIdAnnonce());

        checkCount(1, annonceRepository.count());

        annonceEntityAfterInsert.setTitre("bouloup");
        annonceEntityAfterInsert.setDescription("nouvelle description");
        AnnonceEntity annonceEntityAfterUpdate = saveSingleTest(annonceEntityAfterInsert);

        checkCount(1, annonceRepository.count());

        Assert.assertEquals(idAnnonceAfterInsert, annonceEntityAfterUpdate.getIdAnnonce());
        Assert.assertEquals("bouloup", annonceEntityAfterUpdate.getTitre());
        Assert.assertEquals("nouvelle description", annonceEntityAfterUpdate.getDescription());
        Assert.assertEquals("uidAnnonce", annonceEntityAfterUpdate.getUUID());
        Assert.assertEquals(UID_USER, annonceEntityAfterUpdate.getUuidUtilisateur());
    }
}
