package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.service.sync.ChatSyncService;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;
import oliweb.nc.oliweb.ui.dialog.SortDialog;
import oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment;
import oliweb.nc.oliweb.ui.fragment.ListChatFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Utility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static oliweb.nc.oliweb.service.sync.ChatSyncService.CHAT_SYNC_UID_USER;
import static oliweb.nc.oliweb.ui.activity.PostAnnonceActivity.RC_POST_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.ProfilActivity.PROFIL_ACTIVITY_UID_USER;
import static oliweb.nc.oliweb.ui.activity.ProfilActivity.UPDATE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ACTION_FAVORITE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ACTION_MOST_RECENT;
import static oliweb.nc.oliweb.utility.Utility.DIALOG_FIREBASE_RETRIEVE;
import static oliweb.nc.oliweb.utility.Utility.sendNotificationToRetreiveData;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, NoticeDialogFragment.DialogListener, SortDialog.UpdateSortDialogListener {

    public static final int RC_SIGN_IN = 1001;

    private static final String TAG = MainActivity.class.getName();
    public static final String TAG_LIST_ANNONCE = "TAG_LIST_ANNONCE";
    public static final String SORT_DIALOG = "SORT_DIALOG";
    private static final String TAG_LIST_CHAT = "TAG_LIST_CHAT";

    private LiveData<Integer> liveCountAllActiveAnnonce;
    private LiveData<Integer> liveCountAllFavorite;
    private LiveData<Integer> liveCountAllChat;


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

    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private boolean questionHasBeenAsked;
    private MainActivityViewModel viewModel;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        View viewHeader = navigationView.getHeaderView(0);
        profileImage = viewHeader.findViewById(R.id.profileImage);
        profileName = viewHeader.findViewById(R.id.profileName);
        profileEmail = viewHeader.findViewById(R.id.profileEmail);
        navigationViewMenu = navigationView.getMenu();

        mFirebaseAuth = FirebaseAuth.getInstance();
        defineAuthListener();

        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Init most recent annonce fragment
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

        // Init Chat Fragment
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

        // On attache la searchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
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
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_connect) {
            if (mFirebaseUser == null) {
                signIn(RC_SIGN_IN);
            } else {
                signOut();
            }
        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent();
            intent.setClass(this, SettingsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        } else if (id == R.id.nav_profile) {
            Intent intent = new Intent();
            intent.setClass(this, ProfilActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString(PROFIL_ACTIVITY_UID_USER, mFirebaseUser.getUid());
            bundle.putBoolean(UPDATE, true);
            intent.putExtras(bundle);
            startActivity(intent);
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        } else if (id == R.id.nav_favorites) {
            callFavoriteFragment();
        } else if (id == R.id.nav_chats) {

            // On lance l'activité des conversations.
            Intent intent = new Intent(this, MyChatsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);

        } else if (id == R.id.nav_annonces) {
            Intent intent = new Intent();
            intent.setClass(this, MyAnnoncesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Remise à blanc des champs spécifiques à la connexion
     */
    private void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    SharedPreferencesHelper.getInstance(MainActivity.this).setUidFirebaseUser(null);
                    Toast.makeText(this, "Vous êtes déconnecté", Toast.LENGTH_SHORT).show();
                });

    }

    private void signIn(int requestCode) {
        if (NetworkReceiver.checkConnection(this)) {
            signOut();
            Utility.callLoginUi(this, requestCode);
        } else {
            Snackbar.make(navigationView, "Une connexion est requise pour se connecter", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Authentification simple
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Connexion abandonnée", Toast.LENGTH_SHORT).show();
            }
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bienvenue", Toast.LENGTH_LONG).show();
            }
        }

        // On vient de créer une nouvelle annonce.
        // On envoie un snackbar pour avertir l'utilisateur que cela s'est bien déroulé.
        if (requestCode == RC_POST_ANNONCE && resultCode == RESULT_OK) {
            Snackbar.make(toolbar, "Votre annonce a bien été sauvée.", Snackbar.LENGTH_LONG).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mFirebaseAuth != null) {
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
                    .placeholder(R.drawable.ic_person_white_48dp)
                    .into(profileImage);
        }

        // activeBadges doit être appelé après avoir renseigné l'UID du user dans les SharedPreferences
        activeBadges(user.getUid(), true);
    }

    private void activeBadges(String uid, boolean active) {
        if (active) {
            // On lance les observers pour récupérer les badges
            liveCountAllActiveAnnonce = viewModel.countAllAnnoncesByUser(uid, Utility.allStatusToAvoid());
            liveCountAllFavorite = viewModel.countAllFavoritesByUser(uid);
            liveCountAllChat = viewModel.countAllChatsByUser(uid, Utility.allStatusToAvoid());

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

    private void defineAuthListener() {
        mAuthStateListener = firebaseAuth -> {
            if ((mFirebaseUser != null && mFirebaseUser == firebaseAuth.getCurrentUser())) {
                return;
            }

            mFirebaseUser = firebaseAuth.getCurrentUser();
            prepareNavigationMenu();
            if (mFirebaseUser != null) {

                // Sauvegarde dans les préférences, dans le cas d'une déconnexion
                SharedPreferencesHelper.getInstance(getApplication()).setUidFirebaseUser(mFirebaseUser.getUid());

                viewModel.saveUser(mFirebaseUser);
                initViewsForThisUser(mFirebaseUser);

                if (SharedPreferencesHelper.getInstance(this).getRetrievePreviousAnnonces()) {
                    viewModel.shouldIAskQuestionToRetreiveData(mFirebaseUser.getUid()).observe(this, atomicBoolean -> {
                        if (atomicBoolean != null && atomicBoolean.get() && !questionHasBeenAsked) {
                            questionHasBeenAsked = true;
                            viewModel.shouldIAskQuestionToRetreiveData(null).removeObservers(this);
                            sendNotificationToRetreiveData(getSupportFragmentManager(), this);
                        }
                    });
                    questionHasBeenAsked = false;
                }

                // Lancement du service d'écoute pour toutes les données de cet utilisateur
                // use this to start and trigger a service
                Intent intent = new Intent(getApplicationContext(), ChatSyncService.class);
                intent.putExtra(CHAT_SYNC_UID_USER, mFirebaseUser.getUid());
                getApplicationContext().startService(intent);
            } else {
                // activeBadges doit être appelé avant de supprimer l'UID du user dans les SharedPreferences
                activeBadges(SharedPreferencesHelper.getInstance(getApplicationContext()).getUidFirebaseUser(), false);
                profileName.setText(null);
                profileEmail.setText(null);
                mFirebaseUser = null;
                profileImage.setImageResource(R.drawable.ic_person_white_48dp);
                SharedPreferencesHelper.getInstance(this).setUidFirebaseUser(null);
                viewModel.shouldIAskQuestionToRetreiveData(null).removeObservers(this);

                // Termine le service d'écoute
                Intent intent = new Intent(getApplicationContext(), ChatSyncService.class);
                stopService(intent);
            }
        };
    }

    private void prepareNavigationMenu() {
        navigationView.getMenu().findItem(R.id.nav_annonces).setEnabled(mFirebaseUser != null);
        navigationView.getMenu().findItem(R.id.nav_profile).setEnabled(mFirebaseUser != null);
        navigationView.getMenu().findItem(R.id.nav_favorites).setEnabled(mFirebaseUser != null);
        navigationView.getMenu().findItem(R.id.nav_chats).setEnabled(mFirebaseUser != null);
        navigationViewMenu.findItem(R.id.nav_connect).setTitle((mFirebaseUser != null) ? "Se déconnecter" : "Se connecter");
    }

    private void callFavoriteFragment() {
        ListAnnonceFragment listAnnonceFragment = ListAnnonceFragment.getInstance(mFirebaseUser.getUid(), ACTION_FAVORITE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_frame, listAnnonceFragment)
                .addToBackStack(null)
                .commit();
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
        super.onSaveInstanceState(outState);
    }

    @Override
    public void sortHasBeenUpdated(int sort) {
        SharedPreferencesHelper.getInstance(getApplicationContext()).setPrefSort(sort);
        viewModel.updateSort(sort);
    }
}
