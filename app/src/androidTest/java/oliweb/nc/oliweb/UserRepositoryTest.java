package oliweb.nc.oliweb;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.firebase.auth.FirebaseUser;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.database.repository.local.UserRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;

import static oliweb.nc.oliweb.UtilityTest.checkCount;
import static oliweb.nc.oliweb.UtilityTest.initUtilisateur;
import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class UserRepositoryTest {

    private static final String USER_UID = "123456";
    private static final String USER_PROFILE = "OLIVE JP";
    private static final String USER_EMAIL = "orlanth23@hotmail.com";
    private static final String UPDATED_PROFILE = "Updated Profile";
    private static final String EMAIL_UPDATED = "updated_orlanth23@hotmail.com";

    private FirebaseUserRepository mockFirebaseUserRepository;
    private FirebaseUser mockUser;
    private UserRepository userRepository;

    @Before
    public void init() {
        mockUser = mock(FirebaseUser.class);
        mockFirebaseUserRepository = mock(FirebaseUserRepository.class);

        Context appContext = InstrumentationRegistry.getTargetContext();
        userRepository = UserRepository.getInstance(appContext);

        UtilityTest.cleanBase(appContext);

        when(mockUser.getUid()).thenReturn(USER_UID);
        when(mockUser.getDisplayName()).thenReturn("OLIVE JP");
        when(mockUser.getEmail()).thenReturn("orlanth23@gmail.com");
        when(mockUser.getPhotoUrl()).thenReturn(Uri.parse("https://www.google.com/url?sa=i&rct=j&q=&esrc=s&source=images&cd=&cad=rja&uact=8&ved=2ahUKEwi8sfHXmuncAhWId94KHQ_uCz0QjRx6BAgBEAU&url=https%3A%2F%2Ffr.linkedin.com%2Fin%2Fjean-paul-olive-8619215b&psig=AOvVaw34Rki_yMeRmOYLDNZJzRmO&ust=1534221518501011"));
        when(mockUser.getPhoneNumber()).thenReturn("790723");

        when(mockFirebaseUserRepository.getToken()).thenReturn(Single.create(emitter -> emitter.onSuccess("TOKEN")));
    }

    @After
    public void deleteDatabase(){
        deleteAll();
    }

    private void deleteAll() {
        TestObserver<Integer> subscriber = new TestObserver<>();
        userRepository.deleteAll().subscribe(subscriber);
        waitTerminalEvent(subscriber, 5);
        subscriber.assertNoErrors();
    }

    private UtilisateurEntity saveUser(@NonNull String uidUser, @NonNull String profile, @NonNull String email) {
        UtilisateurEntity utilisateurEntity = initUtilisateur(uidUser, profile, email);
        TestObserver<UtilisateurEntity> subscriberInsert = new TestObserver<>();
        userRepository.singleSave(utilisateurEntity).subscribe(subscriberInsert);
        waitTerminalEvent(subscriberInsert, 5);
        subscriberInsert.assertNoErrors();
        subscriberInsert.assertValueCount(1);
        return subscriberInsert.values().get(0);
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

    @Test
    public void insertUserFromFirebase() {
        deleteAll();
        TestObserver<AtomicBoolean> subscribeSave = new TestObserver<>();
        userRepository.saveUserFromFirebase(mockUser).subscribe(subscribeSave);
        waitTerminalEvent(subscribeSave, 5);
        subscribeSave.assertNoErrors();
        subscribeSave.assertValueCount(1);
        subscribeSave.assertValueAt(0, AtomicBoolean::get);
    }

    @Test
    public void updateUserFromFirebase() {
        deleteAll();
        saveUser(USER_UID, USER_PROFILE, USER_EMAIL);
        TestObserver<AtomicBoolean> subscribeSave = new TestObserver<>();
        userRepository.saveUserFromFirebase(mockUser).subscribe(subscribeSave);
        waitTerminalEvent(subscribeSave, 5);
        subscribeSave.assertNoErrors();
        subscribeSave.assertValueCount(1);
        subscribeSave.assertValueAt(0, AtomicBoolean::get);
    }

    /**
     * Delete All users when the table is empty should not throw a exception
     */
    @Test
    public void deleteThenQuery() {
        deleteAll();
        existByUid(USER_UID, false);
        checkCount(0, userRepository.count());
    }

    @Test
    public void deleteThenFind() {
        deleteAll();
        TestObserver<UtilisateurEntity> testObserver = new TestObserver<>();
        userRepository.findSingleByUid(USER_UID).subscribe(testObserver);
        waitTerminalEvent(testObserver, 5);
        testObserver.assertNoErrors();
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

        saveUser("123", "profile", "email");

        // existById should return a single value with a AtomicBoolean == true
        existByUid("123", true);

        checkCount(1, userRepository.count());
    }

    @Test
    public void deleteAllThenCount() {
        // Erase all the database
        deleteAll();

        saveUser("123", "profile", "email");

        checkCount(1, userRepository.count());

        deleteAll();

        checkCount(0, userRepository.count());
    }

    @Test
    public void insertThenCountThenQuery() {
        // Erase all the database
        deleteAll();

        // Insert a new user
        saveUser("123", "profile", "email");

        // Count
        checkCount(1, userRepository.count());

        // Query
        TestObserver<UtilisateurEntity> subscriberFindByUid = new TestObserver<>();
        userRepository.findSingleByUid("123").subscribe(subscriberFindByUid);
        waitTerminalEvent(subscriberFindByUid, 5);
        subscriberFindByUid.assertNoErrors();
        subscriberFindByUid.assertValueAt(0, utilisateurEntity -> Objects.equals(utilisateurEntity.getEmail(), "email") && Objects.equals(utilisateurEntity.getProfile(), "profile") && Objects.equals(utilisateurEntity.getUid(), "123"));
    }

    @Test
    public void insertThenUpdateThenQuery() {

        String profileUpdated = "Updated Profile";
        String emailUpdated = "updated_orlanth23@hotmail.com";

        // Erase all the database
        deleteAll();

        // Insert a new user
        UtilisateurEntity utilisateurEntity = saveUser("123", "profile", "email");

        // Updated the user
        utilisateurEntity.setProfile(profileUpdated);
        utilisateurEntity.setEmail(emailUpdated);

        // Try to send updated utilisateur to database
        TestObserver<UtilisateurEntity> subscriberUpdate = new TestObserver<>();
        userRepository.singleSave(utilisateurEntity).subscribe(subscriberUpdate);
        waitTerminalEvent(subscriberUpdate, 5);
        subscriberUpdate.assertNoErrors();
        subscriberUpdate.assertValueAt(0, entity -> entity.getUid().equals("123") && entity.getProfile().equals(profileUpdated) && entity.getEmail().equals(emailUpdated));

        // Query the updated values
        TestObserver<UtilisateurEntity> subscriberFindByUidSaved = new TestObserver<>();
        userRepository.findSingleByUid("123").subscribe(subscriberFindByUidSaved);
        waitTerminalEvent(subscriberFindByUidSaved, 5);
        subscriberFindByUidSaved.assertNoErrors();
        subscriberFindByUidSaved.assertValueAt(0, utilisateurEntityUpdated -> {
            boolean sameUid = Objects.equals(utilisateurEntityUpdated.getUid(), "123");
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
        UtilisateurEntity utilisateurEntity = initUtilisateur("123", "profile", "email");

        // Save (insert) the new user
        TestObserver<UtilisateurEntity> subscriberSave = new TestObserver<>();
        userRepository.singleSave(utilisateurEntity).subscribe(subscriberSave);
        waitTerminalEvent(subscriberSave, 5);
        subscriberSave.assertNoErrors();
        UtilisateurEntity userInserted = subscriberSave.values().get(0);

        checkCount(1, userRepository.count());

        // Updated the user
        userInserted.setProfile(UPDATED_PROFILE);
        userInserted.setEmail(EMAIL_UPDATED);

        // Save (update) the updated user
        TestObserver<UtilisateurEntity> subscriberSave1 = new TestObserver<>();
        userRepository.singleSave(userInserted).subscribe(subscriberSave1);
        waitTerminalEvent(subscriberSave1, 5);
        subscriberSave1.assertNoErrors();
        subscriberSave1.assertValueAt(0, entity -> entity.getProfile().equals(UPDATED_PROFILE) && entity.getEmail().equals(EMAIL_UPDATED));

        checkCount(1, userRepository.count());

        // Query the updated values
        TestObserver<UtilisateurEntity> subscriberFindByUidSaved = new TestObserver<>();
        userRepository.findSingleByUid("123").subscribe(subscriberFindByUidSaved);
        waitTerminalEvent(subscriberFindByUidSaved, 5);
        subscriberFindByUidSaved.assertNoErrors();
        subscriberFindByUidSaved.assertValue(utilisateurEntityUpdated -> {
            boolean sameUid = Objects.equals(utilisateurEntityUpdated.getUid(), "123");
            boolean updatedProfile = utilisateurEntityUpdated.getProfile().equals(UPDATED_PROFILE);
            boolean updatedEmail = utilisateurEntityUpdated.getEmail().equals(EMAIL_UPDATED);
            return sameUid && updatedProfile && updatedEmail;
        });
    }

    @Test
    public void getAllTest() {
        // Erase all the database
        deleteAll();

        checkCount(0, userRepository.count());

        // Insert two new users
        saveUser("UID_USER_1", "UTILISATEUR_1", "EMAIL1");
        saveUser("UID_USER_2", "UTILISATEUR_2", "EMAIL2");

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
            if (user.getProfile().equals("UTILISATEUR_1") && user.getEmail().equals("EMAIL1") && user.getUid().equals("UID_USER_1")) {
                isFirstTrue = true;
            }
            if (user.getProfile().equals("UTILISATEUR_2") && user.getEmail().equals("EMAIL2") && user.getUid().equals("UID_USER_2")) {
                isSecondTrue = true;
            }
        }
        Assert.assertTrue(isFirstTrue && isSecondTrue);
    }
}
