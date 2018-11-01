package oliweb.nc.oliweb;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;

import static oliweb.nc.oliweb.UtilityTest.checkCount;
import static oliweb.nc.oliweb.UtilityTest.initAnnonce;
import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AnnonceRepositoryTest {

    private static final String UID_USER = "123456";
    public static final String BOULOUP = "bouloup";
    public static final String DESCRIPTION = "nouvelle description";
    public static final String OLD_DESCRIPTION = "description";
    public static final String TITRE = "titre";
    public static final String UID_ANNONCE = "uidAnnonce";
    private AnnonceRepository annonceRepository;
    private List<CategorieEntity> listCategorie;
    private Context appContext;

    @Before
    public void init() {
        appContext = InstrumentationRegistry.getTargetContext();
        UtilityTest.cleanBase(appContext);

        ContextModule contextModule = new ContextModule(appContext);
        DatabaseRepositoriesComponent repositoriesComponent = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();

        annonceRepository = repositoriesComponent.getAnnonceRepository();
        CategorieRepository categorieRepository = repositoriesComponent.getCategorieRepository();
        TestObserver<List<CategorieEntity>> listTestObs = categorieRepository.getListCategorie().test();
        listCategorie = listTestObs.values().get(0);
    }

    @After
    public void reset(){
        UtilityTest.cleanBase(appContext);
    }

    private void deleteAllTest() {
        TestObserver<Integer> subscriber = new TestObserver<>();
        annonceRepository.deleteAll().subscribe(subscriber);
        waitTerminalEvent(subscriber, 5);
        subscriber.assertNoErrors();
        subscriber.dispose();
    }

    private AnnonceEntity saveSingleTest(AnnonceEntity annonceEntity) {
        TestObserver<AnnonceEntity> subscriberInsert = new TestObserver<>();
        annonceRepository.singleSave(annonceEntity).subscribe(subscriberInsert);
        waitTerminalEvent(subscriberInsert, 5);
        subscriberInsert.assertNoErrors();
        subscriberInsert.assertValueCount(1);
        AnnonceEntity annonceEntity1 = subscriberInsert.values().get(0);
        subscriberInsert.dispose();
        return annonceEntity1;
    }

    /**
     * Delete All users when the table is empty should not throw a exception
     */
    @Test
    public void deleteAllTwice() {
        deleteAllTest();
        deleteAllTest();
    }

    /**
     * Delete all annonces then count() should return 0
     */
    @Test
    public void deleteAllThenCount() {
        deleteAllTest();
        checkCount(0, annonceRepository.count());
    }

    /**
     * Save an annonce in the DB, then count() should return 1
     * Then delete all annonces, then count() should return 0
     */
    @Test
    public void insertAndDeleteAndCount() {
        deleteAllTest();

        AnnonceEntity annonceEntity = initAnnonce(UID_ANNONCE, UID_USER, StatusRemote.TO_SEND, TITRE, OLD_DESCRIPTION, listCategorie.get(0).getId());
        saveSingleTest(annonceEntity);

        checkCount(1, annonceRepository.count());

        deleteAllTest();
        checkCount(0, annonceRepository.count());
    }

    /**
     * Insert an annonce without Id
     * Check that the annonce returned had an Id
     * Count() should return 1
     * Update the annonce returned and save it
     * new Count() should also return 1
     */
    @Test
    public void insertAndCountAndUpdate() {
        deleteAllTest();

        AnnonceEntity annonceEntity = initAnnonce(UID_ANNONCE, UID_USER, StatusRemote.TO_SEND, TITRE, OLD_DESCRIPTION, listCategorie.get(0).getId());

        AnnonceEntity annonceEntityAfterInsert = saveSingleTest(annonceEntity);

        Long idAnnonceAfterInsert = annonceEntityAfterInsert.getId();
        assertNotNull(annonceEntityAfterInsert);
        assertNotNull(annonceEntityAfterInsert.getId());

        checkCount(1, annonceRepository.count());

        annonceEntityAfterInsert.setTitre(BOULOUP);
        annonceEntityAfterInsert.setDescription(DESCRIPTION);
        AnnonceEntity annonceEntityAfterUpdate = saveSingleTest(annonceEntityAfterInsert);

        checkCount(1, annonceRepository.count());

        assertEquals(idAnnonceAfterInsert, annonceEntityAfterUpdate.getId());
        assertEquals(BOULOUP, annonceEntityAfterUpdate.getTitre());
        assertEquals(DESCRIPTION, annonceEntityAfterUpdate.getDescription());
        assertEquals(UID_ANNONCE, annonceEntityAfterUpdate.getUid());
        assertEquals(UID_USER, annonceEntityAfterUpdate.getUidUser());
    }

    /**
     * Save an annonce
     */
    @Test
    public void saveAnnonce() {
        deleteAllTest();

        AnnonceEntity annonceEntity1 = initAnnonce(UID_ANNONCE, UID_USER, StatusRemote.TO_SEND, TITRE, DESCRIPTION, listCategorie.get(0).getId());

        TestObserver<AnnonceEntity> subscriberInsert = new TestObserver<>();
        annonceRepository.singleSave(annonceEntity1).subscribe(subscriberInsert);

        waitTerminalEvent(subscriberInsert, 5);
        subscriberInsert.assertNoErrors();
        subscriberInsert.assertValueCount(1); // Une seule liste
        subscriberInsert.assertValue(annonceEntity -> annonceEntity.getUid().equals(UID_ANNONCE));
        subscriberInsert.dispose();
    }
}
