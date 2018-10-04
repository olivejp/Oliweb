package oliweb.nc.oliweb;

import android.arch.lifecycle.Observer;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.TestScheduler;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.dto.firebase.UserFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.service.firebase.FirebasePhotoStorage;
import oliweb.nc.oliweb.service.firebase.FirebaseRetrieverService;
import oliweb.nc.oliweb.utility.LiveDataOnce;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirebaseRetrieverServiceTest {

    public static final String UID_ANNONCE_1 = "UID1";
    public static final String UID_ANNONCE_2 = "UID2";
    public static final String UID_ANNONCE_3 = "UID3";
    public static final String UID_USER = "123";

    @Mock
    private Context context;

    @Mock
    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    @Mock
    private AnnonceRepository annonceRepository;

    @Mock
    private FirebasePhotoStorage firebasePhotoStorage;

    private TestScheduler testScheduler;

    @Before
    public void setUp() {

        testScheduler = new TestScheduler();

        // Utilisateur
        UserFirebase userDto = new UserFirebase();
        userDto.setUuid(UID_USER);
        userDto.setEmail("orlanth23@hotmail.com");
        userDto.setProfile("Profile");

        // AnnonceFirebase sans photo
        AnnonceFirebase annonceFirebase1 = new AnnonceFirebase();
        annonceFirebase1.setUuid(UID_ANNONCE_1);
        annonceFirebase1.setTitre("Blabla");
        annonceFirebase1.setUtilisateur(userDto);

        // AnnonceFirebase sans photo
        AnnonceFirebase annonceFirebase2 = new AnnonceFirebase();
        annonceFirebase2.setUuid(UID_ANNONCE_2);
        annonceFirebase2.setTitre("Flouflou");
        annonceFirebase2.setUtilisateur(userDto);

        // AnnonceFirebase avec photos
        AnnonceFirebase annonceFirebase3 = new AnnonceFirebase();
        annonceFirebase3.setUuid(UID_ANNONCE_3);
        annonceFirebase3.setTitre("Falala");
        annonceFirebase3.setUtilisateur(userDto);
        annonceFirebase3.setPhotos(Arrays.asList("url1", "url2", "url3"));

        // Annonce entity renvoyé après la sauvegarde de l'AnnonceDto3
        AnnonceEntity annonceEntity = AnnonceConverter.convertDtoToEntity(annonceFirebase3);
        annonceEntity.setUidUser(UID_USER);
        annonceEntity.setIdAnnonce(55L);

        // Firebase Annonce Repo nous renvoie trois AnnonceFirebase
        when(firebaseAnnonceRepository.observeAllAnnonceByUidUser(argThat(UID_USER::equals))).thenReturn(Observable.just(annonceFirebase1, annonceFirebase2, annonceFirebase3));

        // AnnonceDto1 n'existe pas encore en base
        when(annonceRepository.countByUidUserAndUidAnnonce(argThat(UID_USER::equals), argThat(UID_ANNONCE_1::equals))).thenReturn(Single.just(0));

        // AnnonceDto2 existe déjà en base
        when(annonceRepository.countByUidUserAndUidAnnonce(argThat(UID_USER::equals), argThat(UID_ANNONCE_2::equals))).thenReturn(Single.just(1));

        // AnnonceDt3 n'existe pas encore en base
        when(annonceRepository.countByUidUserAndUidAnnonce(argThat(UID_USER::equals), argThat(UID_ANNONCE_3::equals))).thenReturn(Single.just(0));

        // Retour d'annonceRepository après la sauvegarde du AnnonceDto3
        when(annonceRepository.singleSave(any())).thenReturn(Single.just(annonceEntity));
    }

    @Test
    public void ShouldSaveOnce() {
        // Création de mon service à tester
        FirebaseRetrieverService firebaseRetrieverService = new FirebaseRetrieverService(firebaseAnnonceRepository, annonceRepository, firebasePhotoStorage, testScheduler);

        // Appel de ma fonction à tester
        firebaseRetrieverService.synchronize(context, UID_USER);

        testScheduler.triggerActions();

        verify(annonceRepository, times(2)).singleSave(any());
        verify(firebasePhotoStorage, times(1)).savePhotoToLocalByListUrl(any(), anyLong(), anyList());
    }

    /**
     * Vérification que dans le cas où on a plusieurs annonces absentes de notre base locale
     * mais présente sur Firebase, on ne pose la question pour les récupérer qu'une seule fois.
     */
    @Test
    public void ShouldCallOnlyOnceRetrieve() {
        // Création de mon service à tester
        FirebaseRetrieverService firebaseRetrieverService = new FirebaseRetrieverService(firebaseAnnonceRepository, annonceRepository, firebasePhotoStorage, testScheduler);

        // Appel de ma fonction à tester
        LiveDataOnce<AtomicBoolean> liveData = firebaseRetrieverService.checkFirebaseRepository(UID_USER);

        Observer observer = mock(Observer.class);

        liveData.observeOnce(observer);

        // Déclenchement du scheduler RxJava 2.
        testScheduler.triggerActions();

        verify(observer, times(1)).onChanged(any());
    }
}