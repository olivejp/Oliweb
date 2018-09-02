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
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.CategorieRepository;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;

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
    private List<CategorieEntity> listCategorie;


    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        UtilityTest.cleanBase(appContext);

        ContextModule contextModule = new ContextModule(appContext);
        DatabaseRepositoriesComponent repositoriesComponent = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();

        annonceRepository = repositoriesComponent.getAnnonceRepository();
        CategorieRepository categorieRepository = repositoriesComponent.getCategorieRepository();
        TestObserver<List<CategorieEntity>> listTestObs = categorieRepository.getListCategorie().test();
        listCategorie = listTestObs.values().get(0);
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

        AnnonceEntity annonceEntity = initAnnonce("uidAnnonce", UID_USER, StatusRemote.TO_SEND, "titre", "description", listCategorie.get(0).getId());
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

        AnnonceEntity annonceEntity = initAnnonce("uidAnnonce", UID_USER, StatusRemote.TO_SEND, "titre", "description", listCategorie.get(0).getId());

        AnnonceEntity annonceEntityAfterInsert = saveSingleTest(annonceEntity);

        Long idAnnonceAfterInsert = annonceEntityAfterInsert.getId();
        Assert.assertNotNull(annonceEntityAfterInsert);
        Assert.assertNotNull(annonceEntityAfterInsert.getId());

        checkCount(1, annonceRepository.count());

        annonceEntityAfterInsert.setTitre("bouloup");
        annonceEntityAfterInsert.setDescription("nouvelle description");
        AnnonceEntity annonceEntityAfterUpdate = saveSingleTest(annonceEntityAfterInsert);

        checkCount(1, annonceRepository.count());

        Assert.assertEquals(idAnnonceAfterInsert, annonceEntityAfterUpdate.getId());
        Assert.assertEquals("bouloup", annonceEntityAfterUpdate.getTitre());
        Assert.assertEquals("nouvelle description", annonceEntityAfterUpdate.getDescription());
        Assert.assertEquals("uidAnnonce", annonceEntityAfterUpdate.getUid());
        Assert.assertEquals(UID_USER, annonceEntityAfterUpdate.getUidUser());
    }

    @Test
    public void saveMultipleAnnonce() {
        // Erase all the database
        deleteAllTest();

        AnnonceEntity annonceEntity1 = initAnnonce("uidAnnonce1", UID_USER, StatusRemote.TO_SEND, "titre1", "description1", listCategorie.get(0).getId());

        TestObserver<AnnonceEntity> subscriberInsert = new TestObserver<>();
        annonceRepository.singleSave(annonceEntity1).subscribe(subscriberInsert);

        waitTerminalEvent(subscriberInsert, 5);
        subscriberInsert.assertNoErrors();
        subscriberInsert.assertValueCount(1); // Une seule liste
        subscriberInsert.assertValue(annonceEntity -> annonceEntity.getUid().equals("uidAnnonce1"));
        subscriberInsert.dispose();
    }
}
