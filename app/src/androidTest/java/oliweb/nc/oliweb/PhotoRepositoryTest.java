package oliweb.nc.oliweb;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;

import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PhotoRepositoryTest {

    private PhotoRepository photoRepository;


    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        photoRepository = PhotoRepository.getInstance(appContext);
    }

    /**
     * Delete All users when the table is empty should not throw a exception
     */
    @Test
    public void deleteByIdAnnonce() {
        TestObserver<Integer> testObserver = new TestObserver<>();
        photoRepository.deleteAllByIdAnnonce(0L).subscribe(testObserver);
        waitTerminalEvent(testObserver, 5);
        testObserver.assertNoErrors();
    }
}
