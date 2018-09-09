package oliweb.nc.oliweb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.dto.firebase.ChatFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseRepositoryException;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;

import static oliweb.nc.oliweb.Utility.UID_CHAT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirebaseMessageServiceTest {

    private static final String ERROR_MESSAGE = "TEST ERROR";

    @Mock
    private FirebaseMessageRepository firebaseMessageRepository;

    @Mock
    private FirebaseChatRepository firebaseChatRepository;

    @Mock
    private MessageRepository messageRepository;

    private TestScheduler testScheduler = new TestScheduler();

    private void resetMock() {
        Mockito.reset(firebaseChatRepository, firebaseMessageRepository, messageRepository);
    }

    @Test
    public void ShouldMarkAsFailedToSend_When_SaveChat_Return_Error() {

        // Annonce entity renvoyée
        MessageEntity messageEntityWithoutUid = Utility.createMessageEntity();
        messageEntityWithoutUid.setUidMessage(null);
        messageEntityWithoutUid.setTimestamp(null);
        messageEntityWithoutUid.setUidChat(UID_CHAT);

        MessageEntity messageEntityWithUid = Utility.createMessageEntity();
        messageEntityWithUid.setUidMessage("UID");
        messageEntityWithUid.setTimestamp(1234L);

        when(firebaseMessageRepository.getUidAndTimestampFromFirebase(anyString(), any())).thenReturn(Single.just(messageEntityWithUid));
        when(messageRepository.markMessageIsSending(any())).thenReturn(Observable.just(messageEntityWithUid));
        when(firebaseMessageRepository.saveMessage(any())).thenReturn(Single.error(new FirebaseRepositoryException(ERROR_MESSAGE)));

        // Création de mon service à tester
        FirebaseMessageService firebaseMessageService = new FirebaseMessageService(firebaseMessageRepository, firebaseChatRepository, messageRepository, testScheduler);

        // Appel de ma fonction à tester
        TestObserver<ChatFirebase> testSubscriber = new TestObserver<>();
        firebaseMessageService.sendMessage(messageEntityWithoutUid).subscribe(testSubscriber);

        testScheduler.triggerActions();

        testSubscriber.assertError(throwable -> ERROR_MESSAGE.equals(throwable.getMessage()));

        verify(messageRepository, times(1)).markMessageIsSending(any());
        verify(messageRepository, times(1)).markMessageAsFailedToSend(any());
        verify(messageRepository, never()).markMessageHasBeenSend(any());

        resetMock();
    }
}