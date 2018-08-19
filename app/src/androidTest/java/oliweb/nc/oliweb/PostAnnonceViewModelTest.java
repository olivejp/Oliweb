package oliweb.nc.oliweb;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.ui.activity.PostAnnonceActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;
import oliweb.nc.oliweb.utility.UtilityTest;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PostAnnonceViewModelTest {

    private PostAnnonceActivityViewModel viewModel;
    private static final String UID_USER = "12345";
    private static final String CATEGORIE_NAME = "CAT_NAME";

    @Rule
    public ActivityTestRule<PostAnnonceActivity> postAnnonceActivity = new ActivityTestRule<>(PostAnnonceActivity.class);

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        viewModel = ViewModelProviders.of(postAnnonceActivity.getActivity()).get(PostAnnonceActivityViewModel.class);
        UtilityTest.cleanBase(appContext);
    }

    @Test
    public void saveAnnonceTest() {
        TestObserver<List<CategorieEntity>> testListCategorie = viewModel.getListCategorie().test();
        UtilityTest.waitTerminalEvent(testListCategorie, 5);
        testListCategorie.assertNoErrors();
        testListCategorie.assertValueCount(1);
        testListCategorie.assertValue(categorieEntities -> categorieEntities.size() == 1 && categorieEntities.get(0).getName().equals(CATEGORIE_NAME));
        List<CategorieEntity> listCat = testListCategorie.values().get(0);

        viewModel.setCurrentCategorie(listCat.get(0));

        TestObserver<AnnonceEntity> testSaveAnnonce = viewModel.saveAnnonce("titre", "description", 100, UID_USER, false, false, false).test();
        UtilityTest.waitTerminalEvent(testSaveAnnonce, 5);
        testSaveAnnonce.assertNoErrors();
        testSaveAnnonce.assertValueCount(1);
        List<AnnonceEntity> listAnnonce = testSaveAnnonce.values();
        Assert.assertEquals(UID_USER, listAnnonce.get(0).getUidUser());
    }

    @After
    public void close() {
        postAnnonceActivity.finishActivity();
    }
}
