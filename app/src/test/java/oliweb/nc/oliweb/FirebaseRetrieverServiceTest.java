package oliweb.nc.oliweb;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Observable;
import io.reactivex.Single;
import oliweb.nc.oliweb.dto.elasticsearch.AnnonceDto;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirebaseRetrieverServiceTest {

    @Mock
    Context context;

    @Mock
    FirebaseAnnonceRepository firebaseAnnonceRepository;

    @Mock
    AnnonceRepository annonceRepository;

    @Mock
    FirebasePhotoStorage firebasePhotoStorage;

    @Before
    public void setUp() {
        AnnonceDto annonceDto1 = new AnnonceDto();
        annonceDto1.setUuid("UID1");
        annonceDto1.setTitre("Blabla");

        AnnonceDto annonceDto2 = new AnnonceDto();
        annonceDto2.setUuid("UID2");
        annonceDto2.setTitre("Flouflou");

        when(firebaseAnnonceRepository.observeAllAnnonceByUidUser(anyString())).thenReturn(Observable.just(annonceDto1, annonceDto2));

        when(annonceRepository.countByUidUserAndUidAnnonce(anyString(), argThat("UID1"::equals))).thenReturn(Single.just(0));

        when(annonceRepository.countByUidUserAndUidAnnonce(anyString(), argThat("UID2"::equals))).thenReturn(Single.just(1));
    }

    @Test
    public void firstTest() {
        FirebaseRetrieverService firebaseRetrieverService = new FirebaseRetrieverService(firebaseAnnonceRepository, annonceRepository, firebasePhotoStorage);

        firebaseRetrieverService.synchronize(context, "1");

        verify(annonceRepository, times(1)).singleSave(any());
    }
}