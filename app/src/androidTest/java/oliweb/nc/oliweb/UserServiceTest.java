package oliweb.nc.oliweb;

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
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

/**
 * Created by orlanth23 on 18/08/2018.
 */
@RunWith(AndroidJUnit4.class)
public class UserServiceTest {

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

    @Before
    public void init() {
        when(firebaseUserRepository.getToken()).thenReturn(Single.just("TOKEN"));
        when(userRepository.singleSave(any())).thenReturn(Single.just(new UserEntity()));
        TestScheduler testScheduler = new TestScheduler();
        userService = new UserService(userRepository, firebaseUserRepository, testScheduler, testScheduler);
    }

    @After
    public void cleanDatabase() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        UtilityTest.cleanBase(appContext);
        Mockito.reset(firebaseUser, firebaseUserRepository, userRepository);
    }

    @Test
    public void test_1() {
        when(firebaseUser.getUid()).thenReturn("bilbo");
        when(userRepository.findMaybeByUid(argThat("bilbo"::equals))).thenReturn(Maybe.empty());

        TestObserver<UserEntity> testObserver = new TestObserver<>();
        userService.saveSingleUserFromFirebase(firebaseUser).subscribe(testObserver);
        testObserver.assertNoErrors();
    }

    @Test
    public void test_2() {
        when(firebaseUser.getUid()).thenReturn("frodo");
        when(userRepository.findMaybeByUid(argThat("frodo"::equals))).thenReturn(Maybe.just(new UserEntity()));

        TestObserver<UserEntity> testObserver = new TestObserver<>();
        userService.saveSingleUserFromFirebase(firebaseUser).subscribe(testObserver);
        testObserver.assertNoErrors();
    }
}
