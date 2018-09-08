package oliweb.nc.oliweb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.TestScheduler;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.dto.firebase.ChatFirebase;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseRepositoryException;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    public void ShouldUpdateAllMessage() {

        // Annonce entity renvoyée
        ChatEntity chatEntity = Utility.createChatEntity();
        ChatFirebase chatFirebase = new ChatFirebase();
        MessageEntity messageEntity = new MessageEntity();

        when(firebaseChatRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(chatEntity));
        when(chatRepository.markChatAsSending(any())).thenReturn(Observable.just(chatEntity));
        when(firebaseChatRepository.saveChat(any())).thenReturn(Single.just(chatFirebase));
        when(chatRepository.markChatAsSend(any())).thenReturn(Observable.just(chatEntity));
        when(messageRepository.getSingleByIdChat(anyLong())).thenReturn(Single.just(Collections.singletonList(messageEntity)));
        when(messageRepository.singleSave(any())).thenReturn(Single.just(messageEntity));

        // Création de mon service à tester
        FirebaseChatService firebaseChatService = new FirebaseChatService(firebaseChatRepository, chatRepository, messageRepository, firebaseUserRepository, testScheduler);

        // Appel de ma fonction à tester
        firebaseChatService.sendNewChat(chatEntity);

        testScheduler.triggerActions();

        verify(chatRepository, never()).markChatAsFailedToSend(any());
        verify(chatRepository, times(1)).markChatAsSending(any());
        verify(chatRepository, times(1)).markChatAsSend(any());
        verify(messageRepository, times(1)).getSingleByIdChat(anyLong());
        verify(messageRepository, times(1)).singleSave(any());

        resetMock();
    }

    @Test
    public void GetPhotoUrlsByUidUser() {
        Map<String, Boolean> mapMembersChat1 = new HashMap<>();
        mapMembersChat1.put("123", true);
        mapMembersChat1.put("456", true);

        Map<String, Boolean> mapMembersChat2 = new HashMap<>();
        mapMembersChat2.put("123", true);
        mapMembersChat2.put("890", true);

        ChatFirebase chatFirebase1 = new ChatFirebase();
        chatFirebase1.setMembers(mapMembersChat1);

        ChatFirebase chatFirebase2 = new ChatFirebase();
        chatFirebase2.setMembers(mapMembersChat2);

        UserEntity user1 = new UserEntity();
        user1.setUid("123");
        user1.setPhotoUrl("PhotoUser1");

        UserEntity user2 = new UserEntity();
        user2.setUid("456");
        user2.setPhotoUrl("PhotoUser2");

        UserEntity user3 = new UserEntity();
        user3.setUid("890");
        user3.setPhotoUrl("PhotoUser9");

        when(firebaseChatRepository.getByUidUser(anyString())).thenReturn(Single.just(Arrays.asList(chatFirebase1, chatFirebase2)));
        when(firebaseUserRepository.getUtilisateurByUid("123")).thenReturn(Single.just(user1));
        when(firebaseUserRepository.getUtilisateurByUid("456")).thenReturn(Single.just(user2));
        when(firebaseUserRepository.getUtilisateurByUid("890")).thenReturn(Single.just(user3));

        // Création de mon service à tester
        FirebaseChatService firebaseChatService = new FirebaseChatService(firebaseChatRepository, chatRepository, messageRepository, firebaseUserRepository, testScheduler);

        // Appel de ma fonction à tester
        TestObserver<HashMap<String, UserEntity>> subscriber = new TestObserver<>();
        firebaseChatService.getPhotoUrlsByUidUser("123").subscribe(subscriber);

        testScheduler.triggerActions();

        subscriber.assertNoErrors();
        subscriber.assertValue(map -> map.size() == 3);

        verify(firebaseUserRepository, times(1)).getUtilisateurByUid("123");
        verify(firebaseUserRepository, times(1)).getUtilisateurByUid("456");
        verify(firebaseUserRepository, times(1)).getUtilisateurByUid("890");

        resetMock();
    }
}