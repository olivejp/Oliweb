package oliweb.nc.oliweb;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;

import static oliweb.nc.oliweb.UtilityTest.checkCount;
import static oliweb.nc.oliweb.UtilityTest.waitTerminalEvent;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ChatRepositoryTest {

    private ChatRepository chatRepository;
    private Context appContext;

    @Before
    public void init() {
        appContext = ApplicationProvider.getApplicationContext();
        ContextModule contextModule = new ContextModule(appContext);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();
        chatRepository = component.getChatRepository();
        UtilityTest.cleanBase(appContext);
    }

    private void deleteAllTest() {
        TestObserver<Integer> subscriber = new TestObserver<>();
        chatRepository.deleteAll().subscribe(subscriber);
        waitTerminalEvent(subscriber, 5);
        subscriber.assertNoErrors();
        subscriber.dispose();
    }

    @Test
    public void insertThenCountThenDeleteAllThenCount() {
        deleteAllTest();

        // Create new chatEntity
        ChatEntity chat = new ChatEntity();
        chat.setUidAnnonce("uidAnnonce");
        chat.setUidSeller("uidSeller");
        chat.setUidBuyer("uidBuyer");
        chat.setStatusRemote(StatusRemote.TO_SEND);

        // Insert
        TestObserver<ChatEntity> listTestObserver = new TestObserver<>();
        chatRepository.singleSave(chat).subscribe(listTestObserver);
        waitTerminalEvent(listTestObserver, 5);
        listTestObserver.assertNoErrors();
        listTestObserver.dispose();

        // Count (should be 1)
        checkCount(1, chatRepository.count());

        // Erase all the database
        deleteAllTest();
        checkCount(0, chatRepository.count());
    }

    /**
     * Find by uid user and uid annonce
     */
    @Test
    public void findByUidUserAndUidAnnonceTest() {
        TestObserver<ChatEntity> listTestObserver = new TestObserver<>();
        chatRepository.findByUidUserAndUidAnnonce("123", "123").subscribe(listTestObserver);
        waitTerminalEvent(listTestObserver, 5);
        listTestObserver.assertNoErrors();
        listTestObserver.assertNoValues();
        listTestObserver.assertComplete();
        listTestObserver.dispose();
    }

    /**
     * Find by uid user and uid annonce
     */
    @Test
    public void findByIdTest() {
        TestObserver<ChatEntity> listTestObserver = new TestObserver<>();
        chatRepository.findById(1L).subscribe(listTestObserver);
        waitTerminalEvent(listTestObserver, 5);
        listTestObserver.assertNoErrors();
        listTestObserver.assertNoValues();
        listTestObserver.assertComplete();
        listTestObserver.dispose();
    }

    /**
     * Insert a ChatEntity
     */
    @Test
    public void insertThenUpdateTest() {
        // RAZ des chats
        deleteAllTest();

        // Create new chatEntity
        ChatEntity chat = new ChatEntity();
        chat.setUidAnnonce("uidAnnonce");
        chat.setUidSeller("uidSeller");
        chat.setUidBuyer("uidBuyer");
        chat.setStatusRemote(StatusRemote.TO_SEND);

        // Test Insert
        TestObserver<ChatEntity> testObserverInsert = new TestObserver<>();
        chatRepository.singleSave(chat).subscribe(testObserverInsert);
        waitTerminalEvent(testObserverInsert, 5);
        testObserverInsert.assertNoErrors();
        testObserverInsert.assertValue(chatEntity -> chatEntity.getUidAnnonce().equals("uidAnnonce") && chatEntity.getUidSeller().equals("uidSeller"));
        ChatEntity chatSaved = testObserverInsert.values().get(0);
        testObserverInsert.dispose();

        // Test Update
        chatSaved.setUidSeller("newUidSeller");
        TestObserver<ChatEntity> testObserverUpdate = new TestObserver<>();
        chatRepository.singleSave(chatSaved).subscribe(testObserverUpdate);
        waitTerminalEvent(testObserverUpdate, 5);
        testObserverUpdate.assertNoErrors();
        testObserverUpdate.assertValue(chatEntity -> chatSaved.getId().equals(chatEntity.getId()) && chatEntity.getUidSeller().equals("newUidSeller"));
        testObserverUpdate.dispose();
    }

    /**
     * Find by uid user and uid annonce
     */
    @Test
    public void insertThenDelete() {
        TestObserver<ChatEntity> listTestObserver = new TestObserver<>();
        chatRepository.findById(1L).subscribe(listTestObserver);
        waitTerminalEvent(listTestObserver, 5);
        listTestObserver.assertNoErrors();
        listTestObserver.assertNoValues();
        listTestObserver.assertComplete();
        listTestObserver.dispose();
    }
}
