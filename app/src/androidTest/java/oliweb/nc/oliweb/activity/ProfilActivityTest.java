package oliweb.nc.oliweb.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.activity.ProfilActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.ProfilViewModel;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static oliweb.nc.oliweb.UtilityTest.UID_USER;
import static oliweb.nc.oliweb.ui.activity.ProfilActivity.PROFIL_ACTIVITY_UID_USER;
import static oliweb.nc.oliweb.ui.activity.ProfilActivity.UPDATE;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ProfilActivityTest {

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

        viewModel = ViewModelProviders.of(profilActivity).get(ProfilViewModel.class);
    }

    @Test
    public void saveUserTest() {
        // Vérification que le profil de l'utilisateur apparaît correctement
        onView(withId(R.id.profil_email)).check(matches(isDisplayed()));
    }

    @After
    public void close() {
        profilActivityTestRule.finishActivity();
    }
}
