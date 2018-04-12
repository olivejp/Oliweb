package oliweb.nc.oliweb.ui.activity;


import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.relex.circleindicator.CircleIndicator;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.firebase.dto.UtilisateurFirebase;
import oliweb.nc.oliweb.network.CallLoginUi;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.ui.adapter.AnnonceViewPagerAdapter;
import oliweb.nc.oliweb.ui.glide.GlideApp;

import static oliweb.nc.oliweb.Constants.PARAM_MAJ;

public class AnnonceDetailActivity extends AppCompatActivity {

    public static final String ARG_ANNONCE = "ARG_ANNONCE";
    public static final String ARG_UID_ANNONCE = "ARG_UID_ANNONCE";
    private static final int REQUEST_CODE_LOGIN = 100;
    private static final int CALL_POST_ANNONCE = 200;

    @BindView(R.id.collapsing_toolbar_detail)
    CollapsingToolbarLayout collapsingToolbarLayout;

    @BindView(R.id.view_pager_detail)
    ViewPager viewPager;

    @BindView(R.id.indicator_detail)
    CircleIndicator indicator;

    @BindView(R.id.text_description_detail)
    TextView description;

    @BindView(R.id.text_view_prix_detail)
    TextView prix;

    @BindView(R.id.detail_toolbar)
    Toolbar toolbar;

    @BindView(R.id.fab_action_email)
    FloatingActionButton fabActionEmail;

    @BindView(R.id.fab_action_telephone)
    FloatingActionButton fabActionTelephone;

    @BindView(R.id.fab_action_message)
    FloatingActionButton fabActionMessage;

    @BindView(R.id.fab_action_update)
    FloatingActionButton fabActionUpdate;

    @BindView(R.id.image_profil_seller)
    ImageView imageProfilSeller;

    @BindView(R.id.text_date_publication)
    TextView textDatePublication;

    private AnnoncePhotos annoncePhotos;

