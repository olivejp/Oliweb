package oliweb.nc.oliweb;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.UtilisateurRepository;

import static oliweb.nc.oliweb.UtilityTest.UID_USER;
import static oliweb.nc.oliweb.UtilityTest.checkCount;
import static oliweb.nc.oliweb.UtilityTest.initUtilisateur;
import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class UserRepositoryTest {

    private static final String UPDATED_PROFILE = "Updated Profile";
    private static final String EMAIL_UPDATED = "updated_orlanth23@hotmail.com";
    private UtilisateurRepository userRepository;

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        userRepository = UtilisateurRepository.getInstance(appContext);
    }

    private void deleteAll() {
        TestObserver<Integer> subscriber = new TestObserver<>();
        userRepository.deleteAll().subscribe(subscriber);
        waitTerminalEvent(subscriber, 5);
        subscriber.assertNoErrors();
    }

    private void insertUser(@Nullable String uidUser, @Nullable String profile, @Nullable String email) {
        UtilisateurEntity utilisateurEntity = initUtilisateur(uidUser, profile, email);
        TestObserver<AtomicBoolean> subscriberInsert = new TestObserver<>();
        userRepository.insertSingle(utilisateurEntity).subscribe(subscriberInsert);
        waitTerminalEvent(subscriberInsert, 5);
        subscriberInsert.assertNoErrors();
        subscriberInsert.assertValueCount(1);
        subscriberInsert.assertValueAt(0, AtomicBoolean::get);
    }

    private void existByUid(String uid, boolean expectedResult) {
        // existById should return a single value with a AtomicBoolean == true
        TestObserver<AtomicBoolean> subscriberExist = new TestObserver<>();
        userRepository.existByUid(uid).subscribe(subscriberExist);
        waitTerminalEvent(subscriberExist, 5);
        subscriberExist.assertNoErrors();
        subscriberExist.assertValueCount(1);
        subscriberExist.assertValueAt(0, atomicBoolean -> atomicBoolean.get() == expectedResult);
    }

    /**
     * Delete All users when the table is empty should not throw a exception
     */
    @Test
    public void deleteThenQuery() {
        deleteAll();
        existByUid(UID_USER, false);
    }


    /**
     * Delete All users when the table is empty should not throw a exception
     */
    @Test
    public void deleteAllTwice() {
        deleteAll();
        deleteAll();
    }

    /**
     * Clean Utilisateur table
     * then insert a user
     * then ask to see if the user exist
     */
    @Test
    public void insertAndExist() {
        // Erase all the database
        deleteAll();

        insertUser(null, null, null);

        // existById should return a single value with a AtomicBoolean == true
        existByUid(UID_USER, true);
    }

    @Test
    public void deleteAllThenCount() {
        // Erase all the database
        deleteAll();

        insertUser(null, null, null);

        checkCount(1, userRepository.count());

        deleteAll();

        checkCount(0, userRepository.count());
    }

    @Test
    public void insertThenCountThenQuery() {
        // Erase all the database
        deleteAll();

        // Insert a new user
        insertUser(null, null, null);

        // Count
        checkCount(1, userRepository.count());

        // Query
        TestObserver<UtilisateurEntity> subscriberFindByUid = new TestObserver<>();
        userRepository.findSingleByUid(UID_USER).subscribe(subscriberFindByUid);
        waitTerminalEvent(subscriberFindByUid, 5);
        subscriberFindByUid.assertNoErrors();
        subscriberFindByUid.assertValueAt(0, utilisateurEntity -> Objects.equals(utilisateurEntity.getUuidUtilisateur(), UID_USER));
    }

    @Test
    public void insertThenUpdateThenQuery() {

        String profileUpdated = "Updated Profile";
        String emailUpdated = "updated_orlanth23@hotmail.com";

        // Erase all the database
        deleteAll();

        // Insert a new user
        insertUser(null, null, null);

        // Updated the user
        UtilisateurEntity utilisateurEntity = new UtilisateurEntity();
        utilisateurEntity.setUuidUtilisateur(UID_USER);
        utilisateurEntity.setProfile(profileUpdated);
        utilisateurEntity.setEmail(emailUpdated);

        // Try to send updated utilisateur to database
        TestObserver<AtomicBoolean> subscriberUpdate = new TestObserver<>();
        userRepository.updateSingle(utilisateurEntity).subscribe(subscriberUpdate);
        waitTerminalEvent(subscriberUpdate, 5);
        subscriberUpdate.assertNoErrors();
        subscriberUpdate.assertValueAt(0, AtomicBoolean::get);

        // Query the updated values
        TestObserver<UtilisateurEntity> subscriberFindByUidSaved = new TestObserver<>();
        userRepository.findSingleByUid(UID_USER).subscribe(subscriberFindByUidSaved);
        waitTerminalEvent(subscriberFindByUidSaved, 5);
        subscriberFindByUidSaved.assertNoErrors();
        subscriberFindByUidSaved.assertValueAt(0, utilisateurEntityUpdated -> {
            boolean sameUid = Objects.equals(utilisateurEntityUpdated.getUuidUtilisateur(), UID_USER);
            boolean updatedProfile = utilisateurEntityUpdated.getProfile().equals(profileUpdated);
            boolean updatedEmail = utilisateurEntityUpdated.getEmail().equals(emailUpdated);
            return sameUid && updatedProfile && updatedEmail;
        });
    }

    @Test
    public void saveThenUpdateThenQuery() {
        // Erase all the user table
        deleteAll();

        // Create a new user
        UtilisateurEntity utilisateurEntity = initUtilisateur(null, null, null);

        // Save (insert) the new user
        TestObserver<AtomicBoolean> subscriberSave = new TestObserver<>();
        userRepository.save(utilisateurEntity).subscribe(subscriberSave);
        waitTerminalEvent(subscriberSave, 5);
        subscriberSave.assertNoErrors();
        subscriberSave.assertValueAt(0, AtomicBoolean::get);

        checkCount(1, userRepository.count());

        // Updated the user
        UtilisateurEntity userUpdated = new UtilisateurEntity();
        userUpdated.setUuidUtilisateur(UID_USER);
        userUpdated.setProfile(UPDATED_PROFILE);
        userUpdated.setEmail(EMAIL_UPDATED);

        // Save (update) the updated user
        TestObserver<AtomicBoolean> subscriberSave1 = new TestObserver<>();
        userRepository.save(userUpdated).subscribe(subscriberSave1);
        waitTerminalEvent(subscriberSave1, 5);
        subscriberSave1.assertNoErrors();
        subscriberSave1.assertValueAt(0, AtomicBoolean::get);

        checkCount(1, userRepository.count());

        // Query the updated values
        TestObserver<UtilisateurEntity> subscriberFindByUidSaved = new TestObserver<>();
        userRepository.findSingleByUid(UID_USER).subscribe(subscriberFindByUidSaved);
        waitTerminalEvent(subscriberFindByUidSaved, 5);
        subscriberFindByUidSaved.assertNoErrors();
        subscriberFindByUidSaved.assertValueAt(0, utilisateurEntityUpdated -> {
            boolean sameUid = Objects.equals(utilisateurEntityUpdated.getUuidUtilisateur(), UID_USER);
            boolean updatedProfile = utilisateurEntityUpdated.getProfile().equals(UPDATED_PROFILE);
            boolean updatedEmail = utilisateurEntityUpdated.getEmail().equals(EMAIL_UPDATED);
            return sameUid && updatedProfile && updatedEmail;
        });
    }

    @Test
    public void testGetAll() {
        // Erase all the database
        deleteAll();

        checkCount(0, userRepository.count());

        // Insert two new users
        insertUser("UID_USER_1", "UTILISATEUR_1", "EMAIL1");
        insertUser("UID_USER_2", "UTILISATEUR_2", "EMAIL2");

        checkCount(2, userRepository.count());

        TestObserver<List<UtilisateurEntity>> subscriberGetAll = new TestObserver<>();
        userRepository.getAll().subscribe(subscriberGetAll);
        waitTerminalEvent(subscriberGetAll, 5);
        subscriberGetAll.assertNoErrors();
        List<UtilisateurEntity> listRetour = subscriberGetAll.values().get(0);
        Assert.assertEquals(2, listRetour.size());
        boolean isFirstTrue = false;
        boolean isSecondTrue = false;
        for (UtilisateurEntity user : listRetour) {
            if (user.getProfile().equals("UTILISATEUR_1") && user.getEmail().equals("EMAIL1") && user.getUuidUtilisateur().equals("UID_USER_1")) {
                isFirstTrue = true;
            }
            if (user.getProfile().equals("UTILISATEUR_2") && user.getEmail().equals("EMAIL2") && user.getUuidUtilisateur().equals("UID_USER_2")) {
                isSecondTrue = true;
            }
        }
        Assert.assertTrue(isFirstTrue && isSecondTrue);
    }
}
