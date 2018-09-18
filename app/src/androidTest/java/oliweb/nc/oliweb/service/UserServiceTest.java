package oliweb.nc.oliweb.service;

import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.firebase.auth.FirebaseUser;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import oliweb.nc.oliweb.UtilityTest;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

/**
 * Created by orlanth23 on 18/08/2018.
 */
@RunWith(AndroidJUnit4.class)
public class UserServiceTest {

    public static final String BILBO = "bilbo";
    public static final String FRODO = "frodo";
    public static final String TOKEN = "TOKEN";
    @Rule
    public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private FirebaseUserRepository firebaseUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FirebaseUser firebaseUser;

    private UserService userService;

    private TestScheduler testScheduler;

    private UserEntity createUser(String uid){
        UserEntity userEntity = new UserEntity();
        userEntity.setUid(uid);
        return userEntity;
    }

    @Before
    public void init() {
        when(firebaseUserRepository.getToken()).thenReturn(Single.just(TOKEN));
        testScheduler = new TestScheduler();
        userService = new UserService(userRepository, firebaseUserRepository, testScheduler, testScheduler);
    }

    @After
    public void cleanDatabase() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        UtilityTest.cleanBase(appContext);
        Mockito.reset(firebaseUser, firebaseUserRepository, userRepository);
    }

    @Test
    public void testCreation() {
        when(firebaseUser.getUid()).thenReturn(BILBO);
        when(userRepository.singleSave(any())).thenReturn(Single.just(createUser(BILBO)));
        when(userRepository.findMaybeByUid(argThat(BILBO::equals))).thenReturn(Maybe.empty());

        TestObserver<UserEntity> testObserver = new TestObserver<>();
        userService.saveSingleUserFromFirebase(firebaseUser).subscribe(testObserver);

        testScheduler.triggerActions();

        testObserver.assertNoErrors();
        testObserver.assertValueCount(1);
        testObserver.assertValue(userEntity -> userEntity.getUid().equals(BILBO));
    }

    @Test
    public void testUpdate() {
        when(firebaseUser.getUid()).thenReturn(FRODO);
        when(userRepository.singleSave(argThat(argument -> FRODO.equals(argument.getUid())))).thenReturn(Single.just(createUser(FRODO)));
        when(userRepository.findMaybeByUid(argThat(FRODO::equals))).thenReturn(Maybe.just(new UserEntity()));

        TestObserver<UserEntity> testObserver = new TestObserver<>();
        userService.saveSingleUserFromFirebase(firebaseUser).subscribe(testObserver);

        testScheduler.triggerActions();

        testObserver.assertNoErrors();
        testObserver.assertValueCount(1);
        testObserver.assertValue(userEntity -> userEntity.getUid().equals(FRODO));
    }
}