    private ValueEventListener catchSellerPhoto = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            UtilisateurFirebase user = dataSnapshot.getValue(UtilisateurFirebase.class);
            if (user != null && user.getPhotoPath() != null) {
                GlideApp.with(getApplicationContext())
                        .load(user.getPhotoPath())
                        .circleCrop()
                        .placeholder(R.drawable.ic_person_white_48dp)
                        .error(R.drawable.ic_person_white_48dp)
                        .into(imageProfilSeller);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Do nothing
        }
    };

    private ValueEventListener actionEmailListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            UtilisateurFirebase vendeur = dataSnapshot.getValue(UtilisateurFirebase.class);
            if (vendeur != null && vendeur.getEmail() != null && !vendeur.getEmail().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setType("message/rfc822");
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{vendeur.getEmail()});
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " - " + annoncePhotos.getAnnonceEntity().getTitre());
                intent.putExtra(Intent.EXTRA_TEXT, "Bonjour, votre annonce m'intéresse...");
                try {
                    startActivity(Intent.createChooser(intent, "Envoi d'email..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(AnnonceDetailActivity.this, "Pas de client mail installé", Toast.LENGTH_SHORT).show();
                }
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Do nothing
        }
    };

    private ValueEventListener actionTelephoneListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            UtilisateurFirebase vendeur = dataSnapshot.getValue(UtilisateurFirebase.class);
            if (vendeur != null && vendeur.getTelephone() != null && !vendeur.getTelephone().isEmpty()) {
                Intent phoneIntent = new Intent(Intent.ACTION_CALL);
                phoneIntent.setData(Uri.parse(vendeur.getTelephone()));
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Do nothing
        }
    };

    public AnnonceDetailActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_annonce_detail);
        ButterKnife.bind(this);

        Bundle arguments = getIntent().getExtras();

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (arguments != null) {
            annoncePhotos = arguments.getParcelable(ARG_ANNONCE);
        }

        if (annoncePhotos != null) {
            refreshFromFirebase(annoncePhotos.getAnnonceEntity().getUUID());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        }
    }

    private void refreshFromFirebase(String uidAnnonce) {
        FirebaseDatabase.getInstance()
                .getReference(Constants.FIREBASE_DB_ANNONCE_REF)
                .child(uidAnnonce)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        AnnonceDto dto = dataSnapshot.getValue(AnnonceDto.class);
                        if (dto != null) {
                            AnnoncePhotos annonce = AnnonceConverter.convertDtoToEntity(dto);
                            initDisplay(annonce);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Do nothing
                    }
                });
    }

    private void initDisplay(AnnoncePhotos annoncePhotos) {
        if (annoncePhotos != null) {
            boolean amITheOwner = annoncePhotos.getAnnonceEntity().getUuidUtilisateur().equals(FirebaseAuth.getInstance().getUid());

            prix.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", annoncePhotos.getAnnonceEntity().getPrix()) + " XPF"));
            description.setText(annoncePhotos.getAnnonceEntity().getDescription());
            collapsingToolbarLayout.setTitle(annoncePhotos.getAnnonceEntity().getTitre());
            textDatePublication.setText(DateConverter.simpleUiMessageDateFormat.format(new Date(annoncePhotos.getAnnonceEntity().getDatePublication())));

            if (amITheOwner) {
                fabActionUpdate.setVisibility(View.VISIBLE);
                fabActionEmail.setVisibility(View.GONE);
                fabActionTelephone.setVisibility(View.GONE);
                fabActionMessage.setVisibility(View.GONE);
            } else {
                fabActionUpdate.setVisibility(View.GONE);
                fabActionEmail.setVisibility((annoncePhotos.getAnnonceEntity().getContactByEmail() != null && annoncePhotos.getAnnonceEntity().getContactByEmail().equals("O")) ? View.VISIBLE : View.GONE);
                fabActionTelephone.setVisibility((annoncePhotos.getAnnonceEntity().getContactByTel() != null && annoncePhotos.getAnnonceEntity().getContactByTel().equals("O")) ? View.VISIBLE : View.GONE);
                fabActionMessage.setVisibility((annoncePhotos.getAnnonceEntity().getContactByMsg() != null && annoncePhotos.getAnnonceEntity().getContactByMsg().equals("O")) ? View.VISIBLE : View.GONE);
            }

            // Récupération de la photo du vendeur
            FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_USER_REF).child(annoncePhotos.getAnnonceEntity().getUuidUtilisateur()).addListenerForSingleValueEvent(catchSellerPhoto);

            if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
                viewPager.setAdapter(new AnnonceViewPagerAdapter(this, annoncePhotos.getPhotos()));
                indicator.setViewPager(viewPager);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.fab_action_message)
    public void actionMessage() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            CallLoginUi.callLoginUi(this, REQUEST_CODE_LOGIN);
        } else {
            callListMessageFragment();
        }
    }

    @OnClick(R.id.fab_action_telephone)
    public void actionTelephone() {
        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_USER_REF).child(annoncePhotos.getAnnonceEntity().getUuidUtilisateur()).addValueEventListener(actionTelephoneListener);
    }

    @OnClick(R.id.fab_action_email)
    public void actionEmail() {
        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_USER_REF).child(annoncePhotos.getAnnonceEntity().getUuidUtilisateur()).addValueEventListener(actionEmailListener);
    }

    @OnClick(R.id.fab_action_update)
    public void callPostAnnonce(View v) {
        Intent intent = new Intent();
        intent.setClass(this, PostAnnonceActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(PostAnnonceActivity.BUNDLE_KEY_MODE, PARAM_MAJ);
        bundle.putString(PostAnnonceActivity.BUNDLE_KEY_UID_ANNONCE, annoncePhotos.getAnnonceEntity().getUUID());
        intent.putExtras(bundle);
        startActivityForResult(intent, CALL_POST_ANNONCE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LOGIN && resultCode == RESULT_OK) {
            callListMessageFragment();
        }
        if (requestCode == CALL_POST_ANNONCE && resultCode == RESULT_OK) {
            refreshFromFirebase(annoncePhotos.getAnnonceEntity().getUUID());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void callListMessageFragment() {
        Intent intent = new Intent();
        intent.setClass(this, AnnonceMessageActivity.class);
        intent.putExtra(ARG_ANNONCE, annoncePhotos.getAnnonceEntity());
        startActivity(intent);
    }
}
