package oliweb.nc.oliweb.ui.activity;


import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ShareCompat;
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

import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.relex.circleindicator.CircleIndicator;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.AnnonceDetailViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceViewPagerAdapter;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Utility;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.ARG_ACTION_FRAGMENT_MESSAGE;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.DATA_FIREBASE_USER_UID;
import static oliweb.nc.oliweb.utility.Constants.PARAM_MAJ;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AnnonceDetailActivity extends AppCompatActivity {

    public static final String TAG = AnnonceDetailActivity.class.getCanonicalName();

    public static final String ARG_ANNONCE = "ARG_ANNONCE";
    public static final String ARG_COME_FROM_CHAT_FRAGMENT = "ARG_COME_FROM_CHAT_FRAGMENT";
    public static final int REQUEST_CALL_PHONE_CODE = 100;
    public static final int RESULT_PHONE_CALL = 101;

    private static final int REQUEST_CODE_LOGIN = 100;
    private static final int REQUEST_CALL_POST_ANNONCE = 200;
    private static final String MAIL_MESSAGE_TYPE = "message/rfc822";

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
    private UserEntity seller;
    private FirebaseAuth auth;
    private boolean comeFromChatFragment;

    public AnnonceDetailActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vérification des arguments
        Bundle arguments = getIntent().getExtras();
        assertNotNull("No arguments found", arguments);
        assertTrue(String.format("Argument named %s is mandatory", ARG_ANNONCE), arguments.containsKey(ARG_ANNONCE));
        assertNotNull(String.format("Argument named %s should not be null", ARG_ANNONCE), arguments.get(ARG_ANNONCE));

        // Récupération des arguments
        annoncePhotos = arguments.getParcelable(ARG_ANNONCE);
        if (arguments.containsKey(ARG_COME_FROM_CHAT_FRAGMENT)) {
            comeFromChatFragment = arguments.getBoolean(ARG_COME_FROM_CHAT_FRAGMENT);
        }

        // Récupération du ViewModel
        AnnonceDetailViewModel viewModel = ViewModelProviders.of(this).get(AnnonceDetailViewModel.class);

        // Création de la vue
        setContentView(R.layout.activity_annonce_detail);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        auth = FirebaseAuth.getInstance();

        // Récupération de l'annonce
        initDisplay(annoncePhotos);

        // Récupération des infos du vendeur
        viewModel.getFirebaseSeller(annoncePhotos.getAnnonceEntity().getUidUser())
                .observe(this, dataSnapshot -> {
                    if (dataSnapshot == null) return;

                    seller = dataSnapshot.getValue(UserEntity.class);
                    if (seller == null) return;

                    if (seller.getPhotoUrl() != null) {
                        GlideApp.with(imageProfilSeller)
                                .load(seller.getPhotoUrl())
                                .circleCrop()
                                .placeholder(R.drawable.ic_person_white_48dp)
                                .error(R.drawable.ic_person_white_48dp)
                                .into(imageProfilSeller);

                    }

                    // Initialisation des actions possibles
                    initCommunicationActions();
                });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LOGIN && resultCode == RESULT_OK) {
            callListMessageFragment();
        }

        if (requestCode == REQUEST_CALL_POST_ANNONCE && resultCode == RESULT_OK) {
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PHONE_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            makePhoneCall();
        }
    }

    private void initDisplay(AnnoncePhotos annoncePhotos) {
        // Condition de garde. Si pas d'annonce, on ne fait rien.
        if (annoncePhotos == null) {
            return;
        }

        AnnonceEntity annonce = annoncePhotos.getAnnonceEntity();

        prix.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", annonce.getPrix()) + " XPF"));
        description.setText(annonce.getDescription());
        collapsingToolbarLayout.setTitle(annonce.getTitre());
        textDatePublication.setText(DateConverter.simpleUiMessageDateFormat.format(new Date(annonce.getDatePublication())));

        if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
            viewPager.setAdapter(new AnnonceViewPagerAdapter(this, annoncePhotos.getPhotos()));
            indicator.setViewPager(viewPager);
        }
    }

    private void initCommunicationActions() {
        AnnonceEntity annonce = annoncePhotos.getAnnonceEntity();
        boolean amITheOwner = auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(annonce.getUidUser());
        if (amITheOwner) {
            fabActionUpdate.setVisibility(View.VISIBLE);
            fabActionEmail.setVisibility(View.GONE);
            fabActionTelephone.setVisibility(View.GONE);
            fabActionMessage.setVisibility(View.GONE);
        } else {
            fabActionUpdate.setVisibility(View.GONE);
            fabActionEmail.setVisibility((annonce.getContactByEmail() != null && "O".equals(annonce.getContactByEmail()) && seller.getEmail() != null) ? View.VISIBLE : View.GONE);
            fabActionTelephone.setVisibility((annonce.getContactByTel() != null && "O".equals(annonce.getContactByTel()) && seller.getTelephone() != null) ? View.VISIBLE : View.GONE);
            fabActionMessage.setVisibility((annonce.getContactByMsg() != null && "O".equals(annonce.getContactByMsg()) && !comeFromChatFragment) ? View.VISIBLE : View.GONE);
        }
    }

    @OnClick(R.id.fab_action_message)
    public void actionMessage() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Utility.callLoginUi(this, REQUEST_CODE_LOGIN);
        } else {
            callListMessageFragment();
        }
    }

    @OnClick(R.id.fab_action_telephone)
    public void actionTelephone() {
        if (seller != null && seller.getTelephone() != null && !seller.getTelephone().isEmpty()) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE_CODE);
                } else {
                    makePhoneCall();
                }
            } else {
                makePhoneCall();
            }
        } else {
            Toast.makeText(AnnonceDetailActivity.this, R.string.error_cant_retrieve_phone_number, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.fab_action_email)
    public void actionEmail() {
        if (seller != null && seller.getEmail() != null && !seller.getEmail().isEmpty()) {
            ShareCompat.IntentBuilder.from(this)
                    .setType(MAIL_MESSAGE_TYPE)
                    .addEmailTo(seller.getEmail())
                    .setSubject(getString(R.string.app_name) + " - " + annoncePhotos.getAnnonceEntity().getTitre())
                    .setText(getString(R.string.default_mail_message))
                    .setChooserTitle(R.string.default_mail_chooser_title)
                    .startChooser();
        } else {
            Toast.makeText(AnnonceDetailActivity.this, R.string.error_cant_retrieve_email, Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.fab_action_update)
    public void callPostAnnonce(View v) {
        Intent intent = new Intent();
        intent.setClass(this, PostAnnonceActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(PostAnnonceActivity.BUNDLE_KEY_MODE, PARAM_MAJ);
        bundle.putString(PostAnnonceActivity.BUNDLE_KEY_UID_ANNONCE, annoncePhotos.getAnnonceEntity().getUid());
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_CALL_POST_ANNONCE);
    }

    /**
     * Permission is already checked in the onClickListener actionTelephone()
     */
    @SuppressWarnings(value = "MissingPermission")
    private void makePhoneCall() {
        Intent phoneIntent = new Intent(Intent.ACTION_CALL);
        phoneIntent.setData(Uri.parse("tel:" + seller.getTelephone()));
        try {
            startActivityForResult(phoneIntent, RESULT_PHONE_CALL);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(AnnonceDetailActivity.this, R.string.error_cant_make_phone_call, Toast.LENGTH_SHORT).show();
        }
    }

    private void callListMessageFragment() {
        Intent intent = new Intent();
        intent.setClass(this, MyChatsActivity.class);
        intent.setAction(ARG_ACTION_FRAGMENT_MESSAGE);
        intent.putExtra(ARG_ANNONCE, annoncePhotos.getAnnonceEntity());
        intent.putExtra(DATA_FIREBASE_USER_UID, FirebaseAuth.getInstance().getUid());
        startActivity(intent);
        overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }
}
