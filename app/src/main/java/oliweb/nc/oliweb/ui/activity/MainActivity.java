package oliweb.nc.oliweb.ui.activity;

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
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.network.CallLoginUi;
import oliweb.nc.oliweb.network.NetworkReceiver;
import oliweb.nc.oliweb.ui.task.CatchPhotoFromUrlTask;

import static oliweb.nc.oliweb.network.CallLoginUi.RC_SIGN_IN;
import static oliweb.nc.oliweb.ui.activity.PostAnnonceActivity.BUNDLE_KEY_MODE;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, CatchPhotoFromUrlTask.TaskListener {

    private static final String TAG = MainActivity.class.getName();

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @BindView(R.id.nav_view)
    NavigationView navigationView;

    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    private ImageView profileImage;
    private TextView profileName;
    private TextView profileEmail;
    private Menu navigationViewMenu;

    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private CatchPhotoFromUrlTask photoTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

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
                signIn();
            } else {
                signOut();
            }
        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_profile) {

        } else if (id == R.id.nav_annonces) {

        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @OnClick(R.id.fab)
    public void onClickFab(View view) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_MODE, Constants.PARAM_CRE);
        intent.putExtras(bundle);
        intent.setClass(this, PostAnnonceActivity.class);
        startActivity(intent);
    }

    /**
     * Remise à blanc des champs spécifiques à la connexion
     */
    private void signOut() {
        MenuItem item = navigationViewMenu.findItem(R.id.nav_connect);
        item.setTitle("Se connecter");
        profileName.setText(null);
        profileEmail.setText(null);
        mFirebaseUser = null;
        profileImage.setImageResource(R.drawable.ic_person_white_48dp);
    }

    /**
     * Methode pour lancer une connexion
     */
    private void signIn() {
        if (NetworkReceiver.checkConnection(this)) {
            signOut();
            CallLoginUi.callLoginUi(this);
        } else {
            Snackbar.make(navigationView, "Une connexion est requise pour se connecter", Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                MenuItem item = navigationViewMenu.findItem(R.id.nav_connect);
                item.setTitle("Se déconnecter");
                mFirebaseUser = mFirebaseAuth.getCurrentUser();
                Toast.makeText(this, "Bienvenue", Toast.LENGTH_LONG).show();

                // Call the task to retrieve the photo
                callPhotoTask();

                // Refresh data from/to the database here
            }
            if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Connexion abandonnée", Toast.LENGTH_SHORT).show();
                finish();

            }
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

    private void defineAuthListener() {
        mAuthStateListener = firebaseAuth -> {
            mFirebaseUser = firebaseAuth.getCurrentUser();
            if (mFirebaseUser != null) {
                MenuItem item = navigationViewMenu.findItem(R.id.nav_connect);
                item.setTitle("Se déconnecter");
                profileName.setText(mFirebaseUser.getDisplayName());
                if (mFirebaseUser.getEmail() != null) {
                    profileEmail.setText(mFirebaseUser.getEmail());
                }
                callPhotoTask();
            } else {
                signOut();
            }
        };
    }
}
