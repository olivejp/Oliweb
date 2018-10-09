package oliweb.nc.oliweb.ui.activity;


import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.ProfilViewModel;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.ArgumentsChecker;
import oliweb.nc.oliweb.utility.Utility;

import static android.support.v4.internal.view.SupportMenuItem.SHOW_AS_ACTION_ALWAYS;
import static android.support.v4.internal.view.SupportMenuItem.SHOW_AS_ACTION_NEVER;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class ProfilActivity extends AppCompatActivity {

    private static final String TAG = ProfilActivity.class.getName();

    public static final String PROFIL_ACTIVITY_UID_USER = "uidUser";
    public static final String UPDATE = "availableUpdate";

    private boolean availableUpdate;

    @BindView(R.id.profil_photo)
    ImageView imageProfil;

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
    private ProfilViewModel viewModel;

    private UserEntity userEntity;
    private LiveData<UserEntity> liveDataUser;
    private LiveData<Long> liveDataNbChat;
    private LiveData<Long> liveDataNbAnnonce;
    private LiveData<Long> liveDataNbMessage;
    private Bundle mBundle;

    public ProfilActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instanciation de la vue et binding des champs
        setContentView(R.layout.activity_profil);
        ButterKnife.bind(this);

        // Création du viewModel
        viewModel = ViewModelProviders.of(ProfilActivity.this).get(ProfilViewModel.class);

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
        mainConstraint.setTop(Utility.getStatusBarHeight(this));

        // Check des paramètres d'entrée
        ArgumentsChecker argumentsChecker = new ArgumentsChecker();
        argumentsChecker.setArguments(getIntent().getExtras())
                .isMandatory(PROFIL_ACTIVITY_UID_USER)
                .isMandatory(UPDATE)
                .setOnFailureListener(e -> finish())
                .setOnSuccessListener(bundle -> {
                    mBundle = bundle;
                    onStart();
                })
                .check();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initViews(mBundle);
    }

    private void initViews(@NonNull Bundle args) {
        String uidUser = args.getString(PROFIL_ACTIVITY_UID_USER);
        availableUpdate = args.getBoolean(UPDATE);

        liveDataUser = viewModel.getUtilisateurByUid(uidUser);
        liveDataNbAnnonce = viewModel.getFirebaseUserNbAnnoncesCount(uidUser);
        liveDataNbChat = viewModel.getFirebaseUserNbChatsCount(uidUser);
        liveDataNbMessage = viewModel.getFirebaseUserNbMessagesCount(uidUser);

        liveDataUser.observe(this, this::initUserFields);
        liveDataNbAnnonce.observe(this, countAnnonce -> textNbAnnonce.setText(String.valueOf(countAnnonce)));
        liveDataNbChat.observe(this, countcountChats -> textNbChats.setText(String.valueOf(countcountChats)));
        liveDataNbMessage.observe(this, countMessages -> textNbMessages.setText(String.valueOf(countMessages)));
    }

    private void initUserFields(UserEntity user){
        if (user != null) {
            this.userEntity = user;
            textName.setText(this.userEntity.getProfile());
            textEmail.setText(this.userEntity.getEmail());
            textTelephone.setText(this.userEntity.getTelephone());
            GlideApp.with(imageProfil).load(this.userEntity.getPhotoUrl()).placeholder(R.drawable.ic_person_grey_900_48dp).circleCrop().into(imageProfil);
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

            userEntity.setTelephone(textTelephone.getText().toString());

            viewModel.markAsToSend(userEntity)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                    .doOnSuccess(userEntitySaved -> Toast.makeText(this, R.string.UPDATE_SUCCESSFUL, Toast.LENGTH_LONG).show())
                    .subscribe();

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

    @Override
    protected void onStop() {
        if (liveDataUser != null) {
            liveDataUser.removeObservers(this);
        }
        if (liveDataNbAnnonce != null) {
            liveDataNbAnnonce.removeObservers(this);
        }
        if (liveDataNbChat != null) {
            liveDataNbChat.removeObservers(this);
        }
        if (liveDataNbMessage != null) {
            liveDataNbMessage.removeObservers(this);
        }
        super.onStop();
    }
}
