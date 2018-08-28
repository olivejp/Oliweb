package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.service.sharing.DynamicLynksGenerator;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.FavoriteActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Utility;

import static android.support.v4.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.MainActivity.RC_SIGN_IN;

public class ListFavoritesFragment extends Fragment {
    private static final String TAG = ListFavoritesFragment.class.getName();

    private static final String LOADING_DIALOG = "LOADING_DIALOG";

    private static final String ARG_UID_USER = "ARG_UID_USER";

    public static final String SAVE_LIST_ANNONCE = "SAVE_LIST_ANNONCE";

    @BindView(R.id.recycler_list_annonces)
    RecyclerView recyclerView;

    @BindView(R.id.empty_favorite_linear)
    LinearLayout linearLayout;

    @BindView(R.id.coordinator_layout_list_annonce)
    CoordinatorLayout coordinatorLayout;

    private String uidUser;
    private AppCompatActivity appCompatActivity;
    private FavoriteActivityViewModel viewModel;
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private ArrayList<AnnonceFull> annoncePhotosList = new ArrayList<>();
    private ActionBar actionBar;
    private LoadingDialogFragment loadingDialogFragment;

    /**
     * OnClickListener that should open AnnonceDetailActivity
     */
    private View.OnClickListener onClickListener = (View v) -> {
        AnnonceBeautyAdapter.ViewHolderBeauty viewHolderBeauty = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
        Intent intent = new Intent(appCompatActivity, AnnonceDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_ANNONCE, viewHolderBeauty.getAnnoncePhotos());
        intent.putExtras(bundle);
        Pair<View, String> pairImage = new Pair<>(viewHolderBeauty.getImageView(), getString(R.string.image_detail_transition));
        ActivityOptionsCompat options = makeSceneTransitionAnimation(appCompatActivity, pairImage);
        startActivity(intent, options.toBundle());
    };

    /**
     * OnClickListener that share an annonce with a DynamicLink
     */
    private View.OnClickListener onClickListenerShare = v -> {
        if (uidUser != null && !uidUser.isEmpty()) {
            AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
            AnnonceFull annoncePhotos = viewHolder.getAnnoncePhotos();
            AnnonceEntity annonceEntity = annoncePhotos.getAnnonce();

            // Display a loading spinner
            loadingDialogFragment = new LoadingDialogFragment();
            loadingDialogFragment.setText(getString(R.string.dynamic_link_creation));
            loadingDialogFragment.show(appCompatActivity.getSupportFragmentManager(), LOADING_DIALOG);

            DynamicLynksGenerator.generateShortLink(uidUser, annonceEntity, annoncePhotos.photos, new DynamicLynksGenerator.DynamicLinkListener() {
                @Override
                public void getLink(Uri shortLink, Uri flowchartLink) {
                    loadingDialogFragment.dismiss();
                    Intent sendIntent = new Intent();
                    String msg = getString(R.string.default_text_share_link) + shortLink;
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                }

                @Override
                public void getLinkError() {
                    loadingDialogFragment.dismiss();
                    Snackbar.make(coordinatorLayout, R.string.link_share_error, Snackbar.LENGTH_LONG).show();
                }
            });
        } else {
            Snackbar.make(coordinatorLayout, R.string.sign_in_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_in, v1 -> Utility.signIn(appCompatActivity, RC_SIGN_IN))
                    .show();
        }
    };

    private View.OnClickListener onClickListenerFavorite = (View v) -> {
        if (uidUser == null || uidUser.isEmpty()) {
            Snackbar.make(coordinatorLayout, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.sign_in), v1 -> Utility.signIn(appCompatActivity, RC_SIGN_IN))
                    .show();
        } else {
            AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
            viewModel.removeFromFavorite(FirebaseAuth.getInstance().getUid(), viewHolder.getAnnoncePhotos())
                    .observeOnce(isRemoved -> {
                        if (isRemoved != null) {
                            switch (isRemoved) {
                                case REMOVE_SUCCESSFUL:
                                    Snackbar.make(recyclerView, R.string.annonce_remove_from_favorite, Snackbar.LENGTH_LONG).show();
                                    break;
                                case REMOVE_FAILED:
                                    Toast.makeText(appCompatActivity, R.string.remove_from_favorite_failed, Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
        }
    };

    public ListFavoritesFragment() {
        // Empty constructor
    }

    public static synchronized ListFavoritesFragment getInstance(String uidUtilisateur) {
        ListFavoritesFragment listAnnonceFragment = new ListFavoritesFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_UID_USER, uidUtilisateur);
        listAnnonceFragment.setArguments(bundle);
        return listAnnonceFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            appCompatActivity = (AppCompatActivity) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "Context should be an AppCompatActivity and implements SignInActivity", e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            uidUser = getArguments().getString(ARG_UID_USER);
        }
        viewModel = ViewModelProviders.of(this).get(FavoriteActivityViewModel.class);
        viewModel.getLiveUserConnected().observe(this, userEntity ->
                uidUser = (userEntity != null) ? userEntity.getUid() : null
        );

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_annonce, container, false);

        ButterKnife.bind(this, view);

        Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.network_unavailable, BaseTransientBottomBar.LENGTH_INDEFINITE);
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setBackgroundColor(appCompatActivity.getResources().getColor(R.color.colorAccentDarker));
        annonceBeautyAdapter = new AnnonceBeautyAdapter(appCompatActivity.getResources().getColor(R.color.colorPrimary),
                onClickListener,
                onClickListenerShare,
                onClickListenerFavorite);

        recyclerView.setAdapter(annonceBeautyAdapter);

        actionBar = appCompatActivity.getSupportActionBar();

        if (savedInstanceState != null) {
            annoncePhotosList = savedInstanceState.getParcelableArrayList(SAVE_LIST_ANNONCE);
            annonceBeautyAdapter.setListAnnonces(annoncePhotosList);
        }

        initAccordingToAction();

        return view;
    }

    @Override
    public void onDestroyView() {
        GlideApp.get(appCompatActivity).clearMemory();
        recyclerView.setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVE_LIST_ANNONCE, annoncePhotosList);
    }

    private void initAccordingToAction() {
        if (uidUser != null) {
            if (actionBar != null) {
                actionBar.setTitle(R.string.MY_FAVORITE);
            }
            viewModel.getFavoritesByUidUser(uidUser).observe(this, annoncePhotos -> {
                if (annoncePhotos != null && !annoncePhotos.isEmpty()) {
                    Utility.initGridLayout(appCompatActivity, recyclerView, annonceBeautyAdapter);
                    annoncePhotosList = (ArrayList<AnnonceFull>) annoncePhotos;
                    annonceBeautyAdapter.setListAnnonces(annoncePhotosList);
                    linearLayout.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    linearLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            });
        }
    }
}
