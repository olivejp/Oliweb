package oliweb.nc.oliweb;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.reactivex.observers.TestObserver;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
import oliweb.nc.oliweb.utility.UtilityTest;

import static oliweb.nc.oliweb.utility.UtilityTest.checkCount;
import static oliweb.nc.oliweb.utility.UtilityTest.waitTerminalEvent;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ChatRepositoryTest {

    private ChatRepository chatRepository;


    @Before
    public void init() {
        Context appContext = InstrumentationRegistry.getTargetContext();
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
