package oliweb.nc.oliweb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.TestScheduler;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseRepositoryException;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FirebaseChatServiceTest {

    @Mock
    private FirebaseChatRepository firebaseChatRepository;

    @Mock
    private FirebaseUserRepository firebaseUserRepository;

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MessageRepository messageRepository;

    private TestScheduler testScheduler = new TestScheduler();

    private void resetMock() {
        Mockito.reset(firebaseChatRepository, firebaseUserRepository, chatRepository, messageRepository);
    }

    @Test
    public void ShouldMarkAsFailedToSend_When_GetUidAndTimestampFromFirebase_Return_Error() {

        // Annonce entity renvoyée
        ChatEntity chatEntity = Utility.createChatEntity();

        // Firebase Annonce Repo nous renvoie trois AnnonceDto
        when(firebaseChatRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        // Création de mon service à tester
        FirebaseChatService firebaseChatService = new FirebaseChatService(firebaseChatRepository, chatRepository, messageRepository, firebaseUserRepository, testScheduler);

        // Appel de ma fonction à tester
        firebaseChatService.sendNewChat(chatEntity);

        testScheduler.triggerActions();

        verify(chatRepository, times(1)).markChatAsFailedToSend(any());
        verify(chatRepository, never()).markChatAsSending(any());
        verify(chatRepository, never()).markChatAsSend(any());

        resetMock();
    }

    @Test
    public void ShouldMarkAsFailedToSend_When_SaveChat_Return_Error() {

        // Annonce entity renvoyée
        ChatEntity chatEntity = Utility.createChatEntity();

        when(firebaseChatRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(chatEntity));
        when(chatRepository.markChatAsSending(any())).thenReturn(Observable.just(chatEntity));
        when(firebaseChatRepository.saveChat(any())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        // Création de mon service à tester
        FirebaseChatService firebaseChatService = new FirebaseChatService(firebaseChatRepository, chatRepository, messageRepository, firebaseUserRepository, testScheduler);

        // Appel de ma fonction à tester
        firebaseChatService.sendNewChat(chatEntity);

        testScheduler.triggerActions();

        verify(chatRepository, times(1)).markChatAsFailedToSend(any());
        verify(chatRepository, times(1)).markChatAsSending(any());
        verify(messageRepository, never()).getSingleByIdChat(anyLong());
        verify(chatRepository, never()).markChatAsSend(any());

        resetMock();
    }
}