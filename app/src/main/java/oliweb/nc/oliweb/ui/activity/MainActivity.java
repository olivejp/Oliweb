package oliweb.nc.oliweb.ui.activity;

import android.app.ActivityManager;
import android.app.SearchManager;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.service.sync.listener.DatabaseSyncListenerService;
import oliweb.nc.oliweb.service.sync.listener.FirebaseSyncListenerService;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;
import oliweb.nc.oliweb.ui.dialog.SortDialog;
import oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment;
import oliweb.nc.oliweb.ui.fragment.ListChatFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Utility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.FavoriteAnnonceActivity.ARG_USER_UID;
import static oliweb.nc.oliweb.ui.activity.MyAnnoncesActivity.ARG_UID_USER;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.ARG_ACTION_OPEN_CHATS;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.DATA_FIREBASE_USER_UID;
import static oliweb.nc.oliweb.ui.activity.PostAnnonceActivity.RC_POST_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.ProfilActivity.PROFIL_ACTIVITY_UID_USER;
import static oliweb.nc.oliweb.ui.activity.ProfilActivity.UPDATE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ACTION_MOST_RECENT;
import static oliweb.nc.oliweb.utility.Utility.DIALOG_FIREBASE_RETRIEVE;
import static oliweb.nc.oliweb.utility.Utility.sendNotificationToRetreiveData;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MainActivity extends AppCompatActivity
        implements NetworkReceiver.NetworkChangeListener, NavigationView.OnNavigationItemSelectedListener, NoticeDialogFragment.DialogListener, SortDialog.UpdateSortDialogListener {

    public static final int RC_SIGN_IN = 1001;

    private static final String TAG = MainActivity.class.getName();
    public static final String TAG_LIST_ANNONCE = "TAG_LIST_ANNONCE";
    public static final String SORT_DIALOG = "SORT_DIALOG";
    private static final String TAG_LIST_CHAT = "TAG_LIST_CHAT";
    private static final String SAVED_DYNAMIC_LINK_PROCESSED = "SAVED_DYNAMIC_LINK_PROCESSED";

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    SearchView searchView;

    private ImageView profileImage;
    private TextView profileName;
    private TextView profileEmail;
    private Menu navigationViewMenu;

    private LiveData<Integer> liveCountAllActiveAnnonce;
    private LiveData<Integer> liveCountAllFavorite;
    private LiveData<Integer> liveCountAllChat;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseDynamicLinks mFirebaseDynamicLinks;
    private FirebaseRemoteConfig mFirebaseConfig;

    private FirebaseUser mFirebaseUser;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private boolean questionHasBeenAsked;
    private MainActivityViewModel viewModel;
    private boolean dynamicLinkProcessed = false;

    private Intent intentLocalDbService;
    private Intent intentFirebaseDbService;

    private Observer<Integer> observeNumberAnnonceBadge = integer -> {
        TextView numberAnnoncesBadge = (TextView) navigationView.getMenu().findItem(R.id.nav_annonces).getActionView();
        numberAnnoncesBadge.setGravity(Gravity.CENTER_VERTICAL);
        numberAnnoncesBadge.setText(String.valueOf(integer));
    };

    private Observer<Integer> observeNumberFavoriteBadge = integer -> {
        TextView numberFavoriteBadge = (TextView) navigationView.getMenu().findItem(R.id.nav_favorites).getActionView();
        numberFavoriteBadge.setGravity(Gravity.CENTER_VERTICAL);
        numberFavoriteBadge.setText(String.valueOf(integer));
    };

    private Observer<Integer> observeNumberChatBadge = integer -> {
        TextView numberChatBadge = (TextView) navigationView.getMenu().findItem(R.id.nav_chats).getActionView();
        numberChatBadge.setGravity(Gravity.CENTER_VERTICAL);
        numberChatBadge.setText(String.valueOf(integer));
    };

    @Override
    protected void onStart() {
        super.onStart();
        catchDynamicLink();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDynamicLinks = FirebaseDynamicLinks.getInstance();
        mFirebaseConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build());

        // On va écouter le Broadcast Listener pour lancer le service de synchro uniquement dans le
        // cas où il y a du réseau.
        NetworkReceiver.getInstance().listen(this);

        initServicesIntents();

        initConfigDefaultValues();

        initViews();

        initFragments(savedInstanceState);
    }

    private void initServicesIntents() {
        intentLocalDbService = new Intent(getApplicationContext(), DatabaseSyncListenerService.class);
        intentFirebaseDbService = new Intent(getApplicationContext(), FirebaseSyncListenerService.class);
    }


    private void initConfigDefaultValues() {
        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put("column_number_portrait", 2);
        defaults.put("column_number_landscape", 3);
        mFirebaseConfig.setDefaults(defaults);
        final Task<Void> fetch = mFirebaseConfig.fetch(0);
        fetch.addOnSuccessListener(aVoid -> mFirebaseConfig.activateFetched());
    }

    private void initViews() {
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        View viewHeader = navigationView.getHeaderView(0);
        profileImage = viewHeader.findViewById(R.id.profileImage);
        profileName = viewHeader.findViewById(R.id.profileName);
        profileEmail = viewHeader.findViewById(R.id.profileEmail);
        navigationViewMenu = navigationView.getMenu();

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);
    }

    private void initFragments(Bundle savedInstanceState) {
        ListAnnonceFragment listAnnonceFragment;
        if (savedInstanceState != null && savedInstanceState.containsKey(TAG_LIST_ANNONCE)) {
            listAnnonceFragment = (ListAnnonceFragment) getSupportFragmentManager().getFragment(savedInstanceState, TAG_LIST_ANNONCE);
        } else {
            listAnnonceFragment = (ListAnnonceFragment) getSupportFragmentManager().findFragmentByTag(TAG_LIST_ANNONCE);
        }
        if (listAnnonceFragment == null) {
            listAnnonceFragment = ListAnnonceFragment.getInstance(null, ACTION_MOST_RECENT);
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, listAnnonceFragment, TAG_LIST_ANNONCE);
        transaction.commit();

        ListChatFragment listChatFragment;
        if (savedInstanceState != null && savedInstanceState.containsKey(TAG_LIST_CHAT)) {
            listChatFragment = (ListChatFragment) getSupportFragmentManager().getFragment(savedInstanceState, TAG_LIST_CHAT);
            getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, listChatFragment, TAG_LIST_CHAT).commit();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStackImmediate();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        if (searchManager != null && searchView != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.sorting) {
            SortDialog sortDialog = new SortDialog();
            sortDialog.show(getSupportFragmentManager(), SORT_DIALOG);
        }
        return true;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_connect) {
            signInSignOut();
        } else if (id == R.id.nav_settings) {
            callSettingActivity();
        } else if (id == R.id.nav_profile) {
            callProfilActivity();
        } else if (id == R.id.nav_favorites) {
            callFavoriteActivity();
        } else if (id == R.id.nav_chats) {
            callChatsActivity();
        } else if (id == R.id.nav_annonces) {
            callMyAnnoncesActivity();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void signInSignOut() {
        String uidUser = SharedPreferencesHelper.getInstance(getApplication()).getUidFirebaseUser();
        if (uidUser == null) {
            Utility.signIn(this, RC_SIGN_IN);
        } else {
            Utility.signOut(getApplicationContext());
        }
    }

    private void callMyAnnoncesActivity() {
        String uidUser = SharedPreferencesHelper.getInstance(getApplication()).getUidFirebaseUser();
        if (uidUser != null) {
            Intent intent = new Intent();
            intent.setClass(this, MyAnnoncesActivity.class);
            intent.putExtra(ARG_UID_USER, uidUser);
            startActivity(intent);
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        }
    }

    private void callChatsActivity() {
        String uidUser = SharedPreferencesHelper.getInstance(getApplication()).getUidFirebaseUser();
        if (uidUser != null) {
            Intent intent = new Intent(this, MyChatsActivity.class);
            intent.setAction(ARG_ACTION_OPEN_CHATS);
            intent.putExtra(DATA_FIREBASE_USER_UID, uidUser);
            startActivity(intent);
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        }
    }

    private void callSettingActivity() {
        Intent intent = new Intent();
        intent.setClass(this, SettingsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Connexion abandonnée", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == RC_POST_ANNONCE && resultCode == RESULT_OK) {
            Snackbar.make(toolbar, "Votre annonce a bien été sauvée.", Snackbar.LENGTH_LONG).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFirebaseAuth != null) {
            mAuthStateListener = defineAuthListener();
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mFirebaseAuth != null && mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    public void onDialogPositiveClick(NoticeDialogFragment dialog) {
        if (dialog.getTag() != null && dialog.getTag().equals(DIALOG_FIREBASE_RETRIEVE)) {
            SharedPreferencesHelper.getInstance(this).setRetrievePreviousAnnonces(false);
            SyncService.launchSynchroFromFirebase(MainActivity.this, mFirebaseUser.getUid());
            dialog.dismiss();
        }
    }

    @Override
    public void onDialogNegativeClick(NoticeDialogFragment dialog) {
        if (dialog.getTag() != null && dialog.getTag().equals(DIALOG_FIREBASE_RETRIEVE)) {
            SharedPreferencesHelper.getInstance(this).setRetrievePreviousAnnonces(false);
        }
        dialog.dismiss();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        ListAnnonceFragment listAnnonceFragment = (ListAnnonceFragment) getSupportFragmentManager().findFragmentByTag(TAG_LIST_ANNONCE);
        if (listAnnonceFragment != null) {
            getSupportFragmentManager().putFragment(outState, TAG_LIST_ANNONCE, listAnnonceFragment);
        }

        ListChatFragment listChatFragment = (ListChatFragment) getSupportFragmentManager().findFragmentByTag(TAG_LIST_CHAT);
        if (listChatFragment != null) {
            getSupportFragmentManager().putFragment(outState, TAG_LIST_CHAT, listChatFragment);
        }

        outState.putBoolean(SAVED_DYNAMIC_LINK_PROCESSED, dynamicLinkProcessed);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void sortHasBeenUpdated(int sort) {
        SharedPreferencesHelper.getInstance(getApplicationContext()).setPrefSort(sort);
        viewModel.updateSort(sort);
    }

    private void catchDynamicLink() {
        mFirebaseDynamicLinks.getDynamicLink(getIntent())
                .addOnSuccessListener(this, data -> {
                    if (data != null && data.getLink() != null && !dynamicLinkProcessed) {
                        Uri deepLink = data.getLink();
                        String uidAnnonce = deepLink.getLastPathSegment();
                        String from = deepLink.getQueryParameter("from");
                        if (uidAnnonce != null) {
                            dynamicLinkProcessed = true;
                            viewModel.getLiveFromFirebaseByUidAnnonce(uidAnnonce).observeOnce(annoncePhotos -> {
                                if (annoncePhotos != null) {
                                    callAnnonceDetailActivity(annoncePhotos);
                                } else {
                                    Toast.makeText(this, "Cette annonce n'existe plus", Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(this, e -> Log.w(TAG, "getDynamicLink:onFailure", e));
    }

    private void callAnnonceDetailActivity(AnnoncePhotos annoncePhotos) {
        Intent intent = new Intent(this, AnnonceDetailActivity.class);
        intent.putExtra(ARG_ANNONCE, annoncePhotos);
        startActivity(intent);
    }

    private void initViewsForThisUser(FirebaseUser user) {
        if (user == null) {
            return;
        }

        profileName.setText(user.getDisplayName());
        if (user.getEmail() != null) {
            profileEmail.setText(user.getEmail());
        }
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().toString().isEmpty()) {
            GlideApp.with(profileImage)
                    .load(user.getPhotoUrl())
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_person_white_48dp)
                    .into(profileImage);
        }

        // activeBadges doit être appelé après avoir renseigné l'UID du user dans les SharedPreferences
        activeBadges(user.getUid(), true);
    }

    private void activeBadges(String uidUser, boolean active) {
        if (active) {
            // On lance les observers pour récupérer les badges
            liveCountAllActiveAnnonce = viewModel.countAllAnnoncesByUser(uidUser, Utility.allStatusToAvoid());
            liveCountAllFavorite = viewModel.countAllFavoritesByUser(uidUser);
            liveCountAllChat = viewModel.countAllChatsByUser(uidUser, Utility.allStatusToAvoid());

            liveCountAllActiveAnnonce.removeObservers(this);
            liveCountAllFavorite.removeObservers(this);
            liveCountAllChat.removeObservers(this);

            liveCountAllActiveAnnonce.observe(this, observeNumberAnnonceBadge);
            liveCountAllFavorite.observe(this, observeNumberFavoriteBadge);
            liveCountAllChat.observe(this, observeNumberChatBadge);
        } else {
            // On stoppe les observers
            if (liveCountAllActiveAnnonce != null) {
                liveCountAllActiveAnnonce.removeObserver(observeNumberAnnonceBadge);
            }
            if (liveCountAllFavorite != null) {
                liveCountAllFavorite.removeObserver(observeNumberFavoriteBadge);
            }
            if (liveCountAllChat != null) {
                liveCountAllChat.removeObserver(observeNumberChatBadge);
            }

            TextView numberAnnoncesBadge = (TextView) navigationView.getMenu().findItem(R.id.nav_annonces).getActionView();
            TextView numberFavoriteBadge = (TextView) navigationView.getMenu().findItem(R.id.nav_favorites).getActionView();
            TextView numberChatBadge = (TextView) navigationView.getMenu().findItem(R.id.nav_chats).getActionView();
            numberAnnoncesBadge.setText(null);
            numberFavoriteBadge.setText(null);
            numberChatBadge.setText(null);
        }
    }

    private FirebaseAuth.AuthStateListener defineAuthListener() {
        return firebaseAuth -> {

            if (mFirebaseUser != null && mFirebaseUser == firebaseAuth.getCurrentUser()) return;

            mFirebaseUser = firebaseAuth.getCurrentUser();
            prepareNavigationMenu();
            if (mFirebaseUser != null) {

                // Sauvegarde de l'uid de l'utilisateur dans les préférences, dans le cas d'une déconnexion
                SharedPreferencesHelper.getInstance(getApplication()).setUidFirebaseUser(mFirebaseUser.getUid());
                viewModel.setFirebaseUser(mFirebaseUser);

                // Sauvegarde de l'utilisateur
                viewModel.saveUser(mFirebaseUser).observeOnce(atomicBoolean -> {
                    if (atomicBoolean != null && atomicBoolean.get()) {
                        Snackbar.make(navigationView, "Bienvenue sur Oliweb", Snackbar.LENGTH_LONG).setAction("Voir mon profil", v -> callProfilActivity()).show();
                    } else {
                        Toast.makeText(this, String.format("Content de vous revoir %s", mFirebaseUser.getDisplayName()), Toast.LENGTH_LONG).show();
                    }
                });

                initViewsForThisUser(mFirebaseUser);

                if (SharedPreferencesHelper.getInstance(this).getRetrievePreviousAnnonces()) {
                    viewModel.shouldIAskQuestionToRetrieveData(mFirebaseUser.getUid()).observe(this, atomicBoolean -> {
                        if (atomicBoolean != null && atomicBoolean.get() && !questionHasBeenAsked) {
                            questionHasBeenAsked = true;
                            viewModel.shouldIAskQuestionToRetrieveData(null).removeObservers(this);
                            sendNotificationToRetreiveData(getSupportFragmentManager(), this);
                        }
                    });
                    questionHasBeenAsked = false;
                }
            } else {
                // activeBadges doit être appelé avant de supprimer l'UID du user dans les SharedPreferences
                activeBadges(SharedPreferencesHelper.getInstance(getApplicationContext()).getUidFirebaseUser(), false);
                profileName.setText(null);
                profileEmail.setText(null);
                mFirebaseUser = null;
                profileImage.setImageResource(R.drawable.ic_person_white_48dp);
                SharedPreferencesHelper.getInstance(this).setUidFirebaseUser(null);
                viewModel.shouldIAskQuestionToRetrieveData(null).removeObservers(this);
            }

            startStopServices();
        };
    }

    private void prepareNavigationMenu() {
        navigationView.getMenu().findItem(R.id.nav_annonces).setEnabled(mFirebaseUser != null);
        navigationView.getMenu().findItem(R.id.nav_profile).setEnabled(mFirebaseUser != null);
        navigationView.getMenu().findItem(R.id.nav_favorites).setEnabled(mFirebaseUser != null);
        navigationView.getMenu().findItem(R.id.nav_chats).setEnabled(mFirebaseUser != null);
        navigationViewMenu.findItem(R.id.nav_connect).setTitle((mFirebaseUser != null) ? getString(R.string.sign_out) : getString(R.string.sign_in));
    }

    private void callProfilActivity() {
        String uidUser = SharedPreferencesHelper.getInstance(getApplication()).getUidFirebaseUser();
        if (uidUser != null) {
            Intent intent = new Intent();
            intent.setClass(this, ProfilActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(PROFIL_ACTIVITY_UID_USER, uidUser);
            bundle.putBoolean(UPDATE, true);
            intent.putExtras(bundle);
            startActivity(intent);
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        }
    }

    private void callFavoriteActivity() {
        String uidUser = SharedPreferencesHelper.getInstance(getApplication()).getUidFirebaseUser();
        if (uidUser != null) {
            Intent intent = new Intent();
            intent.setClass(this, FavoriteAnnonceActivity.class);
            intent.putExtra(ARG_USER_UID, uidUser);
            startActivity(intent);
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        }
    }

    private void startStopServices() {
        stopAllServices();
        if (mFirebaseUser != null && NetworkReceiver.checkConnection(this)) {
            launchServices(mFirebaseUser.getUid());
        }
    }

    /**
     * Lancement des services de synchronisation
     *
     * @param uidUser de l'utilisateur à connecter
     */
    private void launchServices(String uidUser) {

        stopServicesIfRunning();

        // Lancement du service pour écouter la DB en local
        intentLocalDbService.putExtra(DatabaseSyncListenerService.CHAT_SYNC_UID_USER, uidUser);
        startService(intentLocalDbService);

        // Lancement du service pour écouter Firebase
        intentFirebaseDbService.putExtra(FirebaseSyncListenerService.CHAT_SYNC_UID_USER, uidUser);
        startService(intentFirebaseDbService);
    }

    private void stopServicesIfRunning() {
        if (isServiceRunning("oliweb.nc.oliweb.service.sync.listener.DatabaseSyncListenerService")) {
            stopService(intentLocalDbService);
        }

        if (isServiceRunning("oliweb.nc.oliweb.service.sync.listener.FirebaseSyncListenerService")) {
            stopService(intentFirebaseDbService);
        }
    }

    private boolean isServiceRunning(String packageNameService) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        assert manager != null;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (packageNameService.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stoppe les services de synchronisation
     */
    private void stopAllServices() {
        // Stop the Local DB sync service
        stopService(intentLocalDbService);

        // Stop the Firebase sync service
        stopService(intentFirebaseDbService);
    }

    @Override
    public void onNetworkEnable() {
        startStopServices();
    }

    @Override
    public void onNetworkDisable() {
        stopAllServices();
    }
}
