package oliweb.nc.oliweb;

import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.dto.elasticsearch.AnnonceDto;
import oliweb.nc.oliweb.dto.elasticsearch.CategorieDto;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.verify;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MyChatsActivityViewModelTest {

    private static final String UUID = "sdf";
    private static final String TITRE_ANNONCE = "Titre annonce";
    private static final String DESCRIPTION_ANNONCE = "Description annonce";
    private static final int PRIX = 150444;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public ActivityTestRule<AnnonceDetailActivity> myChatsActivity = new ActivityTestRule<>(AnnonceDetailActivity.class);

    @Rule
    public InstantTaskExecutorRule instantRule = new InstantTaskExecutorRule();

    @Mock
    private Observer<AnnonceDto> observer;

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

    @Test
    public void saveAnnonceTest() {
        Context appContext = InstrumentationRegistry.getTargetContext();
        ContextModule contextModule = new ContextModule(appContext);

        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder()
                .contextModule(contextModule)
                .build();

        AnnonceRepository annonceRepository = component.getAnnonceRepository();

        MyChatsActivityViewModel viewModel = new MyChatsActivityViewModel(myChatsActivity.getActivity().getApplication());

        AnnonceDto annonceDto = getAnnonceDto();
        annonceDto.setUuid(UUID);

        AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceDto);
        annonceRepository.insert(annonceEntity);

        viewModel.findLiveFirebaseByUidAnnonce(UUID).observeOnce(observer);

        verify(observer, after(1000).times(1)).onChanged(any());
    }

    @After
    public void close() {
        myChatsActivity.finishActivity();
    }
}
