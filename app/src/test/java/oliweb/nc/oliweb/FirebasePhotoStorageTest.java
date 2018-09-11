package oliweb.nc.oliweb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.dto.firebase.ChatFirebase;
import oliweb.nc.oliweb.dto.firebase.MessageFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseMessageRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseRepositoryException;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;

import static oliweb.nc.oliweb.Utility.UID_CHAT;
import static oliweb.nc.oliweb.Utility.UID_USER;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirebasePhotoStorageTest {

    private static final String ERROR_MESSAGE = "TEST ERROR";

    @Mock
    private FirebaseMessageRepository firebaseMessageRepository;

    @Mock
    private FirebaseChatRepository firebaseChatRepository;

    @Mock
    private MessageRepository messageRepository;

    private void resetMock() {
        Mockito.reset(firebaseChatRepository, firebaseMessageRepository, messageRepository);
    }

    /**
     * Lors de l'appel de sendMessage() si la méthode saveMessage() plante
     * On doit appeler la méthode markMessageAsFailedToSend() une seule fois
     * On doit appeler la méthode markMessageIsSending() une seule fois
     * On ne doit jamais appeler :
     * - markMessageHasBeenSend()
     * - updateLastMessageChat()
     */
    @Test
    public void if_saveMessage_fails() {

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
        FirebaseMessageService firebaseMessageService = new FirebaseMessageService(firebaseMessageRepository, firebaseChatRepository, messageRepository);

        // Appel de ma fonction à tester
        TestObserver<ChatFirebase> testSubscriber = new TestObserver<>();
        firebaseMessageService.sendMessage(messageEntityWithoutUid).subscribe(testSubscriber);

        testSubscriber.assertError(throwable -> ERROR_MESSAGE.equals(throwable.getMessage()));

        verify(messageRepository, times(1)).markMessageIsSending(any());
        verify(messageRepository, times(1)).markMessageAsFailedToSend(any());
        verify(firebaseMessageRepository, times(1)).saveMessage(any());
        verify(messageRepository, never()).markMessageHasBeenSend(any());
        verify(firebaseChatRepository, never()).updateLastMessageChat(any());

        resetMock();
    }

    /**
     * Lors de l'appel de sendMessage() si la méthode getUidAndTimestampFromFirebase() plante
     * On doit appeler la méthode markMessageAsFailedToSend() une seule fois
     * On ne doit jamais appeler :
     * - markMessageIsSending()
     * - markMessageHasBeenSend()
     * - saveMessage()
     * - updateLastMessageChat()
     */
    @Test
    public void if_getUidAndTimestampFromFirebase_fails() {

        // Annonce entity renvoyée
        MessageEntity messageEntityWithoutUid = Utility.createMessageEntity();
        messageEntityWithoutUid.setUidMessage(null);
        messageEntityWithoutUid.setTimestamp(null);
        messageEntityWithoutUid.setUidChat(UID_CHAT);

        MessageEntity messageEntityWithUid = Utility.createMessageEntity();
        messageEntityWithUid.setUidMessage("UID");
        messageEntityWithUid.setTimestamp(1234L);

        when(firebaseMessageRepository.getUidAndTimestampFromFirebase(anyString(), any())).thenReturn(Single.error(new FirebaseRepositoryException(ERROR_MESSAGE)));

        // Création de mon service à tester
        FirebaseMessageService firebaseMessageService = new FirebaseMessageService(firebaseMessageRepository, firebaseChatRepository, messageRepository);

        // Appel de ma fonction à tester
        TestObserver<ChatFirebase> testSubscriber = new TestObserver<>();
        firebaseMessageService.sendMessage(messageEntityWithoutUid).subscribe(testSubscriber);

        testSubscriber.assertError(throwable -> ERROR_MESSAGE.equals(throwable.getMessage()));

        verify(messageRepository, times(1)).markMessageAsFailedToSend(any());
        verify(messageRepository, never()).markMessageIsSending(any());
        verify(messageRepository, never()).markMessageHasBeenSend(any());
        verify(firebaseMessageRepository, never()).saveMessage(any());
        verify(firebaseChatRepository, never()).updateLastMessageChat(any());

        resetMock();
    }

    /**
     * Si la méthode markMessageHasBeenSend() plante, on ne doit pas appeler :
     * - updateLastMessageChat()
     */
    @Test
    public void if_markMessageHasBeenSend_fails() {

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
        when(firebaseMessageRepository.saveMessage(any())).thenReturn(Single.just(new MessageFirebase()));
        when(messageRepository.markMessageHasBeenSend(any())).thenReturn(Observable.error(new FirebaseRepositoryException(ERROR_MESSAGE)));

        // Création de mon service à tester
        FirebaseMessageService firebaseMessageService = new FirebaseMessageService(firebaseMessageRepository, firebaseChatRepository, messageRepository);

        // Appel de ma fonction à tester
        TestObserver<ChatFirebase> testSubscriber = new TestObserver<>();
        firebaseMessageService.sendMessage(messageEntityWithoutUid).subscribe(testSubscriber);

        testSubscriber.assertError(throwable -> ERROR_MESSAGE.equals(throwable.getMessage()));

        verify(messageRepository, times(1)).markMessageIsSending(any());
        verify(messageRepository, times(1)).markMessageHasBeenSend(any());
        verify(messageRepository, times(1)).markMessageAsFailedToSend(any());
        verify(firebaseMessageRepository, times(1)).saveMessage(any());
        verify(firebaseChatRepository, never()).updateLastMessageChat(any());

        resetMock();
    }


    /**
     * Si la méthode updateLastMessageChat() plante, on doit appeler :
     * - markMessageAsFailedToSend()
     */
    @Test
    public void if_updateLasMessageChat_fails() {

        MessageEntity messageEntityWithUid = Utility.createMessageEntity();
        messageEntityWithUid.setUidMessage("UID");
        messageEntityWithUid.setTimestamp(1234L);

        when(messageRepository.markMessageIsSending(any())).thenReturn(Observable.just(messageEntityWithUid));
        when(firebaseMessageRepository.saveMessage(any())).thenReturn(Single.just(new MessageFirebase()));
        when(messageRepository.markMessageHasBeenSend(any())).thenReturn(Observable.just(messageEntityWithUid));
        when(firebaseChatRepository.updateLastMessageChat(any())).thenReturn(Observable.error(new FirebaseRepositoryException(ERROR_MESSAGE)));

        // Création de mon service à tester
        FirebaseMessageService firebaseMessageService = new FirebaseMessageService(firebaseMessageRepository, firebaseChatRepository, messageRepository);

        // Appel de ma fonction à tester
        TestObserver<ChatFirebase> testSubscriber = new TestObserver<>();
        firebaseMessageService.sendMessage(messageEntityWithUid).subscribe(testSubscriber);

        testSubscriber.assertError(throwable -> ERROR_MESSAGE.equals(throwable.getMessage()));

        verify(messageRepository, times(1)).markMessageIsSending(any());
        verify(messageRepository, times(1)).markMessageHasBeenSend(any());
        verify(messageRepository, times(1)).markMessageAsFailedToSend(any());
        verify(firebaseMessageRepository, times(1)).saveMessage(any());
        verify(firebaseChatRepository, times(1)).updateLastMessageChat(any());

        resetMock();
    }

    /**
     * Si la méthode getByUidUser() plante, on doit générer une erreur.
     */
    @Test
    public void if_getByUidUser_fails() {

        when(firebaseChatRepository.getByUidUser(any())).thenReturn(Single.error(new FirebaseRepositoryException(ERROR_MESSAGE)));

        // Création de mon service à tester
        FirebaseMessageService firebaseMessageService = new FirebaseMessageService(firebaseMessageRepository, firebaseChatRepository, messageRepository);

        // Appel de ma fonction à tester
        TestObserver<Integer> testSubscriber = new TestObserver<>();
        firebaseMessageService.getCount("UID").subscribe(testSubscriber);

        testSubscriber.assertError(throwable -> ERROR_MESSAGE.equals(throwable.getMessage()));

        verify(firebaseChatRepository, times(1)).getByUidUser(any());
        verify(firebaseMessageRepository, never()).getCountMessageByUidUserAndUidChat(any(), any());

        resetMock();
    }

    /**
     * Si la méthode getByUidUser() plante, on doit générer une erreur.
     */
    @Test
    public void if_getByUidUser_succeed() {

        ChatFirebase chatFirebase1 = new ChatFirebase();
        ChatFirebase chatFirebase2 = new ChatFirebase();
        ChatFirebase chatFirebase3 = new ChatFirebase();

        chatFirebase1.setUid("UID1");
        chatFirebase2.setUid("UID2");
        chatFirebase3.setUid("UID3");

        List<ChatFirebase> listChatFirebase = new ArrayList<>();
        listChatFirebase.add(chatFirebase1);
        listChatFirebase.add(chatFirebase2);
        listChatFirebase.add(chatFirebase3);

        when(firebaseChatRepository.getByUidUser(UID_USER)).thenReturn(Single.just(listChatFirebase));
        when(firebaseMessageRepository.getCountMessageByUidUserAndUidChat(UID_USER, "UID1")).thenReturn(Single.just(12L));
        when(firebaseMessageRepository.getCountMessageByUidUserAndUidChat(UID_USER, "UID2")).thenReturn(Single.just(0L));
        when(firebaseMessageRepository.getCountMessageByUidUserAndUidChat(UID_USER, "UID3")).thenReturn(Single.just(45L));

        // Création de mon service à tester
        FirebaseMessageService firebaseMessageService = new FirebaseMessageService(firebaseMessageRepository, firebaseChatRepository, messageRepository);

        // Appel de ma fonction à tester
        TestObserver<Integer> testSubscriber = new TestObserver<>();
        firebaseMessageService.getCount(UID_USER).subscribe(testSubscriber);

        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(57);

        verify(firebaseChatRepository, times(1)).getByUidUser(any());
        verify(firebaseMessageRepository, times(3)).getCountMessageByUidUserAndUidChat(argThat(UID_USER::equals), any());

        resetMock();
    }
}