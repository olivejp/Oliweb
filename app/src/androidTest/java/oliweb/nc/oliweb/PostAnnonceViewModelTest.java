package oliweb.nc.oliweb;

import android.arch.lifecycle.ViewModelProviders;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import oliweb.nc.oliweb.ui.activity.PostAnnonceActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;

import static oliweb.nc.oliweb.database.entity.StatusRemote.TO_SEND;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PostAnnonceViewModelTest {

    private PostAnnonceActivityViewModel viewModel;

    private static String PATH = "sdklfj";

    @Rule
    public ActivityTestRule<PostAnnonceActivity> mPostAnnonceActivity = new ActivityTestRule(PostAnnonceActivity.class);

    @Before
    public void init() {
        viewModel = ViewModelProviders.of(mPostAnnonceActivity.getActivity()).get(PostAnnonceActivityViewModel.class);
    }

    @Test
    public void insertAndCountAndUpdate() {
        viewModel.getLiveListPhoto().observeForever(photoEntities -> {
            Assert.assertNotNull(photoEntities);
            Assert.assertEquals(1, photoEntities.size());
            Assert.assertEquals(PATH, photoEntities.get(0).getUriLocal());
            Assert.assertEquals(TO_SEND, photoEntities.get(0).getStatut());
        });
        viewModel.addPhotoToCurrentList(PATH);
    }
}
