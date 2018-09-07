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
import oliweb.nc.oliweb.repository.firebase.FirebaseRepositoryException;
import oliweb.nc.oliweb.repository.local.AnnonceFullRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.PhotoFirebaseSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
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
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        // Firebase Annonce Repo nous renvoie trois AnnonceDto
        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(annonceRepository.findMaybeByUidAndFavorite(argThat(UID_ANNONCE::equals), anyInt())).thenReturn(Maybe.just(annonceEntity));
        when(annonceRepository.markAsSending(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SENDING)));
        when(annonceRepository.markAsSend(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SEND)));
        when(annonceFullRepository.findAnnonceFullByAnnonceEntity(any())).thenReturn(Observable.just(annonceFull));
        when(annonceFullRepository.findAnnoncesByIdAnnonce(anyLong())).thenReturn(Single.just(annonceFull));
        when(firebaseAnnonceRepository.saveAnnonceToFirebase(any())).thenReturn(Single.just(UID_ANNONCE));

        // Création de mon service à tester
        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        // Appel de ma fonction à tester
        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsSending(any());
        verify(annonceRepository, times(1)).markAsSend(any());
        verify(firebaseAnnonceRepository, times(1)).saveAnnonceToFirebase(any());
    }

    /**
     * If getUidAndTimestampFromFirebase throw an error, should mark the annonce to Failed to send.
     */
    @Test
    public void ShouldThrowError() {

        TestScheduler testScheduler = new TestScheduler();

        // Firebase Annonce Repo nous renvoie trois AnnonceDto
        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        // Création de mon service à tester
        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        // Appel de ma fonction à tester
        annonceFirebaseSender.processToSendAnnonceToFirebase(new AnnonceEntity());

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsFailedToSend(any());
        verify(annonceRepository, times(0)).markAsSending(any());
        verify(annonceRepository, times(0)).markAsSend(any());
        verify(firebaseAnnonceRepository, times(0)).saveAnnonceToFirebase(any());
    }

    @Test
    public void ShouldThrowError2() {

        TestScheduler testScheduler = new TestScheduler();

        // Annonce entity renvoyée
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        // Firebase Annonce Repo nous renvoie trois AnnonceDto
        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(annonceRepository.markAsSending(any())).thenReturn(Observable.error(new FirebaseRepositoryException("TEST ERROR 2")));

        // Création de mon service à tester
        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        // Appel de ma fonction à tester
        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsSending(any());
        verify(annonceRepository, times(1)).markAsFailedToSend(any());
        verify(annonceRepository, times(0)).markAsSend(any());
        verify(firebaseAnnonceRepository, times(0)).saveAnnonceToFirebase(any());
    }
}