package oliweb.nc.oliweb.viewModel;

import android.content.Context;
import android.content.Intent;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.lifecycle.ViewModelProviders;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.UtilityTest;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.ui.activity.ProfilActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.ProfilViewModel;

import static oliweb.nc.oliweb.UtilityTest.DEFAULT_PROFILE;
import static oliweb.nc.oliweb.UtilityTest.UID_USER;
import static oliweb.nc.oliweb.database.entity.StatusRemote.TO_SEND;
import static oliweb.nc.oliweb.ui.activity.ProfilActivity.PROFIL_ACTIVITY_UID_USER;
import static oliweb.nc.oliweb.ui.activity.ProfilActivity.UPDATE;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ProfilViewModelTest {

    private Context appContext;
    private ProfilViewModel viewModel;

    @Rule
    public ActivityTestRule<ProfilActivity> profilActivityTestRule;

    @Before
    public void init() {
        Intent intent = new Intent();
        intent.putExtra(PROFIL_ACTIVITY_UID_USER, UID_USER);
        intent.putExtra(UPDATE, false);

        profilActivityTestRule = new ActivityTestRule<>(ProfilActivity.class);
        ProfilActivity profilActivity = profilActivityTestRule.launchActivity(intent);

        appContext = ApplicationProvider.getApplicationContext();
        viewModel = ViewModelProviders.of(profilActivity).get(ProfilViewModel.class);
        UtilityTest.cleanBase(appContext);
    }

    @Test
    public void saveUserTest() {
        UserEntity userEntity = new UserEntity();
        userEntity.setIdUser(1L);
        userEntity.setUid(UID_USER);
        userEntity.setProfile(DEFAULT_PROFILE);

        TestObserver<UserEntity> testSaveUser = viewModel.markAsToSend(userEntity).test();
        UtilityTest.waitTerminalEvent(testSaveUser, 5);
        testSaveUser.assertNoErrors();
        testSaveUser.assertValueCount(1);
        testSaveUser.assertValue(userEntitySaved -> DEFAULT_PROFILE.equals(userEntitySaved.getProfile())
                && UID_USER.equals(userEntitySaved.getUid())
                && userEntitySaved.getId() == 1L
                && TO_SEND.equals(userEntitySaved.getStatut()));
    }

    @After
    public void close() {
        UtilityTest.cleanBase(appContext);
        profilActivityTestRule.finishActivity();
    }
}
