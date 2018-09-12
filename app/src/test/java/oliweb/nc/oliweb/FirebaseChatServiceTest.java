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

    public static final String UID_USER_1 = "123";
    public static final String UID_USER_2 = "456";
    public static final String UID_USER_3 = "890";
    private static final String PHOTO_URL_1 = "photo_url_1";
    private static final String PHOTO_URL_2 = "photo_url_2";
    private static final String PHOTO_URL_3 = "photo_url_3";
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

    /**
     * Method tested : FirebaseChatService.sendNewChat()
     * Conditions :  FirebaseChatRepository.getUidAndTimestampFromFirebase() return an Exception
     * Expectations :
     * - The method ChatRepository.markChatAsFailedToSend() is called one time
     * - The method ChatRepository.markChatAsSending() is never call
     * - The method ChatRepository.markChatAsSend() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_GetUidAndTimestampFromFirebase_Return_Error() {
        ChatEntity chatEntity = Utility.createChatEntity();

        when(firebaseChatRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        FirebaseChatService firebaseChatService = new FirebaseChatService(firebaseChatRepository, chatRepository, messageRepository, firebaseUserRepository, testScheduler);

        firebaseChatService.sendNewChat(chatEntity);

        testScheduler.triggerActions();

        verify(chatRepository, times(1)).markChatAsFailedToSend(any());
        verify(chatRepository, never()).markChatAsSending(any());
        verify(chatRepository, never()).markChatAsSend(any());

        resetMock();
    }

    /**
     * Method tested : FirebaseChatService.sendNewChat()
     * Conditions :  FirebaseChatRepository.saveChat() return an Exception
     * Expectations :
     * - The method ChatRepository.markChatAsFailedToSend() is called one time
     * - The method ChatRepository.markChatAsSending() is called one time
     * - The method MessageRepository.getSingleByIdChat() is never call
     * - The method ChatRepository.markChatAsSend() is never call
     */
    @Test
    public void ShouldMarkAsFailedToSend_When_SaveChat_Return_Error() {
        ChatEntity chatEntity = Utility.createChatEntity();

        when(firebaseChatRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(chatEntity));
        when(chatRepository.markChatAsSending(any())).thenReturn(Observable.just(chatEntity));
        when(firebaseChatRepository.saveChat(any())).thenReturn(Single.error(new FirebaseRepositoryException("TEST ERROR")));

        FirebaseChatService firebaseChatService = new FirebaseChatService(firebaseChatRepository, chatRepository, messageRepository, firebaseUserRepository, testScheduler);

        firebaseChatService.sendNewChat(chatEntity);

        testScheduler.triggerActions();

        verify(chatRepository, times(1)).markChatAsFailedToSend(any());
        verify(chatRepository, times(1)).markChatAsSending(any());
        verify(messageRepository, never()).getSingleByIdChat(anyLong());
        verify(chatRepository, never()).markChatAsSend(any());

        resetMock();
    }

    /**
     * Method tested : FirebaseChatService.sendNewChat()
     * Conditions :  Nominal case
     * Expectations :
     * - The method ChatRepository.markChatAsFailedToSend() is never called
     * - The method ChatRepository.markChatAsSending() is called one time
     * - The method ChatRepository.markChatAsSend() is called one time
     * - The method MessageRepository.getSingleByIdChat() is call one time
     * - The method MessageRepository.singleSave() is called one time
     */
    @Test
    public void ShouldUpdateAllMessage() {

        ChatEntity chatEntity = Utility.createChatEntity();
        ChatFirebase chatFirebase = new ChatFirebase();
        MessageEntity messageEntity = new MessageEntity();

        when(firebaseChatRepository.getUidAndTimestampFromFirebase(any())).thenReturn(Single.just(chatEntity));
        when(chatRepository.markChatAsSending(any())).thenReturn(Observable.just(chatEntity));
        when(firebaseChatRepository.saveChat(any())).thenReturn(Single.just(chatFirebase));
        when(chatRepository.markChatAsSend(any())).thenReturn(Observable.just(chatEntity));
        when(messageRepository.getSingleByIdChat(anyLong())).thenReturn(Single.just(Collections.singletonList(messageEntity)));
        when(messageRepository.singleSave(any())).thenReturn(Single.just(messageEntity));

        FirebaseChatService firebaseChatService = new FirebaseChatService(firebaseChatRepository, chatRepository, messageRepository, firebaseUserRepository, testScheduler);

        firebaseChatService.sendNewChat(chatEntity);

        testScheduler.triggerActions();

        verify(chatRepository, never()).markChatAsFailedToSend(any());
        verify(chatRepository, times(1)).markChatAsSending(any());
        verify(chatRepository, times(1)).markChatAsSend(any());
        verify(messageRepository, times(1)).getSingleByIdChat(anyLong());
        verify(messageRepository, times(1)).singleSave(any());

        resetMock();
    }

    /**
     * Method tested : FirebaseChatService.getPhotoUrlsByUidUser()
     * Conditions :  Nominal case
     * Expectations :
     * - The uid user (123) passed has two active chats.
     * - Should return three photo urls
     */
    @Test
    public void GetPhotoUrlsByUidUser() {
        Map<String, Boolean> mapMembersChat1 = new HashMap<>();
        mapMembersChat1.put(UID_USER_1, true);
        mapMembersChat1.put(UID_USER_2, true);

        Map<String, Boolean> mapMembersChat2 = new HashMap<>();
        mapMembersChat2.put(UID_USER_1, true);
        mapMembersChat2.put(UID_USER_3, true);

        ChatFirebase chatFirebase1 = new ChatFirebase();
        chatFirebase1.setMembers(mapMembersChat1);

        ChatFirebase chatFirebase2 = new ChatFirebase();
        chatFirebase2.setMembers(mapMembersChat2);

        UserEntity user1 = new UserEntity();
        user1.setUid(UID_USER_1);
        user1.setPhotoUrl(PHOTO_URL_1);

        UserEntity user2 = new UserEntity();
        user2.setUid(UID_USER_2);
        user2.setPhotoUrl(PHOTO_URL_2);

        UserEntity user3 = new UserEntity();
        user3.setUid(UID_USER_3);
        user3.setPhotoUrl(PHOTO_URL_3);

        when(firebaseChatRepository.getByUidUser(anyString())).thenReturn(Single.just(Arrays.asList(chatFirebase1, chatFirebase2)));
        when(firebaseUserRepository.getUtilisateurByUid(UID_USER_1)).thenReturn(Single.just(user1));
        when(firebaseUserRepository.getUtilisateurByUid(UID_USER_2)).thenReturn(Single.just(user2));
        when(firebaseUserRepository.getUtilisateurByUid(UID_USER_3)).thenReturn(Single.just(user3));

        FirebaseChatService firebaseChatService = new FirebaseChatService(firebaseChatRepository, chatRepository, messageRepository, firebaseUserRepository, testScheduler);

        TestObserver<HashMap<String, UserEntity>> subscriber = new TestObserver<>();
        firebaseChatService.getPhotoUrlsByUidUser(UID_USER_1).subscribe(subscriber);

        testScheduler.triggerActions();

        subscriber.assertNoErrors();
        subscriber.assertValue(map -> map.size() == 3);
        subscriber.assertValue(map -> {
            boolean bool1 = false;
            boolean bool2 = false;
            boolean bool3 = false;
            for (Map.Entry<String, UserEntity> entry : map.entrySet()) {
                if (UID_USER_1.equals(entry.getKey()) && PHOTO_URL_1.equals(entry.getValue().getPhotoUrl())) {
                    bool1 = true;
                }
                if (UID_USER_2.equals(entry.getKey()) && PHOTO_URL_2.equals(entry.getValue().getPhotoUrl())) {
                    bool2 = true;
                }
                if (UID_USER_3.equals(entry.getKey()) && PHOTO_URL_3.equals(entry.getValue().getPhotoUrl())) {
                    bool3 = true;
                }
            }
            return bool1 && bool2 && bool3;
        });

        verify(firebaseUserRepository, times(1)).getUtilisateurByUid(UID_USER_1);
        verify(firebaseUserRepository, times(1)).getUtilisateurByUid(UID_USER_2);
        verify(firebaseUserRepository, times(1)).getUtilisateurByUid(UID_USER_3);

        resetMock();
    }
}