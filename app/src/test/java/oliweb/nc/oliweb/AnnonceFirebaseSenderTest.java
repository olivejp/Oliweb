package oliweb.nc.oliweb;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.TestScheduler;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.PhotoFirebaseSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnnonceFirebaseSenderTest {

    private static final String UID_ANNONCE = "UID";
    private static final String UID_USER = "123";

    @Mock
    Context context;

    @Mock
    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    @Mock
    private AnnonceRepository annonceRepository;

    @Mock
    private PhotoFirebaseSender photoFirebaseSender;

    @Mock
    private AnnonceFullRepository annonceFullRepository;

    @Test
    public void ShouldSaveOnce() {

        TestScheduler testScheduler = new TestScheduler();

        // Annonce entity renvoyée
        AnnonceEntity annonceEntity = new AnnonceEntity();
        annonceEntity.setUid(UID_ANNONCE);
        annonceEntity.setUidUser(UID_USER);
        annonceEntity.setIdAnnonce(55L);
        annonceEntity.setTitre("Mon titre");
        annonceEntity.setDatePublication(123456L);
        annonceEntity.setDescription("Ma description");
        annonceEntity.setPrix(7000);
        annonceEntity.setContactByMsg("O");
        annonceEntity.setContactByTel("N");
        annonceEntity.setContactByEmail("O");
        annonceEntity.setIdCategorie(1L);


        // Firebase Annonce Repo nous renvoie trois AnnonceDto
        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));

        // AnnonceDto1 n'existe pas encore en base
        when(annonceRepository.findMaybeByUidAndFavorite(argThat(UID_ANNONCE::equals), anyInt())).thenReturn(Maybe.just(annonceEntity));

        // AnnonceDto1 n'existe pas encore en base
        when(annonceRepository.markAsSending(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SENDING)));
        when(annonceRepository.markAsSend(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SEND)));

        AnnonceFull annonceFull = Utility.createAnnonceFull();
        when(annonceFullRepository.findAnnoncesByIdAnnonce(55L)).thenReturn(Single.just(annonceFull));

        // Création de mon service à tester
        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        // Appel de ma fonction à tester
        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        // TODO Finir ce test qui ne passe pas encore
        verify(annonceRepository, times(1)).markAsSending(any());
        verify(annonceRepository, times(1)).markAsSend(any());
    }
}