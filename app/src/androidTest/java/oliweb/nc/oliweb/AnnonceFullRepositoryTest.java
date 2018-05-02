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
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.CategorieRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;
import static oliweb.nc.oliweb.database.entity.StatusRemote.FAILED_TO_SEND;
import static oliweb.nc.oliweb.database.entity.StatusRemote.SEND;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AnnonceFullRepositoryTest {

    private static final String UID_USER = "123456";
    private AnnonceRepository annonceRepository;
    private AnnonceFullRepository annonceFullRepository;
    private CategorieRepository categorieRepository;
    private UtilisateurRepository utilisateurRepository;
    private List<CategorieEntity> listCategorie;
    private List<UtilisateurEntity> listUtilisateur;

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        annonceRepository = AnnonceRepository.getInstance(appContext);
        annonceFullRepository = AnnonceFullRepository.getInstance(appContext);
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
        subscriberInsertCategorie.dispose();
        subscriberGetListCategorie.dispose();
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
        subscriberInsertUtilisateur.dispose();
        subscriberGetUtilisateur.dispose();
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
        annonceRepository.saveWithSingle(annonceEntity).subscribe(subscriberInsert);
        waitTerminalEvent(subscriberInsert, 5);
        subscriberInsert.assertNoErrors();
        subscriberInsert.assertValueCount(1);
        AnnonceEntity annonceEntity1 = subscriberInsert.values().get(0);
        subscriberInsert.dispose();
        return annonceEntity1;
    }

    @Test
    public void getAllAnnoncesByStatusTest() {

        // Clean database
        deleteAllTest();


        // Insert two annonces
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setTitre("Titre_1");
        annonceEntity.setUuidUtilisateur(UID_USER);
        annonceEntity.setIdCategorie(listCategorie.get(0).getIdCategorie());
        annonceEntity.setDescription("Description_1");
        annonceEntity.setStatut(FAILED_TO_SEND);
        annonceEntity.setUUID("UUID1");

        AnnonceEntity annonceEntity2 = new AnnonceEntity();
        annonceEntity2.setTitre("Titre_2");
        annonceEntity2.setUuidUtilisateur(UID_USER);
        annonceEntity2.setIdCategorie(listCategorie.get(0).getIdCategorie());
        annonceEntity2.setDescription("Description_2");
        annonceEntity2.setStatut(SEND);
        annonceEntity2.setUUID("UUID2");

        saveSingleTest(annonceEntity);
        saveSingleTest(annonceEntity2);


        // Call getAllAnnoncesByStatus
        TestObserver<List<AnnonceFull>> testObserver = new TestObserver<>();
        annonceFullRepository.getAllAnnoncesByStatus(Utility.allStatusToSend()).subscribe(testObserver);
        UtilityTest.waitTerminalEvent(testObserver, 5);
        testObserver.assertNoErrors();
        testObserver.assertValue(annonceFulls -> annonceFulls.size() == 2);
        List<AnnonceFull> listResult = testObserver.values().get(0);
        int indexOfFirst;
        int indexOfSecond;
        if (listResult.get(0).getAnnonce().getUUID().equals("UUID1")) {
            indexOfFirst = 0;
            indexOfSecond = 1;
        } else {
            indexOfFirst = 1;
            indexOfSecond = 0;
        }

        // Assertions
        AnnonceEntity annonceEntity1 = listResult.get(indexOfFirst).annonce;
        Assert.assertEquals("Titre_1", annonceEntity1.getTitre());
        Assert.assertEquals("Description_1", annonceEntity1.getDescription());

        AnnonceEntity annonceEntity3 = listResult.get(indexOfSecond).annonce;
        Assert.assertEquals("Titre_2", annonceEntity3.getTitre());
        Assert.assertEquals("Description_2", annonceEntity3.getDescription());
    }
}
