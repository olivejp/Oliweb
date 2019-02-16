package oliweb.nc.oliweb;

import org.junit.After;
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
    private FirebaseAnnonceRepository mockFirebaseAnnonceRepository;

    @Mock
    private AnnonceRepository mockAnnonceRepository;

    @Mock
    private PhotoFirebaseSender mockPhotoFirebaseSender;

    @Mock
    private AnnonceFullRepository mockAnnonceFullRepository;

    private TestScheduler testScheduler = new TestScheduler();

    @After
    private void resetMock() {
        Mockito.reset(mockAnnonceFullRepository, mockAnnonceRepository, mockFirebaseAnnonceRepository, mockPhotoFirebaseSender);
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  Nominal case
     * Expectations :
     * - The annonce is marked as sending
     * - The annonce is saved in Firebase
     * - The annonce is marked as send
     * - The  mockPhotoFirebaseSender.sendPhotosToRemote() is called one time
     */
    @Test
    public void ShouldSaveOnce() {
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        when(mockFirebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(mockAnnonceRepository.findMaybeByUidAndFavorite(argThat(UID_ANNONCE::equals), anyInt())).thenReturn(Maybe.just(annonceEntity));
        when(mockAnnonceRepository.markAsSending(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SENDING)));
        when(mockAnnonceRepository.markAsSend(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SEND)));
        when(mockAnnonceFullRepository.findAnnonceFullByAnnonceEntity(any())).thenReturn(Observable.just(annonceFull));
        when(mockAnnonceFullRepository.findAnnoncesByIdAnnonce(anyLong())).thenReturn(Single.just(annonceFull));
        when(mockFirebaseAnnonceRepository.saveAnnonceToFirebase(any())).thenReturn(Single.just(UID_ANNONCE));

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(mockFirebaseAnnonceRepository, mockAnnonceRepository, mockPhotoFirebaseSender, mockAnnonceFullRepository, testScheduler);

        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(mockAnnonceRepository, times(1)).markAsSending(any());
        verify(mockAnnonceRepository, times(1)).markAsSend(any());
        verify(mockFirebaseAnnonceRepository, times(1)).saveAnnonceToFirebase(any());
        verify(mockPhotoFirebaseSender, times(1)).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  FirebaseAnnonceRepository.getUidAndTimestampFromFirebase() fails
     * Expectations :
     * - The annonce is marked as failed to send
     * - The method AnnonceRepository.markAsSending() is never call
     * - The method AnnonceRepository.markAsToSend() is never call
     * - The method FirebaseAnnonceRepository.saveAnnonceToFirebase() is never call
     * - The method PhotoFirebaseSender.sentPhotosToRemote() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_GetUidAndTimestampFromFirebase_Fail() {
        when(mockFirebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(mockFirebaseAnnonceRepository, mockAnnonceRepository, mockPhotoFirebaseSender, mockAnnonceFullRepository, testScheduler);

        annonceFirebaseSender.processToSendAnnonceToFirebase(new AnnonceEntity());

        testScheduler.triggerActions();

        verify(mockAnnonceRepository, times(1)).markAsFailedToSend(any());
        verify(mockAnnonceRepository, never()).markAsSending(any());
        verify(mockAnnonceRepository, never()).markAsSend(any());
        verify(mockFirebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(mockPhotoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  FirebaseAnnonceRepository.getUidAndTimestampFromFirebase() fails
     * Expectations :
     * - The annonce is marked as failed to send
     * - The method AnnonceRepository.markAsSending() is never call
     * - The method AnnonceRepository.markAsToSend() is never call
     * - The method FirebaseAnnonceRepository.saveAnnonceToFirebase() is never call
     * - The method PhotoFirebaseSender.sentPhotosToRemote() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_MarkAsSending_Fail() {
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        when(mockFirebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(mockAnnonceRepository.markAsSending(any())).thenReturn(Observable.error(new FirebaseRepositoryException("TEST ERROR 2")));

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(mockFirebaseAnnonceRepository, mockAnnonceRepository, mockPhotoFirebaseSender, mockAnnonceFullRepository, testScheduler);

        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(mockAnnonceRepository, times(1)).markAsSending(any());
        verify(mockAnnonceRepository, times(1)).markAsFailedToSend(any());
        verify(mockAnnonceRepository, never()).markAsSend(any());
        verify(mockFirebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(mockPhotoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  FirebaseAnnonceRepository.findMaybeByUidAndFavorite() return an empty maybe
     * Expectations :
     * - The method AnnonceRepository.markAsFailedToSend() is called one time
     * - The method AnnonceRepository.markAsSending() is called one time
     * - The method AnnonceRepository.markAsToSend() is never call
     * - The method FirebaseAnnonceRepository.saveAnnonceToFirebase() is never call
     * - The method PhotoFirebaseSender.sentPhotosToRemote() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_FindMaybeByUidAnnonceAndFavorite_Return_Empty() {
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        when(mockFirebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(mockAnnonceRepository.findMaybeByUidAndFavorite(anyString(), anyInt())).thenReturn(Maybe.empty());

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(mockFirebaseAnnonceRepository, mockAnnonceRepository, mockPhotoFirebaseSender, mockAnnonceFullRepository, testScheduler);

        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(mockAnnonceRepository, times(1)).markAsSending(any());
        verify(mockAnnonceRepository, times(1)).markAsFailedToSend(any());
        verify(mockAnnonceRepository, never()).markAsSend(any());
        verify(mockFirebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(mockPhotoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  AnnonceFullRepository.findAnnonceFullByAnnonceEntity() return an Exception
     * Expectations :
     * - The method AnnonceRepository.markAsSending() is called one time
     * - The method AnnonceRepository.markAsFailedToSend() is called one time
     * - The method AnnonceRepository.markAsToSend() is never call
     * - The method FirebaseAnnonceRepository.saveAnnonceToFirebase() is never call
     * - The method PhotoFirebaseSender.sentPhotosToRemote() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_FindAnnonceFullByAnnonceEntity_Return_Empty() {
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        when(mockFirebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(mockAnnonceRepository.findMaybeByUidAndFavorite(anyString(), anyInt())).thenReturn(Maybe.empty());
        when(mockAnnonceFullRepository.findAnnonceFullByAnnonceEntity(any())).thenReturn(Observable.error(new FirebaseRepositoryException("TEST ERROR")));

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(mockFirebaseAnnonceRepository, mockAnnonceRepository, mockPhotoFirebaseSender, mockAnnonceFullRepository, testScheduler);

        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(mockAnnonceRepository, times(1)).markAsSending(any());
        verify(mockAnnonceRepository, times(1)).markAsFailedToSend(any());
        verify(mockAnnonceRepository, never()).markAsSend(any());
        verify(mockFirebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(mockPhotoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }

    /**
     * Method tested : AnnonceFirebaseSender.processToSendAnnonceToFirebase()
     * Conditions :  AnnonceFullRepository.findAnnoncesByIdAnnonce() return an Exception
     * Expectations :
     * - The method AnnonceRepository.markAsSending() is called one time
     * - The method AnnonceRepository.markAsFailedToSend() is called one time
     * - The method AnnonceRepository.markAsToSend() is never call
     * - The method FirebaseAnnonceRepository.saveAnnonceToFirebase() is never call
     * - The method PhotoFirebaseSender.sentPhotosToRemote() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_FindAnnoncesByIdAnnonce_Return_Error() {
        AnnonceFull annonceFull = Utility.createAnnonceFull();
        AnnonceEntity annonceEntity = annonceFull.getAnnonce();

        when(mockFirebaseAnnonceRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(annonceEntity));
        when(mockAnnonceRepository.markAsSending(any())).thenReturn(Observable.just(annonceEntity.setStatutAndReturn(StatusRemote.SENDING)));
        when(mockAnnonceFullRepository.findAnnoncesByIdAnnonce(anyLong())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        AnnonceFirebaseSender annonceFirebaseSender = new AnnonceFirebaseSender(mockFirebaseAnnonceRepository, mockAnnonceRepository, mockPhotoFirebaseSender, mockAnnonceFullRepository, testScheduler);

        annonceFirebaseSender.processToSendAnnonceToFirebase(annonceEntity);

        testScheduler.triggerActions();

        verify(mockAnnonceRepository, times(1)).markAsSending(any());
        verify(mockAnnonceRepository, times(1)).markAsFailedToSend(any());
        verify(mockAnnonceRepository, never()).markAsSend(any());
        verify(mockFirebaseAnnonceRepository, never()).saveAnnonceToFirebase(any());
        verify(mockPhotoFirebaseSender, never()).sendPhotosToRemote(anyList());

        resetMock();
    }
}