package oliweb.nc.oliweb.ui.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.fragment.ListChatFragment;
import oliweb.nc.oliweb.ui.fragment.ListMessageFragment;

import static junit.framework.Assert.assertNotNull;
import static oliweb.nc.oliweb.service.sync.SyncService.ARG_ACTION_SEND_DIRECT_MESSAGE_UID_CHAT;
import static oliweb.nc.oliweb.service.sync.SyncService.ARG_ACTION_SEND_DIRECT_UID_USER;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MyChatsActivity extends AppCompatActivity {

    private static final String TAG = MyChatsActivity.class.getName();

    public static final String TAG_MASTER_FRAGMENT = "TAG_MASTER_FRAGMENT";
    public static final String TAG_DETAIL_FRAGMENT = "TAG_DETAIL_FRAGMENT";

    public static final String ARG_ACTION_OPEN_CHATS = "ARG_ACTION_OPEN_CHATS";
    public static final String ARG_ACTION_SEND_DIRECT_MESSAGE = "ARG_ACTION_SEND_DIRECT_MESSAGE";
    public static final String ARG_ACTION_FRAGMENT_MESSAGE = "ARG_ACTION_FRAGMENT_MESSAGE";

    public static final String ARG_ANNONCE = "ARG_ANNONCE";

    public static final String DATA_FIREBASE_USER_UID = "DATA_FIREBASE_USER_UID";

    private MyChatsActivityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() == null || getIntent().getAction() == null || getIntent().getAction().isEmpty()) {
            Log.e(TAG, "No intent or no action to open MyChatsActivity", new RuntimeException());
            finish();
        }

        String action = getIntent().getAction();
        viewModel = ViewModelProviders.of(this).get(MyChatsActivityViewModel.class);
        viewModel.setTwoPane(findViewById(R.id.frame_messages) != null);
        setContentView(R.layout.activity_my_chats);

        if (ARG_ACTION_OPEN_CHATS.equals(action)) {
            assertNotNull(String.format("Need extras named %s containing the uid user", DATA_FIREBASE_USER_UID), getIntent().getExtras());
            assertNotNull(String.format("Extra %s can't be null", DATA_FIREBASE_USER_UID), getIntent().getExtras().get(DATA_FIREBASE_USER_UID));
            String argUidUser = getIntent().getStringExtra(DATA_FIREBASE_USER_UID);
            viewModel.setFirebaseUserUid(argUidUser);
            initFragments();
        } else if (ARG_ACTION_SEND_DIRECT_MESSAGE.equals(action)) {
            assertNotNull(String.format("Need extras named %s containing the uid user and %s containing the uid chat", ARG_ACTION_SEND_DIRECT_UID_USER, ARG_ACTION_SEND_DIRECT_MESSAGE_UID_CHAT), getIntent().getExtras());
            assertNotNull(String.format("Extra %s can't be null", ARG_ACTION_SEND_DIRECT_UID_USER), getIntent().getExtras().get(ARG_ACTION_SEND_DIRECT_UID_USER));
            assertNotNull(String.format("Extra %s can't be null", ARG_ACTION_SEND_DIRECT_MESSAGE_UID_CHAT), getIntent().getExtras().get(ARG_ACTION_SEND_DIRECT_MESSAGE_UID_CHAT));
            String argUidUser = getIntent().getStringExtra(ARG_ACTION_SEND_DIRECT_UID_USER);
            String argUidChat = getIntent().getStringExtra(ARG_ACTION_SEND_DIRECT_MESSAGE_UID_CHAT);
            viewModel.setFirebaseUserUid(argUidUser);
            viewModel.rechercheMessageByUidChat(argUidChat);
            initFragments();
        } else if (ARG_ACTION_FRAGMENT_MESSAGE.equals(action)) {
            assertNotNull(String.format("Need extras named %s containing an AnnonceEntity and %s containing the uid of the connected user", ARG_ANNONCE, DATA_FIREBASE_USER_UID), getIntent().getExtras());
            assertNotNull(String.format("Extra %s can't be null", ARG_ANNONCE), getIntent().getExtras().get(ARG_ANNONCE));
            assertNotNull(String.format("Extra %s can't be null", DATA_FIREBASE_USER_UID), getIntent().getExtras().get(DATA_FIREBASE_USER_UID));
            AnnonceEntity annonce = getIntent().getExtras().getParcelable(ARG_ANNONCE);
            String argUidUser = getIntent().getStringExtra(DATA_FIREBASE_USER_UID);
            viewModel.setFirebaseUserUid(argUidUser);
            viewModel.rechercheMessageByAnnonce(annonce);
            setTitle(annonce.getTitre());
            ListMessageFragment listMessageFragment = new ListMessageFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_chats, listMessageFragment, TAG_MASTER_FRAGMENT).commit();
        } else {
            Log.e(TAG, String.format("%s is not an available actions", action), new RuntimeException());
            finish();
        }

        // Récupération d'une map avec tous les UID des personnes qui correspondent avec moi.
        viewModel.getPhotoUrlsByUidUser();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        }
    }

    private void initFragments() {
        // Récupération du fragment en cours dans frame_chats
        Fragment frag = getSupportFragmentManager().findFragmentById(R.id.frame_chats);

        Fragment listChatFragment = getSupportFragmentManager().findFragmentByTag(TAG_MASTER_FRAGMENT);
        if (listChatFragment == null) {
            listChatFragment = new ListChatFragment();
        }
        if (frag == null || !frag.equals(listChatFragment)) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_chats, listChatFragment, TAG_MASTER_FRAGMENT)
                    .commit();
        }

        Fragment listMessageFragment1 = getSupportFragmentManager().findFragmentByTag(TAG_DETAIL_FRAGMENT);
        if (listMessageFragment1 != null) {
            getSupportFragmentManager().beginTransaction().remove(listMessageFragment1).commit();
            ListMessageFragment listMessageFragment = new ListMessageFragment();
            if (viewModel.isTwoPane()) {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_messages, listMessageFragment, TAG_DETAIL_FRAGMENT).commit();
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_chats, listMessageFragment, TAG_DETAIL_FRAGMENT).addToBackStack(null).commit();
            }
        }
    }
}
