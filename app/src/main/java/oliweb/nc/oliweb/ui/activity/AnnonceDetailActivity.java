package oliweb.nc.oliweb.ui.activity;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.ShareCompat;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProviders;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.relex.circleindicator.CircleIndicator;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.DateConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.service.firebase.DynamicLinkService;
import oliweb.nc.oliweb.ui.activity.viewmodel.AnnonceDetailActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceViewPagerAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.ui.fragment.FromSameAuthorFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.ArgumentsChecker;
import oliweb.nc.oliweb.utility.Utility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static oliweb.nc.oliweb.ui.activity.MainActivity.RC_SIGN_IN;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.ARG_ACTION_FRAGMENT_MESSAGE;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.DATA_FIREBASE_USER_UID;
import static oliweb.nc.oliweb.ui.activity.ZoomImageActivity.ZOOM_IMAGE_ACTIVITY_ARG_URI_IMAGE;
import static oliweb.nc.oliweb.utility.Constants.MAIL_MESSAGE_TYPE;
import static oliweb.nc.oliweb.utility.Constants.PARAM_MAJ;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AnnonceDetailActivity extends AppCompatActivity {

    public static final String TAG = AnnonceDetailActivity.class.getCanonicalName();

    private static final String LOADING_DIALOG = "LOADING_DIALOG";
    public static final String ARG_ANNONCE = "ARG_ANNONCE";
    public static final String ARG_COME_FROM_CHAT_FRAGMENT = "ARG_COME_FROM_CHAT_FRAGMENT";
    public static final String SAVE_FRAG_FROM_SAME_AUTHOR = "SAVE_FRAG_FROM_SAME_AUTHOR";
    public static final int REQUEST_CALL_PHONE_CODE = 100;
    public static final int RESULT_PHONE_CALL = 101;

    private static final int REQUEST_CODE_LOGIN = 100;
    private static final int REQUEST_CALL_POST_ANNONCE = 200;


    @BindView(R.id.appbar)
    AppBarLayout appBarLayout;

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

    @BindView(R.id.categorie_libelle)
    TextView categorieLibelle;

    @BindView(R.id.annonce_detail_img_share)
    ImageView imageShare;

    @BindView(R.id.annonce_detail_img_favorite)
    ImageView imageFavorite;

    private AnnonceFull annonceFull;
    private boolean comeFromChatFragment;
    private String uidUser;
    private UserEntity seller;
    private LoadingDialogFragment loadingDialogFragment;
    private FromSameAuthorFragment fromSameAuthorFragment;
    private AnnonceDetailActivityViewModel viewModel;

    public AnnonceDetailActivity() {
        // Required empty public constructor
    }

    /**
     * OnClickListener that share an annonce with a DynamicLink
     */
    private View.OnClickListener onClickListenerShare = v -> {
        if (uidUser != null && !uidUser.isEmpty()) {

            // Display a loading spinner
            loadingDialogFragment = new LoadingDialogFragment();
            loadingDialogFragment.setText(getString(R.string.dynamic_link_creation));
            loadingDialogFragment.show(getSupportFragmentManager(), LOADING_DIALOG);

            DynamicLinkService.shareDynamicLink(this, annonceFull, uidUser, loadingDialogFragment, prix);
        } else {
            Snackbar.make(prix, R.string.sign_in_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_in, v1 -> Utility.signIn(this, RC_SIGN_IN))
                    .show();
        }
    };



    /**
     * OnClickListener that adds an annonce and all of this photo into the favorite.
     * This save all the photos of the annonce in the device and the annonce into the local database
     * If the annonce was already into the database it remove all the photo from the device,
     * delete all the photos from the database,
     * delete the annonce from the database.
     */
    private View.OnClickListener onClickListenerFavorite = (View v) -> {
        v.setEnabled(false);
        if (uidUser == null || uidUser.isEmpty()) {
            Snackbar.make(prix, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.sign_in), v1 -> Utility.signIn(this, RC_SIGN_IN))
                    .show();
            v.setEnabled(true);
        } else {
            viewModel.addOrRemoveFromFavorite(uidUser, annonceFull).observeOnce(addRemoveFromFavorite -> {
                if (addRemoveFromFavorite != null) {
                    switch (addRemoveFromFavorite) {
                        case ONE_OF_YOURS:
                            Toast.makeText(this, R.string.action_impossible_own_this_annonce, Toast.LENGTH_LONG).show();
                            break;
                        case ADD_SUCCESSFUL:
                            Snackbar.make(prix, R.string.AD_ADD_TO_FAVORITE, Snackbar.LENGTH_LONG)
                                    .show();
                            break;
                        case REMOVE_SUCCESSFUL:
                            Snackbar.make(prix, R.string.annonce_remove_from_favorite, Snackbar.LENGTH_LONG).show();
                            break;
                        case REMOVE_FAILED:
                            Toast.makeText(this, R.string.remove_from_favorite_failed, Toast.LENGTH_LONG).show();
                            break;
                        default:
                            break;
                    }
                }
                v.setEnabled(true);
            });
        }
    };

    private View.OnClickListener onClickImage = (View v) -> {
        try {
            String uriImage = (String) v.getTag();
            Intent intent = new Intent(this, ZoomImageActivity.class);
            intent.putExtra(ZOOM_IMAGE_ACTIVITY_ARG_URI_IMAGE, uriImage);
            Pair<View, String> pairImage = new Pair<>(v, getString(R.string.transition_name_annonce_zoom));
            ActivityOptionsCompat options = makeSceneTransitionAnimation(this, pairImage);
            startActivity(intent, options.toBundle());
        } catch (ClassCastException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle arguments = (savedInstanceState != null) ? savedInstanceState : getIntent().getExtras();

        ArgumentsChecker checker = new ArgumentsChecker();
        checker.setArguments(arguments)
                .isMandatory(ARG_ANNONCE)
                .isOptional(ARG_COME_FROM_CHAT_FRAGMENT)
                .setOnFailureListener(e -> finish())
                .setOnSuccessListener(this::initActivity)
                .check();
    }

    private void initActivity(Bundle params) {

        // Récupération des arguments
        annonceFull = params.getParcelable(ARG_ANNONCE);
        if (params.containsKey(ARG_COME_FROM_CHAT_FRAGMENT)) {
            comeFromChatFragment = params.getBoolean(ARG_COME_FROM_CHAT_FRAGMENT);
        }

        // Recuperation du fragment
        fromSameAuthorFragment = (FromSameAuthorFragment) getSupportFragmentManager().getFragment(params, SAVE_FRAG_FROM_SAME_AUTHOR);

        // Récupération de l'uid de l'utilisateur connecté
        uidUser = SharedPreferencesHelper.getInstance(this).getUidFirebaseUser();

        // Instanciation d'un viewmodel
        viewModel = ViewModelProviders.of(this).get(AnnonceDetailActivityViewModel.class);

        // Création de la vue
        setContentView(R.layout.activity_annonce_detail);
        ButterKnife.bind(this);

        // Récupération de l'annonce
        initAnnonceViews(annonceFull);

        // Permet a l'ecran de ne pas se superposer a la navigation bar.
        if (Utility.hasNavigationBar(this)) {
            View decorView = getWindow().getDecorView();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            } else {
                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            }
        }

        createFragmentFromSameUser();
    }

    private void createFragmentFromSameUser() {
        // Création d'un fragment pour aller récupérer les autres annonces du même vendeur
        viewModel.getListAnnonceByUidUser(annonceFull.getAnnonce().getUidUser())
                .doOnSuccess(this::epureListThenCreateFragment)
                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage(), throwable))
                .subscribe();
    }

    private void epureListThenCreateFragment(List<AnnonceFirebase> annonceFirebases) {
        // Suppression de l'annonce actuellement en cours de visualisation de la liste
        List<AnnonceFirebase> newList = new ArrayList<>();
        for (AnnonceFirebase annonce : annonceFirebases) {
            if (!annonce.getUuid().equals(annonceFull.getAnnonce().getUid())) {
                newList.add(annonce);
            }
        }

        // Si la liste épurée n'est pas vide, je créé un nouveau fragment et je l'affiche.
        if (!newList.isEmpty() && fromSameAuthorFragment == null) {
            fromSameAuthorFragment = FromSameAuthorFragment.newInstance(newList);
            getSupportFragmentManager().beginTransaction().add(R.id.from_same_salesman_fragment, fromSameAuthorFragment).commit();
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
        if (requestCode == RC_SIGN_IN && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Connexion abandonnée", Toast.LENGTH_SHORT).show();
        }

        if (requestCode == RC_SIGN_IN && resultCode == RESULT_OK) {
            uidUser = FirebaseAuth.getInstance().getUid();
        }

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_ANNONCE, annonceFull);
        outState.putBoolean(ARG_COME_FROM_CHAT_FRAGMENT, comeFromChatFragment);
        if (fromSameAuthorFragment != null) {
            getSupportFragmentManager().putFragment(outState, SAVE_FRAG_FROM_SAME_AUTHOR, fromSameAuthorFragment);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GlideApp.get(this).clearMemory();
    }

    private void initAnnonceViews(AnnonceFull annonceFull) {
        if (annonceFull.getAnnonce() != null) {
            AnnonceEntity annonce = annonceFull.getAnnonce();

            setTitle(annonce.getTitre());

            prix.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", annonce.getPrix()) + " XPF"));
            description.setText(annonce.getDescription());
            collapsingToolbarLayout.setTitle(annonce.getTitre());
            textDatePublication.setText(DateConverter.convertDateToUiDate(annonce.getDatePublication()));
            categorieLibelle.setText(annonceFull.getCategorie().get(0).getName());

            imageFavorite.setOnClickListener(onClickListenerFavorite);
            imageShare.setOnClickListener(onClickListenerShare);

            if (annonceFull.getPhotos() != null && !annonceFull.getPhotos().isEmpty()) {
                viewPager.setAdapter(new AnnonceViewPagerAdapter(this, annonceFull.getPhotos(), onClickImage));
                indicator.setViewPager(viewPager);
            } else {
                // If no photo for this ads, we don't expand the AppBarLayout
                appBarLayout.setExpanded(false);
            }

            if (annonceFull.getUtilisateur() != null && !annonceFull.getUtilisateur().isEmpty()) {
                initViewsSeller(annonceFull.getUtilisateur().get(0));
            }

            if (uidUser != null) {
                viewModel.getCountFavoritesByUidUserAndByUidAnnonce(uidUser, annonce.getUid()).observe(this, count ->
                        imageFavorite.setImageResource((count != null && count >= 1) ? R.drawable.ic_favorite_red_700_48dp : R.drawable.ic_favorite_border_white_48dp)
                );
            }
        }
    }

    private void initViewsSeller(@NonNull UserEntity seller) {
        this.seller = seller;
        AnnonceEntity annonce = annonceFull.getAnnonce();
        GlideApp.with(this)
                .load(seller.getPhotoUrl())
                .circleCrop()
                .placeholder(R.drawable.ic_person_white_48dp)
                .error(R.drawable.ic_person_white_48dp)
                .into(imageProfilSeller);

        boolean amITheOwner = uidUser != null && !uidUser.isEmpty() && uidUser.equals(annonce.getUidUser());
        if (amITheOwner) {
            fabActionUpdate.setVisibility(View.VISIBLE);
            fabActionEmail.setVisibility(View.GONE);
            fabActionTelephone.setVisibility(View.GONE);
            fabActionMessage.setVisibility(View.GONE);
            imageFavorite.setVisibility(View.GONE);
        } else {
            fabActionUpdate.setVisibility(View.GONE);
            fabActionEmail.setVisibility((annonce.getContactByEmail() != null && "O".equals(annonce.getContactByEmail()) && seller.getEmail() != null) ? View.VISIBLE : View.GONE);
            fabActionTelephone.setVisibility((annonce.getContactByTel() != null && "O".equals(annonce.getContactByTel()) && seller.getTelephone() != null) ? View.VISIBLE : View.GONE);
            fabActionMessage.setVisibility((annonce.getContactByMsg() != null && "O".equals(annonce.getContactByMsg()) && !comeFromChatFragment) ? View.VISIBLE : View.GONE);
            imageFavorite.setVisibility(View.VISIBLE);
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
                    .setSubject(getString(R.string.app_name) + " - " + annonceFull.getAnnonce().getTitre())
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
        bundle.putString(PostAnnonceActivity.BUNDLE_UID_USER, uidUser);
        bundle.putString(PostAnnonceActivity.BUNDLE_KEY_UID_ANNONCE, annonceFull.getAnnonce().getUid());
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
        intent.putExtra(ARG_ANNONCE, annonceFull.getAnnonce());
        intent.putExtra(DATA_FIREBASE_USER_UID, FirebaseAuth.getInstance().getUid());
        startActivity(intent);
        overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }
}
