package oliweb.nc.oliweb.viewModel;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.UtilityTest;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.ui.activity.PostAnnonceActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;

import static oliweb.nc.oliweb.UtilityTest.CATEGORIE_NAME;
import static oliweb.nc.oliweb.UtilityTest.UID_USER;
import static org.junit.Assert.assertEquals;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PostAnnonceViewModelTest {

    private Context appContext;
    private PostAnnonceActivityViewModel viewModel;

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Rule
    public ActivityTestRule<PostAnnonceActivity> postAnnonceActivity = new ActivityTestRule<>(PostAnnonceActivity.class);

    @Before
    public void init() {
        appContext = ApplicationProvider.getApplicationContext();
        viewModel = ViewModelProviders.of(postAnnonceActivity.getActivity()).get(PostAnnonceActivityViewModel.class);
        UtilityTest.cleanBase(appContext);
    }

    @Test
    public void saveAnnonceTest() {

        List<CategorieEntity> listCategorie =  new ArrayList<>();
        Observer<ArrayList<CategorieEntity>> observer = listCategorie::addAll;

        viewModel.getListCategorie().observeForever(observer);
        assertEquals(1, listCategorie.size());
        assertEquals(CATEGORIE_NAME, listCategorie.get(0).getName());

        viewModel.setCurrentCategorie(listCategorie.get(0));

        TestObserver<AnnonceEntity> testSaveAnnonce = viewModel.saveAnnonce(UID_USER, "titre", "description", 100, false, false, false, false).test();
        UtilityTest.waitTerminalEvent(testSaveAnnonce, 5);
        testSaveAnnonce.assertNoErrors();
        testSaveAnnonce.assertValueCount(1);
        List<AnnonceEntity> listAnnonce = testSaveAnnonce.values();
        assertEquals(UID_USER, listAnnonce.get(0).getUidUser());
    }

    @After
    public void close() {
        UtilityTest.cleanBase(appContext);
        postAnnonceActivity.finishActivity();
    }
}
