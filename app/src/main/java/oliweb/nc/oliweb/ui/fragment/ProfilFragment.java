package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.ui.activity.viewmodel.ProfilViewModel;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Utility;

public class ProfilFragment extends Fragment {

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

    private AppCompatActivity appCompatActivity;
    private UtilisateurFirebase utilisateurFirebase;
    private ProfilViewModel viewModel;

    public ProfilFragment() {
        // Empty constructor
    }

    public static synchronized ProfilFragment getInstance(String uidUtilisateur, boolean availableUpdate) {
        ProfilFragment listAnnonceFragment = new ProfilFragment();
        Bundle bundle = new Bundle();
        bundle.putString(UID_USER, uidUtilisateur);
        bundle.putBoolean(UPDATE, availableUpdate);
        listAnnonceFragment.setArguments(bundle);
        return listAnnonceFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            uidUser = getArguments().getString(UID_USER);
            availableUpdate = getArguments().getBoolean(UPDATE);
        }
        viewModel = ViewModelProviders.of(appCompatActivity).get(ProfilViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profil, container, false);
        ButterKnife.bind(this, rootView);

        if (uidUser != null) {
            viewModel.getFirebaseUser(uidUser).observe(this, dataSnapshot -> {
                if (dataSnapshot != null) {
                    utilisateurFirebase = dataSnapshot.getValue(UtilisateurFirebase.class);
                    if (utilisateurFirebase != null) {
                        textName.setText(utilisateurFirebase.getProfileName());
                        textEmail.setText(utilisateurFirebase.getEmail());
                        textTelephone.setText(utilisateurFirebase.getTelephone());
                        GlideApp.with(imageProfil)
                                .load(utilisateurFirebase.getPhotoPath())
                                .placeholder(R.drawable.ic_person_grey_900_48dp)
                                .circleCrop()
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .into(imageProfil);
                    }
                }
            });

            viewModel.getFirebaseUserNbAnnoncesCount(uidUser).observe(this, countAnnonce -> textNbAnnonce.setText(String.valueOf(countAnnonce)));

            viewModel.getFirebaseUserNbChatsCount(uidUser).observe(this, countcountChats -> textNbChats.setText(String.valueOf(countcountChats)));

            viewModel.getFirebaseUserNbMessagesCount(uidUser).observe(this, countMessages -> textNbMessages.setText(String.valueOf(countMessages)));
        }

        mainConstraint.setTop(Utility.getStatusBarHeight(appCompatActivity));

        appCompatActivity.setSupportActionBar(toolbar);
        ActionBar actionBar = appCompatActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        appCompatActivity.setTitle("");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = appCompatActivity.getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (availableUpdate) {
            appCompatActivity.getMenuInflater().inflate(R.menu.fragment_profil, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            appCompatActivity.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
