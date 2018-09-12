package oliweb.nc.oliweb;

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
    private FirebaseAnnonceRepository firebaseAnnonceRepository;

    @Mock
    private AnnonceRepository annonceRepository;

    @Mock
    private PhotoFirebaseSender photoFirebaseSender;

    @Mock
    private AnnonceFullRepository annonceFullRepository;

    private TestScheduler testScheduler = new TestScheduler();

    private void resetMock() {
        Mockito.reset(annonceFullRepository, annonceRepository, firebaseAnnonceRepository, photoFirebaseSender);
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  Nominal case
     * Expectations :
     * - The annonce is marked as sending
     * - The annonce is saved in Firebase
     * - The annonce is marked as send
     * - The  photoFirebaseSender.sendPhotosToRemote() is called one time
     */
    @Test
    public void ShouldSaveOnce() {
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(annonceRepository.findMaybeByUidAndFavorite(argThat(UID_ANNONCE::equals), anyInt())).thenReturn(Maybe.just(annonceEntity));
        when(annonceRepository.markAsSending(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SENDING)));
        when(annonceRepository.markAsSend(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SEND)));
        when(annonceFullRepository.findAnnonceFullByAnnonceEntity(any())).thenReturn(Observable.just(annonceFull));
        when(annonceFullRepository.findAnnoncesByIdAnnonce(anyLong())).thenReturn(Single.just(annonceFull));
        when(firebaseAnnonceRepository.saveAnnonceToFirebase(any())).thenReturn(Single.just(UID_ANNONCE));

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsSending(any());
        verify(annonceRepository, times(1)).markAsSend(any());
        verify(firebaseAnnonceRepository, times(1)).saveAnnonceToFirebase(any());
        verify(photoFirebaseSender, times(1)).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  FirebaseAnnonceRepository.getUidAndTimestampFromFirebase() fails
     * Expectations :
     * - The annonce is marked as failed to send
     * - The method AnnonceRepository.markAsSending() is never call
     * - The method AnnonceRepository.markAsSend() is never call
     * - The method FirebaseAnnonceRepository.saveAnnonceToFirebase() is never call
     * - The method PhotoFirebaseSender.sentPhotosToRemote() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_GetUidAndTimestampFromFirebase_Fail() {
        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

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
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  FirebaseAnnonceRepository.getUidAndTimestampFromFirebase() fails
     * Expectations :
     * - The annonce is marked as failed to send
     * - The method AnnonceRepository.markAsSending() is never call
     * - The method AnnonceRepository.markAsSend() is never call
     * - The method FirebaseAnnonceRepository.saveAnnonceToFirebase() is never call
     * - The method PhotoFirebaseSender.sentPhotosToRemote() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_MarkAsSending_Fail() {
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(annonceRepository.markAsSending(any())).thenReturn(Observable.error(new FirebaseRepositoryException("TEST ERROR 2")));

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsSending(any());
        verify(annonceRepository, times(1)).markAsFailedToSend(any());
        verify(annonceRepository, never()).markAsSend(any());
        verify(firebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(photoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  FirebaseAnnonceRepository.findMaybeByUidAndFavorite() return an empty maybe
     * Expectations :
     * - The method AnnonceRepository.markAsFailedToSend() is called one time
     * - The method AnnonceRepository.markAsSending() is called one time
     * - The method AnnonceRepository.markAsSend() is never call
     * - The method FirebaseAnnonceRepository.saveAnnonceToFirebase() is never call
     * - The method PhotoFirebaseSender.sentPhotosToRemote() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_FindMaybeByUidAnnonceAndFavorite_Return_Empty() {
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(annonceRepository.findMaybeByUidAndFavorite(anyString(), anyInt())).thenReturn(Maybe.empty());

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsSending(any());
        verify(annonceRepository, times(1)).markAsFailedToSend(any());
        verify(annonceRepository, never()).markAsSend(any());
        verify(firebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(photoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  AnnonceFullRepository.findAnnonceFullByAnnonceEntity() return an Exception
     * Expectations :
     * - The method AnnonceRepository.markAsSending() is called one time
     * - The method AnnonceRepository.markAsFailedToSend() is called one time
     * - The method AnnonceRepository.markAsSend() is never call
     * - The method FirebaseAnnonceRepository.saveAnnonceToFirebase() is never call
     * - The method PhotoFirebaseSender.sentPhotosToRemote() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_FindAnnonceFullByAnnonceEntity_Return_Empty() {
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(annonceRepository.findMaybeByUidAndFavorite(anyString(), anyInt())).thenReturn(Maybe.empty());
        when(annonceFullRepository.findAnnonceFullByAnnonceEntity(any())).thenReturn(Observable.error(new FirebaseRepositoryException("TEST ERROR")));

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(annonceRepository, times(1)).markAsSending(any());
        verify(annonceRepository, times(1)).markAsFailedToSend(any());
        verify(annonceRepository, never()).markAsSend(any());
        verify(firebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(photoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  AnnonceFullRepository.findAnnoncesByIdAnnonce() return an Exception
     * Expectations :
     * - The method AnnonceRepository.markAsSending() is called one time
     * - The method AnnonceRepository.markAsFailedToSend() is called one time
     * - The method AnnonceRepository.markAsSend() is never call
     * - The method FirebaseAnnonceRepository.saveAnnonceToFirebase() is never call
     * - The method PhotoFirebaseSender.sentPhotosToRemote() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_FindAnnoncesByIdAnnonce_Return_Error() {
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        when(firebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(annonceRepository.markAsSending(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SENDING)));
        when(annonceFullRepository.findAnnoncesByIdAnnonce(anyLong())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(firebaseAnnonceRepository, annonceRepository, photoFirebaseSender, annonceFullRepository, testScheduler);

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