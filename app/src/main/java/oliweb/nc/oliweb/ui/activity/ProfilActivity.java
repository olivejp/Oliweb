package oliweb.nc.oliweb.ui.activity;


import android.arch.lifecycle.ViewModelProviders;
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
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.ui.activity.viewmodel.ProfilViewModel;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;

import static android.support.v4.internal.view.SupportMenuItem.SHOW_AS_ACTION_ALWAYS;
import static android.support.v4.internal.view.SupportMenuItem.SHOW_AS_ACTION_NEVER;

public class ProfilActivity extends AppCompatActivity {
    public static final String UID_USER = "uidUser";
    public static final String UPDATE = "availableUpdate";

    private String uidUser;
    private boolean availableUpdate;

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

    @BindView(R.id.profil_nb_messages)
    TextView textNbMessages;

    @BindView(R.id.profil_toolbar)
    Toolbar toolbar;

    @BindView(R.id.profil_main_constraint)
    ConstraintLayout mainConstraint;

    private Menu mMenu;

    private UtilisateurFirebase utilisateurFirebase;

    public ProfilActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profil);
        ButterKnife.bind(this);

        ProfilViewModel viewModel = ViewModelProviders.of(this).get(ProfilViewModel.class);

        Bundle args = getIntent().getExtras();
        if (args != null) {
            uidUser = args.getString(UID_USER);
            availableUpdate = args.getBoolean(UPDATE);
            if (uidUser != null) {
                viewModel.getFirebaseUser(uidUser).observe(this, dataSnapshot -> {
                    if (dataSnapshot != null) {
                        utilisateurFirebase = dataSnapshot.getValue(UtilisateurFirebase.class);
                        if (utilisateurFirebase != null) {
                            textName.setText(utilisateurFirebase.getProfileName());
                            textEmail.setText(utilisateurFirebase.getEmail());
                            textTelephone.setText(utilisateurFirebase.getTelephone());
                            GlideApp.with(imageProfil).load(utilisateurFirebase.getPhotoPath()).placeholder(R.drawable.ic_person_grey_900_48dp).circleCrop().into(imageProfil);
                        }
                    }
                });

                viewModel.getFirebaseUserNbAnnoncesCount(uidUser).observe(this, countAnnonce -> textNbAnnonce.setText(String.valueOf(countAnnonce)));

                viewModel.getFirebaseUserNbChatsCount(uidUser).observe(this, countcountChats -> textNbChats.setText(String.valueOf(countcountChats)));

                viewModel.getFirebaseUserNbMessagesCount(uidUser).observe(this, countMessages -> textNbMessages.setText(String.valueOf(countMessages)));
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
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (availableUpdate) {
            getMenuInflater().inflate(R.menu.fragment_profil, menu);
            this.mMenu = menu;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (item.getItemId() == R.id.menu_profil_save) {
            textTelephone.setEnabled(false);
            mMenu.findItem(R.id.menu_profil_edit).setVisible(true);
            mMenu.findItem(R.id.menu_profil_save).setVisible(false);
            mMenu.findItem(R.id.menu_profil_save).setShowAsAction(SHOW_AS_ACTION_NEVER);
            utilisateurFirebase.setTelephone(textTelephone.getText().toString());
            FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_USER_REF).child(uidUser).setValue(utilisateurFirebase).addOnSuccessListener(aVoid ->
                    Toast.makeText(getApplicationContext(), "Mise à jour effectuée", Toast.LENGTH_LONG).show()
            );
            return true;
        }

        if (item.getItemId() == R.id.menu_profil_edit) {
            textTelephone.setEnabled(true);
            mMenu.findItem(R.id.menu_profil_edit).setVisible(false);
            mMenu.findItem(R.id.menu_profil_save).setVisible(true);
            mMenu.findItem(R.id.menu_profil_save).setShowAsAction(SHOW_AS_ACTION_ALWAYS);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }
}
