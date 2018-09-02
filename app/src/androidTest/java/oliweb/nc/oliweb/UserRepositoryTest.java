package oliweb.nc.oliweb;

import android.arch.lifecycle.Observer;
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
import org.mockito.Mock;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.UserService;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.ServicesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;

import static oliweb.nc.oliweb.UtilityTest.checkCount;
import static oliweb.nc.oliweb.UtilityTest.initUtilisateur;
import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    private static final String UPDATED_EMAIL = "updated_orlanth23@hotmail.com";
    private static final String USER_PHOTO_URL = "https://www.google.com/url?sa=i&rct=j&q=&esrc=s&source=images&cd=&cad=rja&uact=8&ved=2ahUKEwi8sfHXmuncAhWId94KHQ_uCz0QjRx6BAgBEAU&url=https%3A%2F%2Ffr.linkedin.com%2Fin%2Fjean-paul-olive-8619215b&psig=AOvVaw34Rki_yMeRmOYLDNZJzRmO&ust=1534221518501011";
    private static final String USER_TELEPHONE = "790723";
    private static final String TOKEN = "TOKEN";

    private FirebaseUser mockUser;
    private UserRepository userRepository;
    private UserService userService;

    @Mock
    private Observer<AtomicBoolean> observer;

    @Before
    public void init() {
        mockUser = mock(FirebaseUser.class);
        FirebaseUserRepository mockFirebaseUserRepository = mock(FirebaseUserRepository.class);

        Context appContext = InstrumentationRegistry.getTargetContext();

        ContextModule contextModule = new ContextModule(appContext);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();
        ServicesComponent servicesComponent = DaggerServicesComponent.builder().contextModule(contextModule).build();

        userService = servicesComponent.getUserService();
        userRepository = component.getUserRepository();

        UtilityTest.cleanBase(appContext);

        when(mockUser.getUid()).thenReturn(USER_UID);
        when(mockUser.getDisplayName()).thenReturn(USER_PROFILE);
        when(mockUser.getEmail()).thenReturn(USER_EMAIL);
        when(mockUser.getPhotoUrl()).thenReturn(Uri.parse(USER_PHOTO_URL));
        when(mockUser.getPhoneNumber()).thenReturn(USER_TELEPHONE);

        when(mockFirebaseUserRepository.getToken()).thenReturn(Single.just(TOKEN));
    }

    @After
    public void deleteDatabase() {
        test_deleteAll();
    }

    private void test_deleteAll() {
        TestObserver<Integer> subscriber = new TestObserver<>();
        userRepository.deleteAll().subscribe(subscriber);
        waitTerminalEvent(subscriber, 5);
        subscriber.assertNoErrors();
    }

    private void test_singleSave(@NonNull String uidUser, @NonNull String profile, @NonNull String email) {
        UserEntity userEntity = initUtilisateur(uidUser, profile, email);
        TestObserver<UserEntity> subscriberInsert = new TestObserver<>();
        userRepository.singleSave(userEntity).subscribe(subscriberInsert);
        waitTerminalEvent(subscriberInsert, 5);
        subscriberInsert.assertNoErrors();
        subscriberInsert.assertValueCount(1);
    }

    private void test_existByUid(String userUid, boolean expectedResult) {
        // existById should return a single value with a AtomicBoolean == true
        TestObserver<AtomicBoolean> subscriberExist = new TestObserver<>();
        userRepository.existByUid(userUid).subscribe(subscriberExist);
        waitTerminalEvent(subscriberExist, 5);
        subscriberExist.assertNoErrors();
        subscriberExist.assertValueCount(1);
        subscriberExist.assertValueAt(0, atomicBoolean -> atomicBoolean.get() == expectedResult);
    }

    @Test
    public void test_saveUserFromFirebase_insert() {
        test_deleteAll();

        userService.saveUserFromFirebase(mockUser).observeOnce(observer);
        verify(observer).onChanged(new AtomicBoolean(true));

        TestObserver<UserEntity> subscribeFind = new TestObserver<>();
        userRepository.findMaybeByUid(USER_UID).subscribe(subscribeFind);
        waitTerminalEvent(subscribeFind, 5);
        subscribeFind.assertNoErrors();
        subscribeFind.assertValueCount(1);
        subscribeFind.assertValueAt(0, user -> USER_UID.equals(user.getUid())
                && USER_PROFILE.equals(user.getProfile())
                && USER_EMAIL.equals(user.getEmail())
                && USER_PHOTO_URL.equals(user.getPhotoUrl())
                && USER_TELEPHONE.equals(user.getTelephone()));
    }

    @Test
    public void test_saveUserFromFirebase_update() {
        test_deleteAll();

        test_singleSave(USER_UID, USER_PROFILE, USER_EMAIL);

        userService.saveUserFromFirebase(mockUser).observeOnce(observer);
        verify(observer).onChanged(new AtomicBoolean(false));

        TestObserver<UserEntity> subscribeFind = new TestObserver<>();
        userRepository.findMaybeByUid(USER_UID).subscribe(subscribeFind);
        waitTerminalEvent(subscribeFind, 5);
        subscribeFind.assertNoErrors();
        subscribeFind.assertValueCount(1);
        subscribeFind.assertValueAt(0, user -> USER_UID.equals(user.getUid())
                && USER_PROFILE.equals(user.getProfile())
                && USER_EMAIL.equals(user.getEmail()));
    }

    @Test
    public void test_delete() {
        test_deleteAll();
        test_existByUid(USER_UID, false);
        checkCount(0, userRepository.count());
    }

    @Test
    public void test_delete_twice() {
        test_deleteAll();
        test_deleteAll();
    }

    @Test
    public void test_count() {
        test_deleteAll();

        test_singleSave(USER_UID, USER_PROFILE, USER_EMAIL);

        test_existByUid(USER_UID, true);

        checkCount(1, userRepository.count());
    }

    @Test
    public void test_delete_afterInsert() {
        test_deleteAll();

        test_singleSave(USER_UID, USER_PROFILE, USER_EMAIL);

        checkCount(1, userRepository.count());

        test_deleteAll();

        checkCount(0, userRepository.count());
    }

    @Test
    public void test_findSingleByUid() {
        test_deleteAll();

        test_singleSave(USER_UID, USER_PROFILE, USER_EMAIL);

        checkCount(1, userRepository.count());

        TestObserver<UserEntity> subscriberFindByUid = new TestObserver<>();
        userRepository.findMaybeByUid(USER_UID).subscribe(subscriberFindByUid);
        waitTerminalEvent(subscriberFindByUid, 5);
        subscriberFindByUid.assertNoErrors();
        subscriberFindByUid.assertValueAt(0, utilisateurEntity -> Objects.equals(utilisateurEntity.getEmail(), USER_EMAIL)
                && Objects.equals(utilisateurEntity.getProfile(), USER_PROFILE)
                && Objects.equals(utilisateurEntity.getUid(), USER_UID));
    }

    @Test
    public void test_singleSave_update() {
        test_deleteAll();

        // Create a new user
        test_singleSave(USER_UID, USER_PROFILE, USER_EMAIL);

        // Retrieve user
        TestObserver<UserEntity> subscribeFindSingle = new TestObserver<>();
        userRepository.findMaybeByUid(USER_UID).subscribe(subscribeFindSingle);
        waitTerminalEvent(subscribeFindSingle, 5);
        subscribeFindSingle.assertNoErrors();

        // Updated the user
        test_singleSave(USER_UID, UPDATED_PROFILE, UPDATED_EMAIL);

        // Check that we don't create a second user
        checkCount(1, userRepository.count());

        // Query the updated values
        TestObserver<UserEntity> subscriberFindByUidSaved = new TestObserver<>();
        userRepository.findMaybeByUid(USER_UID).subscribe(subscriberFindByUidSaved);
        waitTerminalEvent(subscriberFindByUidSaved, 5);
        subscriberFindByUidSaved.assertNoErrors();
        subscriberFindByUidSaved.assertValue(utilisateurEntityUpdated -> {
            boolean sameUid = Objects.equals(utilisateurEntityUpdated.getUid(), USER_UID);
            boolean updatedProfile = utilisateurEntityUpdated.getProfile().equals(UPDATED_PROFILE);
            boolean updatedEmail = utilisateurEntityUpdated.getEmail().equals(UPDATED_EMAIL);
            return sameUid && updatedProfile && updatedEmail;
        });
    }

    @Test
    public void test_getAll() {
        test_deleteAll();

        // Insert two new users
        test_singleSave("UID_USER_1", "UTILISATEUR_1", "EMAIL1");
        test_singleSave("UID_USER_2", "UTILISATEUR_2", "EMAIL2");

        checkCount(2, userRepository.count());

        TestObserver<List<UserEntity>> subscriberGetAll = new TestObserver<>();
        userRepository.getAll().subscribe(subscriberGetAll);
        waitTerminalEvent(subscriberGetAll, 5);
        subscriberGetAll.assertNoErrors();
        List<UserEntity> listRetour = subscriberGetAll.values().get(0);
        Assert.assertEquals(2, listRetour.size());
        boolean isFirstTrue = false;
        boolean isSecondTrue = false;
        for (UserEntity user : listRetour) {
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
