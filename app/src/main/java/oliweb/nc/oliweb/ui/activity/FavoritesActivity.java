package oliweb.nc.oliweb.ui.activity;


import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.service.firebase.DynamicLinkService;
import oliweb.nc.oliweb.ui.activity.viewmodel.FavoriteActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.ArgumentsChecker;
import oliweb.nc.oliweb.utility.Utility;

import static androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.MainActivity.RC_SIGN_IN;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class FavoritesActivity extends AppCompatActivity {

    public static final String TAG = AnnonceDetailActivity.class.getCanonicalName();

    private static final String LOADING_DIALOG = "LOADING_DIALOG";

    public static final String ARG_UID_USER = "ARG_UID_USER";

    @BindView(R.id.recycler_list_annonces)
    RecyclerView recyclerView;

    @BindView(R.id.empty_favorite_linear)
    LinearLayout emptyLinearLayout;

    @BindView(R.id.coordinator_layout_list_annonce)
    CoordinatorLayout coordinatorLayout;

    private String uidUser;
    private FavoriteActivityViewModel viewModel;
    private LoadingDialogFragment loadingDialogFragment;
    private AnnonceBeautyAdapter annonceBeautyAdapter;

    /**
     * OnClickListener that should open AnnonceDetailActivity
     */
    private View.OnClickListener onClickListener = (View v) -> {
        AnnonceBeautyAdapter.ViewHolderBeauty viewHolderBeauty = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
        Intent intent = new Intent(this, AnnonceDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_ANNONCE, viewHolderBeauty.getAnnonceFull());
        intent.putExtras(bundle);
        Pair<View, String> pairImage = new Pair<>(viewHolderBeauty.getImageView(), getString(R.string.image_detail_transition));
        ActivityOptionsCompat options = makeSceneTransitionAnimation(this, pairImage);
        startActivity(intent, options.toBundle());
    };

    /**
     * OnClickListener that share an annonce with a DynamicLink
     */
    private View.OnClickListener onClickListenerShare = v -> {
        if (uidUser != null && !uidUser.isEmpty()) {
            AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
            AnnonceFull annonceFull = viewHolder.getAnnonceFull();

            // Display a loading spinner
            loadingDialogFragment = new LoadingDialogFragment();
            loadingDialogFragment.setText(getString(R.string.dynamic_link_creation));
            loadingDialogFragment.show(getSupportFragmentManager(), LOADING_DIALOG);

            // Partage du lien dynamique
            DynamicLinkService.shareDynamicLink(this, annonceFull, uidUser, loadingDialogFragment, coordinatorLayout);
        } else {
            Snackbar.make(coordinatorLayout, R.string.sign_in_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_in, v1 -> Utility.signIn(this, RC_SIGN_IN))
                    .show();
        }
    };

    private View.OnClickListener onClickListenerFavorite = (View v) -> {
        v.setEnabled(false);
        if (uidUser == null || uidUser.isEmpty()) {
            Snackbar.make(coordinatorLayout, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.sign_in), v1 -> Utility.signIn(this, RC_SIGN_IN))
                    .show();
            v.setEnabled(true);
        } else {
            AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
            viewModel.removeFromFavorite(FirebaseAuth.getInstance().getUid(), viewHolder.getAnnonceFull())
                    .observeOnce(isRemoved -> {
                        if (isRemoved != null) {
                            switch (isRemoved) {
                                case REMOVE_SUCCESSFUL:
                                    Snackbar.make(recyclerView, R.string.annonce_remove_from_favorite, Snackbar.LENGTH_LONG).show();
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = (savedInstanceState != null) ? savedInstanceState : getIntent().getExtras();
        ArgumentsChecker argumentsChecker = new ArgumentsChecker();
        argumentsChecker
                .setArguments(args)
                .isMandatory(ARG_UID_USER)
                .setOnSuccessListener(this::initActivity)
                .setOnFailureListener(e -> finish())
                .check();
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
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GlideApp.get(this).clearMemory();
    }

    private void initActivity(Bundle args) {
        uidUser = args.getString(ARG_UID_USER);
        viewModel = ViewModelProviders.of(this).get(FavoriteActivityViewModel.class);

        // Init views
        setContentView(R.layout.activity_list_favorite);
        ButterKnife.bind(this);
        annonceBeautyAdapter = new AnnonceBeautyAdapter(onClickListener, onClickListenerShare, onClickListenerFavorite, this, null);

        RecyclerView.LayoutManager layoutManager = Utility.initGridLayout(this, recyclerView);
        recyclerView.setAdapter(annonceBeautyAdapter);
        recyclerView.setLayoutManager(layoutManager);

        viewModel.getFavoritesByUidUser(uidUser).observe(this, annonceWithPhotos -> {
            if (annonceWithPhotos == null || annonceWithPhotos.isEmpty()) {
                initEmptyLayout();
            } else {
                initLayout(annonceWithPhotos);
            }
        });
    }

    private void initLayout(List<AnnonceFull> listAnnonces) {
        recyclerView.setVisibility(View.VISIBLE);
        emptyLinearLayout.setVisibility(View.GONE);
        annonceBeautyAdapter.setListAnnonces(listAnnonces);
        annonceBeautyAdapter.notifyDataSetChanged();
    }

    private void initEmptyLayout() {
        emptyLinearLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }
}
