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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.UserService;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;

import static oliweb.nc.oliweb.UtilityTest.UID_USER;
import static oliweb.nc.oliweb.UtilityTest.checkCount;
import static oliweb.nc.oliweb.UtilityTest.initUtilisateur;
import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
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

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private UserRepository mockUserRepository;

    @Mock
    FirebaseUserRepository mockFirebaseUserRepository;

    private UserRepository userRepository;

    @Mock
    private FirebaseUser mockUser;

    private UserService userService;

    private TestScheduler testScheduler;

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();

        ContextModule contextModule = new ContextModule(appContext);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();
        userRepository = component.getUserRepository();

        testScheduler = new TestScheduler();

        UserEntity userEntity= new UserEntity();
        userEntity.setIdUser(1L);
        userEntity.setUid(UID_USER);
        userEntity.setEmail(USER_EMAIL);
        userEntity.setProfile(USER_PROFILE);
        userEntity.setPhotoUrl(USER_PHOTO_URL);
        userEntity.setTelephone(USER_TELEPHONE);

        when(mockUserRepository.findMaybeByUid(anyString())).thenReturn(Maybe.just(userEntity));
        when(mockUserRepository.singleSave(any())).then((Answer<Single<UserEntity>>) invocation -> userRepository.singleSave(userEntity));
        when(mockFirebaseUserRepository.getToken()).thenReturn(Single.just(TOKEN));

        userService = new UserService(mockUserRepository, mockFirebaseUserRepository, testScheduler, testScheduler);

        UtilityTest.cleanBase(appContext);

        when(mockUser.getUid()).thenReturn(USER_UID);
        when(mockUser.getDisplayName()).thenReturn(USER_PROFILE);
        when(mockUser.getEmail()).thenReturn(USER_EMAIL);
        when(mockUser.getPhotoUrl()).thenReturn(Uri.parse(USER_PHOTO_URL));
        when(mockUser.getPhoneNumber()).thenReturn(USER_TELEPHONE);
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

    private void test_singleSave(@NonNull Long id, @NonNull String uidUser, @NonNull String profile, @NonNull String email) {
        UserEntity userEntity = initUtilisateur(id, uidUser, profile, email);
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

        TestObserver<UserEntity> singleObserverUserEntity = new TestObserver<>();

        userService.saveSingleUserFromFirebase(mockUser).subscribe(singleObserverUserEntity);
        singleObserverUserEntity.assertNoErrors();

        testScheduler.triggerActions();

        verify(mockUserRepository, after(1000).atLeastOnce()).singleSave(any());

        checkCount(1, userRepository.count());

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

        TestObserver<UserEntity> testObserver = new TestObserver<>();

        userService.saveSingleUserFromFirebase(mockUser).subscribe(testObserver);
        testObserver.assertNoErrors();

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
        subscriberFindByUid.assertValueCount(1);
        subscriberFindByUid.assertValueAt(0, utilisateurEntity -> Objects.equals(utilisateurEntity.getEmail(), USER_EMAIL)
                && Objects.equals(utilisateurEntity.getProfile(), USER_PROFILE)
                && Objects.equals(utilisateurEntity.getUid(), USER_UID));
    }

    @Test
    public void test_singleSave_update() {
        test_deleteAll();

        // Create a new user
        test_singleSave(USER_UID, USER_PROFILE, USER_EMAIL);

        checkCount(1, userRepository.count());

        // Retrieve user
        TestObserver<UserEntity> subscribeFindSingle = new TestObserver<>();
        userRepository.findMaybeByUid(USER_UID).subscribe(subscribeFindSingle);
        waitTerminalEvent(subscribeFindSingle, 5);
        subscribeFindSingle.assertNoErrors();
        subscribeFindSingle.assertValueAt(0, userEntity -> USER_UID.equals(userEntity.getUid()));
        UserEntity userEntity = subscribeFindSingle.values().get(0);
        Long id = userEntity.getId();
        assertNotNull(id);

        // Updated the user
        test_singleSave(id, USER_UID, UPDATED_PROFILE, UPDATED_EMAIL);

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
        assertEquals(2, listRetour.size());
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
