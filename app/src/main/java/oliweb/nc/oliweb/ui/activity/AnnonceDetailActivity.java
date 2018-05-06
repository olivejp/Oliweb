package oliweb.nc.oliweb.ui.activity;


import android.arch.lifecycle.ViewModelProviders;
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

import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.relex.circleindicator.CircleIndicator;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;
import oliweb.nc.oliweb.network.elasticsearchDto.AnnonceDto;
import oliweb.nc.oliweb.ui.activity.viewmodel.AnnonceDetailViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceViewPagerAdapter;
import oliweb.nc.oliweb.ui.fragment.ProfilFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.utility.Constants.PARAM_MAJ;

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
    private UtilisateurEntity seller;
    private FirebaseAuth auth;

    public AnnonceDetailActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        AnnonceDetailViewModel viewModel = ViewModelProviders.of(this).get(AnnonceDetailViewModel.class);

        setContentView(R.layout.activity_annonce_detail);
        ButterKnife.bind(this);

        Bundle arguments = getIntent().getExtras();

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (arguments != null && arguments.containsKey(ARG_ANNONCE)) {
            annoncePhotos = arguments.getParcelable(ARG_ANNONCE);
        }

        if (annoncePhotos != null) {
            // Récupération de l'annonce
            viewModel.getFirebaseAnnonceDetailByUid(annoncePhotos.getAnnonceEntity().getUid()).observe(this, dataSnapshot -> {
                if (dataSnapshot != null) {
                    AnnonceDto dto = dataSnapshot.getValue(AnnonceDto.class);
                    if (dto != null) {
                        AnnoncePhotos annonce = AnnonceConverter.convertDtoToAnnoncePhotos(dto);
                        initDisplay(annonce);
                    }
                }
            });

            // Récupération du vendeur
            viewModel.getFirebaseSeller(annoncePhotos.getAnnonceEntity().getUidUser()).observe(AnnonceDetailActivity.this, dataSnapshot -> {
                if (dataSnapshot != null) {
                    seller = dataSnapshot.getValue(UtilisateurEntity.class);
                    if (seller != null && seller.getPhotoUrl() != null) {
                        imageProfilSeller.setOnClickListener(v -> {
                            ProfilFragment fragment = ProfilFragment.getInstance(annoncePhotos.getAnnonceEntity().getUidUser(), false);
                            getSupportFragmentManager()
                                    .beginTransaction()
                                    .add(R.id.frame_annonce_detail, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        });
                        GlideApp.with(imageProfilSeller)
                                .load(seller.getPhotoUrl())
                                .circleCrop()
                                .placeholder(R.drawable.ic_person_white_48dp)
                                .error(R.drawable.ic_person_white_48dp)
                                .into(imageProfilSeller);
                    }
                }
            });
        }

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
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initDisplay(AnnoncePhotos annoncePhotos) {
        // Condition de garde. Si pas d'annonce, on ne fait rien.
        if (annoncePhotos == null) {
            return;
        }

        AnnonceEntity annonce = annoncePhotos.getAnnonceEntity();

        boolean amITheOwner = auth.getCurrentUser() != null && auth.getCurrentUser().getUid().equals(annonce.getUidUser());

        prix.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", annonce.getPrix()) + " XPF"));
        description.setText(annonce.getDescription());
        collapsingToolbarLayout.setTitle(annonce.getTitre());
        textDatePublication.setText(DateConverter.simpleUiMessageDateFormat.format(new Date(annonce.getDatePublication())));

        if (amITheOwner) {
            fabActionUpdate.setVisibility(View.VISIBLE);
            fabActionEmail.setVisibility(View.GONE);
            fabActionTelephone.setVisibility(View.GONE);
            fabActionMessage.setVisibility(View.GONE);
        } else {
            fabActionUpdate.setVisibility(View.GONE);
            fabActionEmail.setVisibility((annonce.getContactByEmail() != null && annonce.getContactByEmail().equals("O")) ? View.VISIBLE : View.GONE);
            fabActionTelephone.setVisibility((annonce.getContactByTel() != null && annonce.getContactByTel().equals("O")) ? View.VISIBLE : View.GONE);
            fabActionMessage.setVisibility((annonce.getContactByMsg() != null && annonce.getContactByMsg().equals("O")) ? View.VISIBLE : View.GONE);
        }

        if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
            viewPager.setAdapter(new AnnonceViewPagerAdapter(this, annoncePhotos.getPhotos()));
            indicator.setViewPager(viewPager);
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
            Intent phoneIntent = new Intent(Intent.ACTION_CALL);
            phoneIntent.setData(Uri.parse(seller.getTelephone()));
        }
    }

    @OnClick(R.id.fab_action_email)
    public void actionEmail() {
        if (seller != null && seller.getEmail() != null && !seller.getEmail().isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setType("message/rfc822");
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{seller.getEmail()});
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " - " + annoncePhotos.getAnnonceEntity().getTitre());
            intent.putExtra(Intent.EXTRA_TEXT, "Bonjour, votre annonce m'intéresse...");
            try {
                startActivity(Intent.createChooser(intent, "Envoi d'email..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(AnnonceDetailActivity.this, "Pas de client mail installé", Toast.LENGTH_SHORT).show();
            }
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
        startActivityForResult(intent, CALL_POST_ANNONCE);
    }

    private void callListMessageFragment() {
        Intent intent = new Intent();
        intent.setClass(this, AnnonceMessageActivity.class);
        intent.putExtra(ARG_ANNONCE, annoncePhotos.getAnnonceEntity());
        startActivity(intent);
    }
}
