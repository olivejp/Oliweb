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
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.network.CallLoginUi;
import oliweb.nc.oliweb.network.NetworkReceiver;
import oliweb.nc.oliweb.ui.task.CatchPhotoFromUrlTask;

import static oliweb.nc.oliweb.network.CallLoginUi.RC_SIGN_IN;

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

    @BindView(R.id.profileImage)
    ImageView profileImage;

    @BindView(R.id.profileName)
    TextView profileName;

    @BindView(R.id.profileEmail)
    TextView profileEmail;

    @BindView(R.id.buttonSign)
    Button buttonSign;

    private FirebaseUser mFirebaseUser;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private CatchPhotoFromUrlTask photoTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

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

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @OnClick(R.id.fab)
    public void onClickFab(View view) {
        Intent intent = new Intent();
        intent.setClass(this, PostAnnonceActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.buttonSign)
    public void onClickSign(View view) {
        if (mFirebaseUser == null) {
            signIn();
        } else {
            signOut();
        }
    }

    /**
     * Remise à blanc des champs spécifiques à la connexion
     */
    private void signOut() {
        buttonSign.setText("Se connecter");
        profileName.setText(null);
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
                buttonSign.setText("Se déconnecter");
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
        photoTask.setListener(null);
        photoTask.setContext(null);
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
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                mFirebaseUser = firebaseAuth.getCurrentUser();
                if (mFirebaseUser != null) {
                    buttonSign.setText("Se déconnecter");
                    profileName.setText(mFirebaseUser.getDisplayName());
                    callPhotoTask();
                } else {
                    signOut();
                }
            }
        };
    }
}
