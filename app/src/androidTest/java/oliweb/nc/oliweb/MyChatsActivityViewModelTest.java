package oliweb.nc.oliweb;

import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MyChatsActivityViewModelTest {

    private static final String UID_USER = "123456";
    private static final String UID_ANNONCE = "annonce";
    private static final String TITRE_ANNONCE = "TITRE ANNONCE";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @InjectMocks
    private MyChatsActivityViewModel viewModel;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private Observer<ChatEntity> observer;

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        UtilityTest.cleanBase(appContext);

        when(chatRepository.findByUidUserAndUidAnnonce(argThat(UID_USER::equals), argThat(UID_ANNONCE::equals)))
                .thenReturn(Maybe.create(e -> {
                    ChatEntity chatEntity = new ChatEntity();
                    chatEntity.setTitreAnnonce(TITRE_ANNONCE);
                    chatEntity.setUidAnnonce(UID_ANNONCE);
                    chatEntity.setUidSeller(UID_USER);
                    e.onSuccess(chatEntity);
                }));
    }

    @Test
    public void test() {
        viewModel.findOrCreateLiveNewChat().observeOnce(observer);
    }
}
