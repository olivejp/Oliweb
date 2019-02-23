package oliweb.nc.oliweb.ui.activity;

import android.Manifest;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.Collections;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.ShareCompat;
import androidx.core.util.Pair;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import oliweb.nc.oliweb.BuildConfig;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.service.firebase.DynamicLinkService;
import oliweb.nc.oliweb.service.notification.MyFirebaseMessagingService;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.dialog.ListAnnonceBottomSheetDialog;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;
import oliweb.nc.oliweb.ui.dialog.SortDialog;
import oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment;
import oliweb.nc.oliweb.ui.fragment.ListCategorieFragment;
import oliweb.nc.oliweb.ui.fragment.ListChatFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.MyAnnoncesActivity.ACTION_CODE_POST;
import static oliweb.nc.oliweb.ui.activity.MyAnnoncesActivity.ARG_UID_USER;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.ARG_ACTION_OPEN_CHATS;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.DATA_FIREBASE_USER_UID;
import static oliweb.nc.oliweb.ui.activity.PostAnnonceActivity.RC_POST_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.ProfilActivity.PROFIL_ACTIVITY_UID_USER;
import static oliweb.nc.oliweb.ui.activity.ProfilActivity.UPDATE;
import static oliweb.nc.oliweb.ui.fragment.ListCategorieFragment.ID_ALL_CATEGORY;
import static oliweb.nc.oliweb.utility.Constants.DEFAULT_MAX_IMG_PIXEL_SIZE;
import static oliweb.nc.oliweb.utility.Constants.DEFAULT_NUMBER_PICTURES;
import static oliweb.nc.oliweb.utility.Constants.EMAIL_ADMIN;
import static oliweb.nc.oliweb.utility.Constants.MAIL_MESSAGE_TYPE;
import static oliweb.nc.oliweb.utility.Constants.REMOTE_DECREASE_JPEG_QUALITY_DEFAULT;
import static oliweb.nc.oliweb.utility.Constants.REMOTE_DELAY_DEFAULT;
import static oliweb.nc.oliweb.utility.Utility.DIALOG_FIREBASE_RETRIEVE;
import static oliweb.nc.oliweb.utility.Utility.callLoginUi;
import static oliweb.nc.oliweb.utility.Utility.sendNotificationToRetrieveData;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MainActivity extends AppCompatActivity
        implements NetworkReceiver.NetworkChangeListener, NavigationView.OnNavigationItemSelectedListener, NoticeDialogFragment.DialogListener, SortDialog.UpdateSortDialogListener, SnackbarViewProvider, ListAnnonceBottomSheetDialog.BottomSheetDialogListener {

    private static final String TAG = MainActivity.class.getName();

    private static final int REQUEST_WRITE_EXTERNAL_PERMISSION_CODE = 101;
    private static final String LOADING_DIALOG = "LOADING_DIALOG";
    public static final int RC_SIGN_IN = 1001;
    public static final int RC_SIGN_IN_TO_POST = 1002;
    public static final String TAG_LIST_ANNONCE = "TAG_LIST_ANNONCE";
    public static final String TAG_LIST_CATEGORY = "TAG_LIST_CATEGORY";
    public static final String SORT_DIALOG = "SORT_DIALOG";
    public static final String ACTION_REDIRECT = "ACTION_REDIRECT";
    public static final String ACTION_CHAT = "ACTION_CHAT";
    public static final String ACTION_ANNONCE = "ACTION_ANNONCE";

    private static final String TAG_LIST_CHAT = "TAG_LIST_CHAT";
    private static final String SAVED_DYNAMIC_LINK_PROCESSED = "SAVED_DYNAMIC_LINK_PROCESSED";
    private static final String SAVED_ANNONCE_FAVORITE = "SAVED_ANNONCE_FAVORITE";

    @BindView(R.id.appbarlayout)
    AppBarLayout appBarLayout;

    @BindView(R.id.main_coordinator_layout)
    CoordinatorLayout coordinatorLayout;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.frame_category_list)
    FrameLayout frameCategoryList;

    @BindView(R.id.main_activity_bar)
    BottomAppBar bottomAppBar;

    SearchView searchView;

    private ConstraintLayout constraintProfil;
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

    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private boolean questionHasBeenAsked;
    private MainActivityViewModel viewModel;
    private boolean dynamicLinkProcessed = false;
    private boolean shouldCallAddAnnonce = false;
    private AnnonceFull annonceFullToSaveTofavorite;

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

    private ActionBarDrawerToggle toggle;

    private Bundle mBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Récupération du viewModel
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        // Get instances of Firebase tooling
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDynamicLinks = FirebaseDynamicLinks.getInstance();
        mFirebaseConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(true)
                .build());

        // Instanciation de notre vue
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Sauvegarde du bundle pour l'utiliser dans le onStart()
        mBundle = savedInstanceState;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Récupération des liens dynamiques
        catchDynamicLink();

        // On va écouter le Broadcast Listener pour lancer le service de synchro uniquement dans le cas où il y a du réseau.
        NetworkReceiver.listen(this);
        viewModel.setIsNetworkAvailable(NetworkReceiver.checkConnection(this));

        // Récupération dans le config remote de toutes les variables configurables
        initRemoteConfigDefaultValues();

        // Récupération de la liste des catégories dans Firebase (si on a une connexion)
        if (NetworkReceiver.checkConnection(this))
            viewModel.checkRemoteCategoriesEqualToLocalCategories();

        // Init des widgets sur l'écran
        initViews();

        // Initialisation des fragments si il y en avait
        initFragments(mBundle);

        // Vérifie les actions passées dans l'Intent.
        checkAction(getIntent());
    }

    private void checkAction(Intent intent) {
        // Recherche d'une action pour rediriger vers une activité
        if (ACTION_CHAT.equals(intent.getStringExtra(ACTION_REDIRECT)) && mFirebaseAuth.getCurrentUser() != null) {
            callChatsActivity();
        }

        // L'action demandée est une redirection vers la fiche annonce
        if (ACTION_ANNONCE.equals(intent.getStringExtra(ACTION_REDIRECT))) {
            String uidAnnonce = intent.getStringExtra(MyFirebaseMessagingService.KEY_UID_ANNONCE);
            String uidAuthor = intent.getStringExtra(MyFirebaseMessagingService.KEY_UID_AUTHOR);
            viewModel.getLiveFromFirebaseByUidAnnonce(uidAnnonce).observeOnce(annoncePhotos -> {
                if (annoncePhotos != null) {
                    callAnnonceDetailActivity(annoncePhotos);
                } else {
                    Toast.makeText(this, R.string.AD_DONT_EXIST_ANYMORE, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void initRemoteConfigDefaultValues() {
        HashMap<String, Object> defaults = new HashMap<>();
        defaults.put(Constants.REMOTE_COLUMN_NUMBER, 1);
        defaults.put(Constants.REMOTE_COLUMN_NUMBER_LANDSCAPE, 2);
        defaults.put(Constants.REMOTE_IMAGE_RESOLUTION_RESIZE, DEFAULT_MAX_IMG_PIXEL_SIZE);
        defaults.put(Constants.REMOTE_NUMBER_PICTURES, DEFAULT_NUMBER_PICTURES);
        defaults.put(Constants.REMOTE_DELAY, REMOTE_DELAY_DEFAULT);
        defaults.put(Constants.REMOTE_DECREASE_JPEG_QUALITY, REMOTE_DECREASE_JPEG_QUALITY_DEFAULT);
        mFirebaseConfig.setDefaults(defaults);
        final Task<Void> fetch = mFirebaseConfig.fetch(0);
        fetch.addOnSuccessListener(this, aVoid -> mFirebaseConfig.activateFetched());
    }

    private void initViews() {
        View viewHeader = navigationView.getHeaderView(0);

        TextView headerVersion = viewHeader.findViewById(R.id.nav_header_version);
        constraintProfil = viewHeader.findViewById(R.id.constraint_nav_profile);
        profileImage = viewHeader.findViewById(R.id.profileImage);
        profileName = viewHeader.findViewById(R.id.profileName);
        profileEmail = viewHeader.findViewById(R.id.profileEmail);

        headerVersion.setText(BuildConfig.VERSION_NAME);
        profileImage.setOnClickListener(v -> {
            if (mFirebaseAuth.getUid() != null) {
                callProfilActivity();
            }
        });

        setSupportActionBar(bottomAppBar);

        toggle = new ActionBarDrawerToggle(this, drawer, bottomAppBar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        navigationViewMenu = navigationView.getMenu();
        navigationView.setNavigationItemSelectedListener(this);
        prepareNavigationMenu(false);

        // Observe les changements effectués sur cet utilisateur
        viewModel.getLiveUserConnected().observe(this, this::initViewsForThisUser);
    }

    private void initFragments(Bundle savedInstanceState) {
        // Fragment liste catégorie
        ListCategorieFragment listCategorieFragment;
        if (savedInstanceState != null && savedInstanceState.containsKey(TAG_LIST_CATEGORY)) {
            listCategorieFragment = (ListCategorieFragment) getSupportFragmentManager().getFragment(savedInstanceState, TAG_LIST_CATEGORY);
        } else {
            listCategorieFragment = (ListCategorieFragment) getSupportFragmentManager().findFragmentByTag(TAG_LIST_CATEGORY);
        }
        if (listCategorieFragment == null) {
            createListCategoryFragment();
        } else {
            FragmentTransaction transactionListeCategorie = getSupportFragmentManager().beginTransaction().replace(R.id.frame_category_list, listCategorieFragment, TAG_LIST_CATEGORY);
            transactionListeCategorie.commit();
        }


        // Fragment liste annonce
        ListAnnonceFragment listAnnonceFragment;
        if (savedInstanceState != null && savedInstanceState.containsKey(TAG_LIST_ANNONCE)) {
            listAnnonceFragment = (ListAnnonceFragment) getSupportFragmentManager().getFragment(savedInstanceState, TAG_LIST_ANNONCE);
        } else {
            listAnnonceFragment = (ListAnnonceFragment) getSupportFragmentManager().findFragmentByTag(TAG_LIST_ANNONCE);
        }
        if (listAnnonceFragment == null) {
            listAnnonceFragment = new ListAnnonceFragment();
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, listAnnonceFragment, TAG_LIST_ANNONCE);
        transaction.commit();

        // Fragment liste de chats (TODO voir si encore utilisé)
        ListChatFragment listChatFragment;
        if (savedInstanceState != null && savedInstanceState.containsKey(TAG_LIST_CHAT)) {
            listChatFragment = (ListChatFragment) getSupportFragmentManager().getFragment(savedInstanceState, TAG_LIST_CHAT);
            if (listChatFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.main_frame, listChatFragment, TAG_LIST_CHAT).commit();
            }
        }
    }

    private void createListCategoryFragment() {
        viewModel.getLiveListCategory().observe(this, categorieEntities -> {
            if (categorieEntities == null || categorieEntities.isEmpty()) return;

            // Ajout d'une catégorie, dans la liste et sélection de cette catégorie par défaut
            CategorieEntity cat = new CategorieEntity();
            cat.setIdCategorie(ID_ALL_CATEGORY);
            cat.setName(getString(R.string.all_categories));
            categorieEntities.add(0, cat);
            viewModel.setCategorySelected(cat);

            ListCategorieFragment nouveauFragment = ListCategorieFragment.newInstance(categorieEntities);
            FragmentTransaction transactionListeCategorie = getSupportFragmentManager().beginTransaction().replace(R.id.frame_category_list, nouveauFragment, TAG_LIST_CATEGORY);
            transactionListeCategorie.commit();
        });
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
        if (item.getItemId() == android.R.id.home) {
            return true;
        }
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
        } else if (id == R.id.nav_favorites) {
            callFavoriteActivity();
        } else if (id == R.id.nav_chats) {
            callChatsActivity();
        } else if (id == R.id.nav_annonces) {
            callMyAnnoncesActivity(null);
        } else if (id == R.id.nav_advanced_search) {
            callAdvancedSearchActivity();
        } else if (id == R.id.nav_suggestion) {
            callSuggestionActivity();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void callSuggestionActivity() {
        ShareCompat.IntentBuilder.from(this)
                .setType(MAIL_MESSAGE_TYPE)
                .addEmailTo(EMAIL_ADMIN)
                .setSubject(getString(R.string.app_name) + " - Suggestion d'amélioration")
                .setText(getString(R.string.default_mail_suggestion_message))
                .setChooserTitle(R.string.default_mail_chooser_title)
                .startChooser();
    }

    private void signInSignOut() {
        String uidUser = SharedPreferencesHelper.getInstance(getApplication()).getUidFirebaseUser();
        if (uidUser == null) {
            Utility.signIn(this, RC_SIGN_IN);
        } else {
            Utility.signOut(getApplicationContext());
        }
    }

    private void callMyAnnoncesActivity(@Nullable String action) {
        String uidUser = SharedPreferencesHelper.getInstance(getApplication()).getUidFirebaseUser();
        if (uidUser != null) {
            Intent intent = new Intent();
            intent.setClass(this, MyAnnoncesActivity.class);
            intent.putExtra(ARG_UID_USER, uidUser);
            if (action != null) {
                intent.setAction(action);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        }
    }

    private void callAdvancedSearchActivity() {
        Intent intent = new Intent();
        intent.setClass(this, AdvancedSearchActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callAddOrRemoveFromFavorite(annonceFullToSaveTofavorite);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Connexion abandonnée", Toast.LENGTH_SHORT).show();
        }
        if (requestCode == RC_SIGN_IN_TO_POST && resultCode == RESULT_OK) {
            SharedPreferencesHelper.getInstance(getApplication()).setUidFirebaseUser(FirebaseAuth.getInstance().getUid());
            shouldCallAddAnnonce = true;
        }
        if (requestCode == RC_POST_ANNONCE && resultCode == RESULT_OK) {
            Snackbar.make(coordinatorLayout, "Votre annonce a bien été sauvée.", Snackbar.LENGTH_LONG).show();
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
    protected void onStop() {
        navigationView.setNavigationItemSelectedListener(null);
        drawer.removeDrawerListener(toggle);

        NetworkReceiver.removeListener(this);

        if (mFirebaseAuth != null && mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        if (liveCountAllActiveAnnonce != null) {
            liveCountAllActiveAnnonce.removeObservers(this);
        }
        if (liveCountAllFavorite != null) {
            liveCountAllFavorite.removeObservers(this);
        }
        if (liveCountAllChat != null) {
            liveCountAllChat.removeObservers(this);
        }
        super.onStop();
    }

    @Override
    public void onDialogPositiveClick(NoticeDialogFragment dialog) {
        if (dialog.getTag() != null && dialog.getTag().equals(DIALOG_FIREBASE_RETRIEVE)) {
            // The connected user want to retrieve his annonces not present on the device but present on the network
            SharedPreferencesHelper.getInstance(this).setRetrievePreviousAnnonces(false);
            SyncService.launchSynchroFromFirebase(MainActivity.this, viewModel.getUserActuallyConnected().getUid());
        }
        dialog.dismiss();
    }

    @Override
    public void onDialogNegativeClick(NoticeDialogFragment dialog) {
        if (dialog.getTag() != null && dialog.getTag().equals(DIALOG_FIREBASE_RETRIEVE)) {
            // The connected user DON'T want to retrieve his annonces not present on the device but present on the network
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

        ListCategorieFragment listCategorieFragment = (ListCategorieFragment) getSupportFragmentManager().findFragmentByTag(TAG_LIST_CATEGORY);
        if (listCategorieFragment != null) {
            getSupportFragmentManager().putFragment(outState, TAG_LIST_CATEGORY, listCategorieFragment);
        }

        outState.putBoolean(SAVED_DYNAMIC_LINK_PROCESSED, dynamicLinkProcessed);
        outState.putParcelable(SAVED_ANNONCE_FAVORITE, annonceFullToSaveTofavorite);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void sortHasBeenUpdated(int sort) {
        SharedPreferencesHelper.getInstance(getApplicationContext()).setPrefSort(sort);
        viewModel.updateSort(sort);
    }

    @OnClick(R.id.fab_add_annonce)
    public void addAnnonce(View view) {
        if (viewModel.getUserActuallyConnected() != null) {
            callMyAnnoncesActivity(ACTION_CODE_POST);
        } else {
            callLoginUi(this, RC_SIGN_IN_TO_POST);
        }
    }

    private void catchDynamicLink() {
        mFirebaseDynamicLinks.getDynamicLink(getIntent())
                .addOnFailureListener(this, e -> Crashlytics.log(1, TAG, "getDynamicLink:onFailure" + e.getLocalizedMessage()))
                .addOnSuccessListener(this, data -> {
                    // TODO Déplacer ce code dans une classe plus business.
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
                                    Toast.makeText(this, R.string.AD_DONT_EXIST_ANYMORE, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
    }

    private void callAnnonceDetailActivity(AnnonceFull annonceFull) {
        Intent intent = new Intent(this, AnnonceDetailActivity.class);
        intent.putExtra(ARG_ANNONCE, annonceFull);
        startActivity(intent);
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
            Pair<View, String> pairImage = new Pair<>(constraintProfil, getString(R.string.TRANSITION_CONSTRAINT_PROFILE));
            Pair<View, String> pairImageUser = new Pair<>(profileImage, getString(R.string.TRANSITION_PROFILE_IMAGE));
            ActivityOptionsCompat options = makeSceneTransitionAnimation(this, pairImage, pairImageUser);
            startActivity(intent, options.toBundle());
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        }
    }

    private void callFavoriteActivity() {
        String uidUser = SharedPreferencesHelper.getInstance(getApplication()).getUidFirebaseUser();
        if (uidUser != null) {
            Intent intent = new Intent();
            intent.setClass(this, FavoritesActivity.class);
            intent.putExtra(ARG_UID_USER, uidUser);
            startActivity(intent);
            overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
        }
    }

    private void initViewsForThisUser(UserEntity userEntity) {
        if (userEntity == null) return;

        prepareNavigationMenu(true);
        profileName.setText(userEntity.getProfile());
        if (userEntity.getEmail() != null && !userEntity.getEmail().isEmpty()) {
            profileEmail.setText(userEntity.getEmail());
        }
        if (userEntity.getPhotoUrl() != null) {
            GlideApp.with(profileImage)
                    .load(userEntity.getPhotoUrl())
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .error(R.mipmap.ic_banana_launcher_foreground)
                    .into(profileImage);
        }

        // activeBadges doit être appelé après avoir renseigné l'UID du user dans les SharedPreferences
        activeBadges(userEntity.getUid(), true);
    }

    private void activeBadges(@Nullable String uidUser, boolean active) {
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

    /**
     * On définit un listener qui va écouter toutes les connections et déconnections de Firebase
     * Il appelle ensuite la méthode onAuthenticationEvent()
     *
     * @return AuthStateListener
     */
    private FirebaseAuth.AuthStateListener defineAuthListener() {
        return auth -> viewModel
                .getAuthEventType(auth.getCurrentUser())
                .observeOnce(authEventType -> onAuthenticationEvent(authEventType, auth.getCurrentUser()));
    }

    private void onAuthenticationEvent(MainActivityViewModel.AuthEventType authEventType, @Nullable FirebaseUser user) {
        if (authEventType == null) return;

        viewModel.stopStartServicesOnAuthEvent(authEventType, user);
        switch (authEventType) {
            case DISCONNECT:
                clearConnectedUser();
                break;
            case NEW_CONNECTION:
                if (user != null) {
                    initNewConnection(user);
                }
                if (shouldCallAddAnnonce) {
                    shouldCallAddAnnonce = false;
                    addAnnonce(null);
                }
                break;
            case NOTHING:
            case SAME_CONNECTION:
            default:
        }
    }

    private void initNewConnection(FirebaseUser user) {
        Crashlytics.setUserIdentifier(user.getUid()); // Nous permet de connaître l'UID de l'utilisateur quand il y a un crash
        Snackbar.make(coordinatorLayout, R.string.welcome, Snackbar.LENGTH_LONG).setAction(R.string.show_profile, v -> callProfilActivity()).show();
        if (SharedPreferencesHelper.getInstance(this).getRetrievePreviousAnnonces()) {
            viewModel.shouldIAskQuestionToRetrieveData(user.getUid()).observeOnce(shouldAsk -> {
                if (shouldAsk != null && shouldAsk.get() && !questionHasBeenAsked) {
                    questionHasBeenAsked = true;
                    sendNotificationToRetrieveData(getSupportFragmentManager(), this, getString(R.string.ads_found_on_network));
                }
            });
            questionHasBeenAsked = false;
        }
    }

    private void clearConnectedUser() {
        // activeBadges doit être appelé avant de supprimer l'UID du user dans les SharedPreferences
        prepareNavigationMenu(false);
        activeBadges(SharedPreferencesHelper.getInstance(getApplicationContext()).getUidFirebaseUser(), false);
        profileName.setText(null);
        profileEmail.setText(null);
        profileImage.setImageResource(R.drawable.ic_person_white_48dp);
        SharedPreferencesHelper.getInstance(this).setUidFirebaseUser(null);
        SharedPreferencesHelper.getInstance(this).setRetrievePreviousAnnonces(true);
    }

    private void prepareNavigationMenu(boolean isConnected) {
        navigationView.getMenu().findItem(R.id.nav_annonces).setEnabled(isConnected);
        navigationView.getMenu().findItem(R.id.nav_favorites).setEnabled(isConnected);
        navigationView.getMenu().findItem(R.id.nav_chats).setEnabled(isConnected);
        navigationViewMenu.findItem(R.id.nav_connect).setTitle((isConnected) ? getString(R.string.sign_out) : getString(R.string.sign_in));
    }

    @Override
    public void onNetworkEnable() {
        String uidUser = SharedPreferencesHelper.getInstance(this).getUidFirebaseUser();
        if (uidUser != null && !uidUser.isEmpty()) {
            viewModel.startAllServices(uidUser);
        }
        viewModel.setIsNetworkAvailable(true);
    }

    @Override
    public void onNetworkDisable() {
        viewModel.stopAllServices();
        viewModel.setIsNetworkAvailable(false);
    }

    @Override
    public View getSnackbarViewProvider() {
        return findViewById(R.id.main_coordinator_layout);
    }

    private boolean checkPermissionMversion() {
        return Build.VERSION.SDK_INT >= 23 &&
                viewModel.getMediaUtility().isExternalStorageAvailable() &&
                viewModel.getMediaUtility().allPermissionsAreGranted(getApplicationContext(), Collections.singletonList(Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    private void callAddOrRemoveFromFavorite(AnnonceFull annonceFull) {
        viewModel.addOrRemoveFromFavorite(viewModel.getUserActuallyConnected().getUid(), annonceFull).observeOnce(addRemoveFromFavorite -> {
            if (addRemoveFromFavorite != null) {
                switch (addRemoveFromFavorite) {
                    case ONE_OF_YOURS:
                        Toast.makeText(this, R.string.action_impossible_own_this_annonce, Toast.LENGTH_LONG).show();
                        break;
                    case ADD_SUCCESSFUL:
                        Snackbar.make(coordinatorLayout, R.string.AD_ADD_TO_FAVORITE, Snackbar.LENGTH_LONG)
                                .setAction(R.string.MY_FAVORITE, v12 -> callAddOrRemoveFromFavorite(annonceFullToSaveTofavorite))
                                .show();
                        break;
                    case REMOVE_SUCCESSFUL:
                        Snackbar.make(coordinatorLayout, R.string.annonce_remove_from_favorite, Snackbar.LENGTH_LONG).show();
                        break;
                    case REMOVE_FAILED:
                        Toast.makeText(this, R.string.remove_from_favorite_failed, Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * OnClickListener that share an annonce with a DynamicLink
     */
    @Override
    public void onShareClick(AnnonceFull annonceFull) {
        String uidUser = (viewModel.getUserActuallyConnected() != null) ? viewModel.getUserActuallyConnected().getUid() : null;
        if (uidUser != null && !uidUser.isEmpty()) {
            // Display a loading spinner
            LoadingDialogFragment loadingDialogFragment = new LoadingDialogFragment();
            loadingDialogFragment.setText(getString(R.string.dynamic_link_creation));
            loadingDialogFragment.show(getSupportFragmentManager(), LOADING_DIALOG);

            // Génération d'un lien
            DynamicLinkService.shareDynamicLink(this, annonceFull, uidUser, loadingDialogFragment, coordinatorLayout);
        } else {
            Snackbar.make(coordinatorLayout, R.string.sign_in_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_in, v1 -> Utility.signIn(this, RC_SIGN_IN))
                    .show();
        }
    }

    /**
     * OnClickListener that adds an annonce and all of this photo into the favorite.
     * This save all the photos of the annonce in the device and the annonce into the local database
     * If the annonce was already into the database it remove all the photo from the device,
     * delete all the photos from the database,
     * delete the annonce from the database.
     */
    @Override
    public void onFavoriteClick(AnnonceFull annonceFull) {
        String uidUser = (viewModel.getUserActuallyConnected() != null) ? viewModel.getUserActuallyConnected().getUid() : null;
        annonceFullToSaveTofavorite = annonceFull;
        if (uidUser != null && !uidUser.isEmpty()) {

            // Ask for permission to write on the external storage of the device
            if (checkPermissionMversion()) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_PERMISSION_CODE);
            } else {
                callAddOrRemoveFromFavorite(annonceFull);
            }
        } else {
            Snackbar.make(coordinatorLayout, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.sign_in), v1 -> Utility.signIn(this, RC_SIGN_IN))
                    .show();
        }
    }
}
