package oliweb.nc.oliweb.ui.activity;

import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.fragment.ListChatFragment;
import oliweb.nc.oliweb.ui.fragment.ListMessageFragment;
import oliweb.nc.oliweb.utility.ArgumentsChecker;

import static oliweb.nc.oliweb.service.sync.SyncService.ARG_UID_CHAT;
import static oliweb.nc.oliweb.service.sync.SyncService.ARG_UID_USER;

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

        String action = getIntent().getAction();
        Bundle args = getIntent().getExtras();
        ArgumentsChecker argumentsChecker = new ArgumentsChecker();
        argumentsChecker.setArguments(args)
                .isMandatoryWithCondition(DATA_FIREBASE_USER_UID, bundle -> ARG_ACTION_OPEN_CHATS.equals(action))
                .isMandatoryWithCondition(ARG_UID_USER, bundle -> ARG_ACTION_SEND_DIRECT_MESSAGE.equals(action))
                .isMandatoryWithCondition(ARG_UID_CHAT, bundle -> ARG_ACTION_SEND_DIRECT_MESSAGE.equals(action))
                .isMandatoryWithCondition(ARG_ANNONCE, bundle -> ARG_ACTION_FRAGMENT_MESSAGE.equals(action))
                .isMandatoryWithCondition(DATA_FIREBASE_USER_UID, bundle -> ARG_ACTION_FRAGMENT_MESSAGE.equals(action))
                .setOnFailureListener(e -> finish())
                .check();

        viewModel = ViewModelProviders.of(this).get(MyChatsActivityViewModel.class);
        setContentView(R.layout.activity_my_chats);
        viewModel.setTwoPane(findViewById(R.id.frame_messages) != null);

        if (ARG_ACTION_OPEN_CHATS.equals(action)) {
            String argUidUser = getIntent().getStringExtra(DATA_FIREBASE_USER_UID);
            viewModel.setFirebaseUserUid(argUidUser);
            initFragments();
        } else if (ARG_ACTION_SEND_DIRECT_MESSAGE.equals(action)) {
            String argUidUser = getIntent().getStringExtra(ARG_UID_USER);
            String argUidChat = getIntent().getStringExtra(ARG_UID_CHAT);
            viewModel.setFirebaseUserUid(argUidUser);
            viewModel.rechercheMessageByUidChat(argUidChat);
            initFragments();
        } else if (ARG_ACTION_FRAGMENT_MESSAGE.equals(action)) {
            AnnonceEntity annonce = args.getParcelable(ARG_ANNONCE);
            String argUidUser = getIntent().getStringExtra(DATA_FIREBASE_USER_UID);
            viewModel.setFirebaseUserUid(argUidUser);
            viewModel.rechercheMessageByAnnonce(annonce);
            setTitle(annonce.getTitre());
            ListMessageFragment listMessageFragment = new ListMessageFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_chats, listMessageFragment, TAG_MASTER_FRAGMENT).commit();
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
            getSupportFragmentManager().executePendingTransactions();
            if (viewModel.isTwoPane()) {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_messages, listMessageFragment1, TAG_DETAIL_FRAGMENT).commit();
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.frame_chats, listMessageFragment1, TAG_DETAIL_FRAGMENT).addToBackStack(null).commit();
            }
        }
    }
}
