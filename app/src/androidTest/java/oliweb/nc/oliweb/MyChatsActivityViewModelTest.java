package oliweb.nc.oliweb;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.local.CategorieRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.ui.activity.MyChatsActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;

import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MyChatsActivityViewModelTest {

    private static final String UID_USER = "123456";
    private MyChatsActivityViewModel viewModel;
    private CategorieRepository categorieRepository;
    private UtilisateurRepository utilisateurRepository;
    private List<CategorieEntity> listCategorie;

    @Rule
    public ActivityTestRule<MyChatsActivity> activityTestRule = new ActivityTestRule<>(MyChatsActivity.class);

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        viewModel = ViewModelProviders.of(activityTestRule.getActivity()).get(MyChatsActivityViewModel.class);
        categorieRepository = CategorieRepository.getInstance(appContext);
        utilisateurRepository = UtilisateurRepository.getInstance(appContext);
        initCategories();
        initUsers();
    }

    private void initUsers() {
        TestObserver<Integer> testDeleteAll = utilisateurRepository.deleteAll().test();
        waitTerminalEvent(testDeleteAll, 5);

        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setUuidUtilisateur(UID_USER);

        TestObserver<UtilisateurEntity> subscriberInsertUtilisateur = new TestObserver<>();
        this.utilisateurRepository.saveWithSingle(utilisateurEntity).subscribe(subscriberInsertUtilisateur);
        waitTerminalEvent(subscriberInsertUtilisateur, 5);

        TestObserver<List<UtilisateurEntity>> subscriberGetUtilisateur = new TestObserver<>();
        this.utilisateurRepository.getAll().subscribe(subscriberGetUtilisateur);
        waitTerminalEvent(subscriberInsertUtilisateur, 5);
        if (subscriberGetUtilisateur.values() != null && !subscriberGetUtilisateur.values().isEmpty()) {
            List<UtilisateurEntity> listUtilisateur = subscriberGetUtilisateur.values().get(0);
        }
    }

    private void initCategories() {
        TestObserver<Integer> testDeleteAll = categorieRepository.deleteAll().test();
        waitTerminalEvent(testDeleteAll, 5);

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

    @Test
    public void findOrCreateChatTest() {
        // Create an annonceEntity
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setTitre("sdf");
        annonceEntity.setDescription("sdf");
        annonceEntity.setPrix(100);
        annonceEntity.setUuid("UID");
        annonceEntity.setIdCategorie(listCategorie.get(0).getIdCategorie());

        // FindOrCreateChat
        viewModel.findOrCreateChat("123", annonceEntity).observeForever(chatEntity -> {
            Assert.assertNotNull(chatEntity);
            Assert.assertEquals("UID", chatEntity.getUidAnnonce());
            Assert.assertEquals("123", chatEntity.getUidBuyer());
        });
    }
}
