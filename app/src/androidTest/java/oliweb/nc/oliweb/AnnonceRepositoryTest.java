package oliweb.nc.oliweb;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class AnnonceRepositoryTest {

    private static final String UID_USER = "123456";
    private AnnonceRepository annonceRepository;

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        annonceRepository = AnnonceRepository.getInstance(appContext);
    }

    private void waitTerminalEvent(TestObserver testObserver, int countDown) {
        if (!testObserver.awaitTerminalEvent(countDown, TimeUnit.SECONDS)) {
            Assert.assertTrue(false);
        }
    }

    private AnnonceEntity initAnnonce() {
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setUUID("orlanth23");
        annonceEntity.setUuidUtilisateur(UID_USER);
        annonceEntity.setTitre("titre");
        annonceEntity.setDescription("description");
        return annonceEntity;
    }

    private void checkCount(Integer countExpected) {
        TestObserver<Integer> subscriberCount = new TestObserver<>();
        annonceRepository.count()
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .subscribe(subscriberCount);
        waitTerminalEvent(subscriberCount, 5);
        subscriberCount.assertNoErrors();
        subscriberCount.assertValueAt(0, count -> count.equals(countExpected));
    }

    private void deleteAll() {
        TestObserver<Integer> subscriber = new TestObserver<>();
        annonceRepository.deleteAll().subscribe(subscriber);
        waitTerminalEvent(subscriber, 5);
        subscriber.assertNoErrors();
    }

    private void insertAnnonce() {
        AnnonceEntity annonceEntity = initAnnonce();
        TestObserver<AtomicBoolean> subscriberInsert = new TestObserver<>();
        annonceRepository.insertSingle(annonceEntity).subscribe(subscriberInsert);
        waitTerminalEvent(subscriberInsert, 5);
        subscriberInsert.assertNoErrors();
        subscriberInsert.assertValueCount(1);
        subscriberInsert.assertValueAt(0, AtomicBoolean::get);
    }

    /**
     * Delete All users when the table is empty should not throw a exception
     */
    @Test
    public void deleteAllTwice() {
        deleteAll();
        deleteAll();
    }

    @Test
    public void deleteAllThenCount() {
        // Erase all the database
        deleteAll();
        checkCount(0);
    }

    @Test
    public void insertAndCount() {
        // Erase all the database
        deleteAll();

        insertAnnonce();

        checkCount(1);
    }
}
