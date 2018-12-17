package oliweb.nc.oliweb.activity;

import android.content.Intent;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.lifecycle.ViewModelProviders;
import androidx.test.espresso.PerformException;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.activity.AdvancedSearchActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.AdvancedSearchActivityViewModel;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.typeText;
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
public class AdvancedSearchActivityTest {

    private Long longValue = 999_999_999_999L;
    private AdvancedSearchActivityViewModel viewModel;

    @Rule
    public ActivityTestRule<AdvancedSearchActivity> advancedSearchActivityTestRule;

    @Before
    public void init() {
        Intent intent = new Intent();
        intent.putExtra(PROFIL_ACTIVITY_UID_USER, UID_USER);
        intent.putExtra(UPDATE, false);

        advancedSearchActivityTestRule = new ActivityTestRule<>(AdvancedSearchActivity.class);
        AdvancedSearchActivity advancedSearchActivity = advancedSearchActivityTestRule.launchActivity(intent);

        viewModel = ViewModelProviders.of(advancedSearchActivity).get(AdvancedSearchActivityViewModel.class);
    }

    @Test(expected = PerformException.class)
    public void testExceptionMinimum() {
        onView(withId(R.id.lower_price)).perform(typeText(String.valueOf(longValue)));
    }

    @After
    public void close() {
        advancedSearchActivityTestRule.finishActivity();
    }
}
