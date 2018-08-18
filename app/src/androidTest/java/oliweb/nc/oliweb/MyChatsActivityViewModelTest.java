package oliweb.nc.oliweb;

import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.dto.elasticsearch.AnnonceDto;
import oliweb.nc.oliweb.dto.elasticsearch.CategorieDto;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MyChatsActivityViewModelTest {
    public static final String UUID = "sdf";
    public static final String TITRE_ANNONCE = "Titre annonce";
    public static final String DESCRIPTION_ANNONCE = "Description annonce";
    public static final int PRIX = 150444;
    private MyChatsActivityViewModel viewModel;

    @Rule
    public ActivityTestRule<AnnonceDetailActivity> myChatsActivity = new ActivityTestRule<>(AnnonceDetailActivity.class);

    @Rule
    public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    private LifecycleOwner mockLifecycle;

    private FirebaseAnnonceRepository mockFirebaseAnnonceRepository;

    @Mock
    private Observer<AnnonceDto> observer;

    private AnnonceRepository annonceRepository;

    private AnnonceDto getAnnonceDto() {
        CategorieDto categorieDto = new CategorieDto();
        categorieDto.setId(1);

        AnnonceDto annonceDto = new AnnonceDto();
        annonceDto.setCategorie(categorieDto);
        annonceDto.setUuid(UUID);
        annonceDto.setTitre(TITRE_ANNONCE);
        annonceDto.setDescription(DESCRIPTION_ANNONCE);
        annonceDto.setPrix(PRIX);
        return annonceDto;
    }

    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();

        ContextModule contextModule = new ContextModule(appContext);

        mockFirebaseAnnonceRepository = mock(FirebaseAnnonceRepository.class);

        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder()
                .contextModule(contextModule)
                .build();

        annonceRepository = component.getAnnonceRepository();

        viewModel = new MyChatsActivityViewModel(myChatsActivity.getActivity().getApplication());
        observer = mock(Observer.class);

        mockLifecycle = mock(LifecycleOwner.class);
        LifecycleRegistry lifecycleRegistry = new LifecycleRegistry(mockLifecycle);
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    }

    @Test
    public void saveAnnonceTest() {
        AnnonceDto annonceDto = getAnnonceDto();

        AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceDto);
        this.annonceRepository.insert(annonceEntity);

        when(mockFirebaseAnnonceRepository.findMaybeByUidAnnonce(argThat(UUID::equals))).thenReturn(Maybe.just(annonceDto));

        viewModel.findLiveFirebaseByUidAnnonce(UUID).observeOnce(observer);
        verify(observer).onChanged(annonceDto);
    }

    @After
    public void close() {
        myChatsActivity.finishActivity();
    }
}
