package oliweb.nc.oliweb.activity;

import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.activity.ProfilActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.ProfilViewModel;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
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
