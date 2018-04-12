package oliweb.nc.oliweb.ui.activity;


import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Utility;

public class ProfilActivity extends AppCompatActivity {
    public static final String UID_USER = "uidUser";

    private String uidUser;

    @BindView(R.id.profil_photo)
    ImageView imageProfil;

    @BindView(R.id.profil_image_background)
    ImageView imageBackground;

    @BindView(R.id.profil_name)
    TextView textName;

    @BindView(R.id.profil_email)
    TextView textEmail;

    @BindView(R.id.profil_telephone)
    TextView textTelephone;

    @BindView(R.id.profil_nb_annonce)
    TextView textNbAnnonce;

    @BindView(R.id.profil_nb_chats)
    TextView textNbChats;

    @BindView(R.id.profil_toolbar)
    Toolbar toolbar;

    @BindView(R.id.profil_main_constraint)
    ConstraintLayout mainConstraint;

    private UtilisateurFirebase utilisateurFirebase;

    public ProfilActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profil);
        ButterKnife.bind(this);

        Bundle args = getIntent().getExtras();
        if (args != null) {
            uidUser = args.getString(UID_USER);
            if (uidUser != null) {
                FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_USER_REF).child(uidUser).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        utilisateurFirebase = dataSnapshot.getValue(UtilisateurFirebase.class);
                        if (utilisateurFirebase != null) {
                            textName.setText(utilisateurFirebase.getProfileName());
                            textEmail.setText(utilisateurFirebase.getEmail());
                            textTelephone.setText(utilisateurFirebase.getTelephone());
                            GlideApp.with(ProfilActivity.this).load(utilisateurFirebase.getPhotoPath()).circleCrop().into(imageProfil);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Do nothing
                    }
                });
            }
        }

        mainConstraint.setTop(Utility.getStatusBarHeight(this));

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setTitle("");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.fragment_profil, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (item.getItemId() == R.id.menu_profil_edit) {
            // TODO passer l'écran en mode mise à jour et afficher un menu pour sauvegarder les modifications
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
