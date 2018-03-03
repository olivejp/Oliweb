package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.SharedPreferencesHelper;
import oliweb.nc.oliweb.database.repository.task.TypeTask;
import oliweb.nc.oliweb.job.FirebaseSync;
import oliweb.nc.oliweb.job.SyncService;
import oliweb.nc.oliweb.network.CallLoginUi;
import oliweb.nc.oliweb.network.NetworkReceiver;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceSearchDto;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;
import oliweb.nc.oliweb.ui.task.CatchPhotoFromUrlTask;

import static oliweb.nc.oliweb.ui.activity.PostAnnonceActivity.RC_POST_ANNONCE;
import static oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment.TYPE_BOUTON_YESNO;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, CatchPhotoFromUrlTask.TaskListener, NoticeDialogFragment.DialogListener {

    public static final int RC_SIGN_IN = 1001;

    public static final String DIALOG_FIREBASE_RETRIEVE = "DIALOG_FIREBASE_RETRIEVE";

    private static final String TAG = MainActivity.class.getName();

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    @BindView(R.id.search_view)
    SearchView searchView;

    private ImageView profileImage;
    private TextView profileName;
    private TextView profileEmail;
    private Menu navigationViewMenu;

    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private CatchPhotoFromUrlTask photoTask;

    private MainActivityViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // On attache la searchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

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
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
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
        } else if (id == R.id.nav_profile) {

        } else if (id == R.id.nav_annonces) {
            Intent intent = new Intent();
            intent.setClass(this, MyAnnoncesActivity.class);
            startActivity(intent);
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Remise à blanc des champs spécifiques à la connexion
     */
    private void signOut() {
        mFirebaseAuth.signOut();
        SharedPreferencesHelper.getInstance(this).setUidFirebaseUser(null);
    }

    /**
     * Methode pour lancer une connexion
     */
    private void signIn(int requestCode) {
        if (NetworkReceiver.checkConnection(this)) {
            signOut();
            CallLoginUi.callLoginUi(this, requestCode);
        } else {
            Snackbar.make(navigationView, "Une connexion est requise pour se connecter", Snackbar.LENGTH_LONG).show();
        }
    }

    private void createUser() {
        viewModel.createUtilisateur(mFirebaseUser, dataReturn -> {
            if (dataReturn.getTypeTask() == TypeTask.INSERT && dataReturn.getNb() > 0) {
                Snackbar.make(toolbar, "Utilisateur " + mFirebaseUser.getDisplayName() + " bien créé", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Authentification simple
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Connexion abandonnée", Toast.LENGTH_SHORT).show();
                finish();

            }
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bienvenue", Toast.LENGTH_LONG).show();
                // All the rest is done in defineAuthListener()
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

    @Override
    protected void onDestroy() {
        if (photoTask != null) {
            photoTask.setListener(null);
            photoTask.setContext(null);
        }
        super.onDestroy();
    }

    @Override
    public Object onSuccess(Drawable drawable) {
        profileImage.setImageDrawable(drawable);
        return null;
    }

    /**
     * Try to retrieve the photo via URL
     */
    private void callPhotoTask() {
        Uri[] uris = new Uri[]{mFirebaseUser.getPhotoUrl()};
        photoTask = new CatchPhotoFromUrlTask();
        photoTask.setContext(getApplicationContext());
        photoTask.setListener(this);
        photoTask.execute(uris);
    }

    private void retrieveAnnonceFromFirebase(String uidUtilisateur) {
        FirebaseSync firebaseSync = FirebaseSync.getInstance(this);
        firebaseSync.getAllAnnonceByUidUtilisateur(uidUtilisateur).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                AtomicBoolean questionAsked = new AtomicBoolean(false);
                if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                    HashMap<String, AnnonceSearchDto> mapAnnonceSearchDto = dataSnapshot.getValue(FirebaseSync.genericClass);
                    if (mapAnnonceSearchDto != null && !mapAnnonceSearchDto.isEmpty()) {
                        for (Map.Entry<String, AnnonceSearchDto> entry : mapAnnonceSearchDto.entrySet()) {
                            if (questionAsked.get()) {
                                break;
                            }
                            firebaseSync.existByUidUtilisateurAndUidAnnonce(uidUtilisateur, entry.getValue().getUuid())
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.io())
                                    .subscribe(integer -> {
                                        if ((integer == null || integer.equals(0)) && !questionAsked.get()) {
                                            questionAsked.set(true);
                                            String message = "Des annonces vous appartenant ont été trouvées sur le réseau, voulez vous les récupérer sur votre appareil ?";
                                            NoticeDialogFragment.sendDialogByFragmentManagerWithRes(getSupportFragmentManager(), message, TYPE_BOUTON_YESNO, R.drawable.ic_announcement_white_48dp, DIALOG_FIREBASE_RETRIEVE, null, MainActivity.this);
                                        }
                                    });

                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled");
            }
        });
    }

    private void defineAuthListener() {
        mAuthStateListener = firebaseAuth -> {
            mFirebaseUser = firebaseAuth.getCurrentUser();
            prepareNavigationMenu();
            if (mFirebaseUser != null) {

                retrieveAnnonceFromFirebase(mFirebaseUser.getUid());

                SharedPreferencesHelper.getInstance(this).setUidFirebaseUser(mFirebaseUser.getUid());
                profileName.setText(mFirebaseUser.getDisplayName());
                if (mFirebaseUser.getEmail() != null) {
                    profileEmail.setText(mFirebaseUser.getEmail());
                }

                // Create user in local Db
                createUser();

                // Call the task to retrieve the photo
                callPhotoTask();
            } else {
                SharedPreferencesHelper.getInstance(this).setUidFirebaseUser(null);
                profileName.setText(null);
                profileEmail.setText(null);
                mFirebaseUser = null;
                profileImage.setImageResource(R.drawable.ic_person_white_48dp);
            }
        };
    }

    private void prepareNavigationMenu() {
        navigationView.getMenu().findItem(R.id.nav_annonces).setEnabled(mFirebaseUser != null);
        navigationView.getMenu().findItem(R.id.nav_profile).setEnabled(mFirebaseUser != null);
        navigationView.getMenu().findItem(R.id.nav_favorites).setEnabled(mFirebaseUser != null);
        navigationViewMenu.findItem(R.id.nav_connect).setTitle((mFirebaseUser != null) ? "Se déconnecter" : "Se connecter");
    }

    @Override
    public void onDialogPositiveClick(NoticeDialogFragment dialog) {
        if (dialog.getTag().equals(DIALOG_FIREBASE_RETRIEVE)) {
            // Launch synchro to retrieve datas from Firebase
            SyncService.launchSynchroFromFirebase(MainActivity.this, mFirebaseUser.getUid());
            dialog.dismiss();
        }
    }

    @Override
    public void onDialogNegativeClick(NoticeDialogFragment dialog) {
        dialog.dismiss();
    }
}
