package oliweb.nc.oliweb;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnnonceFirebaseSenderTest {

    private static final String UID_ANNONCE = "UID";


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

    private void resetMock() {
        Mockito.reset(annonceFullRepository, annonceRepository, firebaseAnnonceRepository, photoFirebaseSender);
    }

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
        verify(photoFirebaseSender, times(1)).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * If getUidAndTimestampFromFirebase throw an error, should mark the annonce to Failed to send.
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_GetUidAndTimestampFromFirebase_Fail() {

        TestScheduler testScheduler = new TestScheduler();

        // Firebase Annonce Repo nous renvoie trois AnnonceDto
        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        // Création de mon service à tester
        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        // Appel de ma fonction à tester
        annonceFirebaseSender.processToSendAnnonceToFirebase(new AnnonceEntity());

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsFailedToSend(any());
        verify(annonceRepository, never()).markAsSending(any());
        verify(annonceRepository, never()).markAsSend(any());
        verify(firebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(photoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * Si le service markAsSending échoue on veut que l'annonce soit passée au statut markAsFailedToSend
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_MarkAsSending_Fail() {

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
        verify(annonceRepository, never()).markAsSend(any());
        verify(firebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(photoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    @Test
    public void ShouldMarkAsFailedToSend_When_FindMaybeByUidAnnonceAndFavorite_Return_Empty() {

        TestScheduler testScheduler = new TestScheduler();

        // Annonce entity renvoyée
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        // Firebase Annonce Repo nous renvoie trois AnnonceDto
        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(annonceRepository.findMaybeByUidAndFavorite(anyString(), anyInt())).thenReturn(Maybe.empty());

        // Création de mon service à tester
        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        // Appel de ma fonction à tester
        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsSending(any());
        verify(annonceRepository, times(1)).markAsFailedToSend(any());
        verify(annonceRepository, never()).markAsSend(any());
        verify(firebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(photoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    @Test
    public void ShouldMarkAsFailedToSend_When_FindAnnonceFullByAnnonceEntity_Return_Empty() {

        TestScheduler testScheduler = new TestScheduler();

        // Annonce entity renvoyée
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        // Firebase Annonce Repo nous renvoie trois AnnonceDto
        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(annonceRepository.findMaybeByUidAndFavorite(anyString(), anyInt())).thenReturn(Maybe.empty());
        when(annonceFullRepository.findAnnonceFullByAnnonceEntity(any())).thenReturn(Observable.error(new FirebaseRepositoryException("TEST ERROR")));

        // Création de mon service à tester
        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        // Appel de ma fonction à tester
        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsSending(any());
        verify(annonceRepository, times(1)).markAsFailedToSend(any());
        verify(annonceRepository, never()).markAsSend(any());
        verify(firebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(photoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    @Test
    public void ShouldMarkAsFailedToSend_When_FindAnnoncesByIdAnnonce_Return_Error() {

        TestScheduler testScheduler = new TestScheduler();

        // Annonce entity renvoyée
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        // Firebase Annonce Repo nous renvoie trois AnnonceDto
        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(annonceRepository.markAsSending(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SENDING)));
        when(annonceFullRepository.findAnnoncesByIdAnnonce(anyLong())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        // Création de mon service à tester
        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        // Appel de ma fonction à tester
        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsSending(any());
        verify(annonceRepository, times(1)).markAsFailedToSend(any());
        verify(annonceRepository, never()).markAsSend(any());
        verify(firebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(photoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }
}