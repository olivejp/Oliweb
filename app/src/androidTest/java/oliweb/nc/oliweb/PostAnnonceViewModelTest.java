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
import oliweb.nc.oliweb.ui.activity.PostAnnonceActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;

import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;
import static oliweb.nc.oliweb.database.entity.StatusRemote.TO_SEND;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PostAnnonceViewModelTest {

    private PostAnnonceActivityViewModel viewModel;
    private static final String UID_USER = "123456";
    private static String PATH = "sdklfj";
    private CategorieRepository categorieRepository;
    private UtilisateurRepository utilisateurRepository;
    private List<CategorieEntity> listCategorie;
    private List<UtilisateurEntity> listUtilisateur;

    @Rule
    public ActivityTestRule<PostAnnonceActivity> mPostAnnonceActivity = new ActivityTestRule(PostAnnonceActivity.class);

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        viewModel = ViewModelProviders.of(mPostAnnonceActivity.getActivity()).get(PostAnnonceActivityViewModel.class);
        categorieRepository = CategorieRepository.getInstance(appContext);
        initCategories();
        initUsers();
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

    @Test
    public void getLiveListPhotoTest() {
        viewModel.getLiveListPhoto().observeForever(photoEntities -> {
            Assert.assertNotNull(photoEntities);
            Assert.assertEquals(1, photoEntities.size());
            Assert.assertEquals(PATH, photoEntities.get(0).getUriLocal());
            Assert.assertEquals(TO_SEND, photoEntities.get(0).getStatut());
        });
        viewModel.addPhotoToCurrentList(PATH);
    }

    @Test
    public void saveAnnonceTest() {
        TestObserver<AnnonceEntity> testObserver = new TestObserver<>();
        viewModel.saveAnnonce("titre", "des", 15, UID_USER, false, true, false, listCategorie.get(0).getIdCategorie()).subscribe(testObserver);
        waitTerminalEvent(testObserver, 5);
        testObserver.assertNoErrors();
        testObserver.assertValue(annonceEntity -> annonceEntity.getTitre().equals("titre") && annonceEntity.getDescription().equals("des"));
        AnnonceEntity annonceEntity = testObserver.values().get(0);
        annonceEntity.setDescription("df");
        viewModel.addPhotoToCurrentList(PATH);
    }
}
