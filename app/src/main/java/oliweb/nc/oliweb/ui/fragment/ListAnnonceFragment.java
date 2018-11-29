package oliweb.nc.oliweb.ui.fragment;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Maybe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchHitsResult;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchResult;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.service.sharing.DynamicLinksGenerator;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.ui.EndlessRecyclerOnScrollListener;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.FavoritesActivity;
import oliweb.nc.oliweb.ui.activity.SnackbarViewProvider;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static oliweb.nc.oliweb.service.search.SearchEngine.ASC;
import static oliweb.nc.oliweb.service.search.SearchEngine.DESC;
import static oliweb.nc.oliweb.service.search.SearchEngine.SORT_DATE;
import static oliweb.nc.oliweb.service.search.SearchEngine.SORT_PRICE;
import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.FavoritesActivity.ARG_UID_USER;
import static oliweb.nc.oliweb.ui.activity.MainActivity.RC_SIGN_IN;
import static oliweb.nc.oliweb.ui.fragment.ListCategorieFragment.ID_ALL_CATEGORY;
import static oliweb.nc.oliweb.utility.Constants.REMOTE_DELAY;

public class ListAnnonceFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = ListAnnonceFragment.class.getName();

    private static final String LOADING_DIALOG = "LOADING_DIALOG";

    private static final String SAVE_LIST_ANNONCE = "SAVE_LIST_ANNONCE";
    private static final String SAVE_SORT = "SAVE_SORT";
    private static final String SAVE_DIRECTION = "SAVE_DIRECTION";
    private static final int REQUEST_WRITE_EXTERNAL_PERMISSION_CODE = 101;
    private static final String SAVE_ANNONCE_FAVORITE = "SAVE_ANNONCE_FAVORITE";
    private static final String SAVE_CATEGORY_SELECTED = "SAVE_CATEGORY_SELECTED";
    private static final String SAVE_TOTAL_LOADED = "SAVE_TOTAL_LOADED";
    private static final String SAVE_ACTUAL_SORT = "SAVE_ACTUAL_SORT";

    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_list_annonces)
    RecyclerView recyclerView;

    @BindView(R.id.constraint_list_annonce)
    ConstraintLayout constraintLayout;

    @BindView(R.id.coordinator_layout_list_annonce)
    CoordinatorLayout coordinatorLayout;

    @BindView(R.id.list_annonce_image_timeout)
    ImageView imageViewTimeout;

    @BindView(R.id.list_annonce_timeout)
    TextView textViewTimeout;

    private String uidUser;
    private AppCompatActivity appCompatActivity;
    private MainActivityViewModel viewModel;
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private ArrayList<AnnonceFull> annoncePhotosList = new ArrayList<>();
    private int sortSelected = SORT_DATE;
    private int directionSelected;
    private ActionBar actionBar;
    private LoadingDialogFragment loadingDialogFragment;
    private EndlessRecyclerOnScrollListener scrollListener;
    private List<String> listUidFavorites = new ArrayList<>();
    private AnnonceFull annonceFullToSaveTofavorite;
    private View viewToEnabled;
    private CategorieEntity categorieSelected;
    private int from = 0;
    private Long totalLoaded = 0L;
    private int actualSort;
    private Long delay;
    private Disposable actualSearch;
    private SnackbarViewProvider snackbarViewProvider;

    /**
     * OnClickListener that should open AnnonceDetailActivity
     */
    private View.OnClickListener onClickListener = (View v) -> {
        AnnonceBeautyAdapter.ViewHolderBeauty viewHolderBeauty = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
        Intent intent = new Intent(appCompatActivity, AnnonceDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_ANNONCE, viewHolderBeauty.getAnnonceFull());
        intent.putExtras(bundle);
        Pair<View, String> pairImage = new Pair<>(viewHolderBeauty.getImageView(), getString(R.string.image_detail_transition));
        Pair<View, String> pairImageUser = new Pair<>(viewHolderBeauty.getImageUserBeauty(), getString(R.string.image_detail_transition_user));
        Pair<View, String> pairImageShare = new Pair<>(viewHolderBeauty.getImageShare(), getString(R.string.image_share_transition));
        Pair<View, String> pairImageFavorite = new Pair<>(viewHolderBeauty.getImageFavorite(), getString(R.string.image_favorite_transition));
        ActivityOptionsCompat options = makeSceneTransitionAnimation(appCompatActivity, pairImage, pairImageUser, pairImageShare, pairImageFavorite);
        startActivity(intent, options.toBundle());
    };

    /**
     * OnClickListener that share an annonce with a DynamicLink
     */
    private View.OnClickListener onClickListenerShare = v -> {
        if (uidUser != null && !uidUser.isEmpty()) {
            AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
            AnnonceFull annonceFull = viewHolder.getAnnonceFull();
            AnnonceEntity annonceEntity = annonceFull.getAnnonce();

            // Display a loading spinner
            loadingDialogFragment = new LoadingDialogFragment();
            loadingDialogFragment.setText(getString(R.string.dynamic_link_creation));
            loadingDialogFragment.show(appCompatActivity.getSupportFragmentManager(), LOADING_DIALOG);

            // Génération d'un lien
            DynamicLink link = DynamicLinksGenerator.generateLong(uidUser, annonceEntity, annonceFull.photos);
            DynamicLinksGenerator.generateShortWithLong(link.getUri(), new DynamicLinksGenerator.DynamicLinkListener() {
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
                    Snackbar.make(snackbarViewProvider.getSnackbarViewProvider(), R.string.link_share_error, Snackbar.LENGTH_LONG).show();
                }
            });
        } else {
            Snackbar.make(snackbarViewProvider.getSnackbarViewProvider(), R.string.sign_in_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_in, v1 -> Utility.signIn(appCompatActivity, RC_SIGN_IN))
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
            // User not logged
            Snackbar.make(snackbarViewProvider.getSnackbarViewProvider(), getString(R.string.sign_in_required), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.sign_in), v1 -> Utility.signIn(appCompatActivity, RC_SIGN_IN))
                    .show();
            v.setEnabled(true);
        } else {
            // User logged
            AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();

            annonceFullToSaveTofavorite = viewHolder.getAnnonceFull();
            viewToEnabled = v;

            // Ask for permission to write on the external storage of the device
            if (checkPermissionMversion()) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_PERMISSION_CODE);
            } else {
                callAddOrRemoveFromFavorite();
            }
        }
    };

    public ListAnnonceFragment() {
        // Empty constructor
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_EXTERNAL_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callAddOrRemoveFromFavorite();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            appCompatActivity = (AppCompatActivity) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "Context should be an AppCompatActivity", e);
        }

        try {
            snackbarViewProvider = (SnackbarViewProvider) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "Context should implement SnackbarViewProvider", e);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        appCompatActivity = null;
        snackbarViewProvider = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(appCompatActivity).get(MainActivityViewModel.class);

        if (savedInstanceState != null) {
            sortSelected = savedInstanceState.getInt(SAVE_SORT);
            directionSelected = savedInstanceState.getInt(SAVE_DIRECTION);
            annoncePhotosList = savedInstanceState.getParcelableArrayList(SAVE_LIST_ANNONCE);
            categorieSelected = savedInstanceState.getParcelable(SAVE_CATEGORY_SELECTED);
            totalLoaded = savedInstanceState.getLong(SAVE_TOTAL_LOADED);
            actualSort = savedInstanceState.getInt(SAVE_ACTUAL_SORT);
        }

        viewModel.getLiveUserConnected().observe(appCompatActivity, userEntity -> {
            uidUser = (userEntity != null) ? userEntity.getUid() : null;
            viewModel.getFavoritesByUidUser(uidUser).observe(appCompatActivity, annonceFulls -> {
                listUidFavorites.clear();
                if (annonceFulls != null) {
                    for (AnnonceFull annonceFull : annonceFulls) {
                        listUidFavorites.add(annonceFull.getAnnonce().getUid());
                    }
                }
                updateListWithFavorite(annoncePhotosList, listUidFavorites);
                updateListAdapter();
            });
        });

        // Dès que la catégorie sélectionnée aura changé, je vais relancer une recherche...
        viewModel.getCategorySelected().observe(appCompatActivity, categorieEntity -> {
            if (categorieEntity != null && (categorieSelected == null || !categorieEntity.getId().equals(categorieSelected.getId()))) {
                categorieSelected = categorieEntity;
                makeNewSearch();
            }
        });

        // Récupération du délai configuré avant d'envoyer un Timeoutexception
        this.delay = FirebaseRemoteConfig.getInstance().getLong(REMOTE_DELAY);
    }

    @Override
    public void onDestroyView() {
        GlideApp.get(appCompatActivity).clearMemory();
        recyclerView.setAdapter(null);
        recyclerView.removeOnScrollListener(scrollListener);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVE_LIST_ANNONCE, annoncePhotosList);
        outState.putInt(SAVE_SORT, sortSelected);
        outState.putInt(SAVE_DIRECTION, directionSelected);
        outState.putParcelable(SAVE_ANNONCE_FAVORITE, annonceFullToSaveTofavorite);
        outState.putParcelable(SAVE_CATEGORY_SELECTED, categorieSelected);
        outState.putLong(SAVE_TOTAL_LOADED, totalLoaded);
        outState.putInt(SAVE_ACTUAL_SORT, actualSort);
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        makeNewSearch();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_annonce, container, false);

        ButterKnife.bind(this, view);

        Snackbar snackbar = Snackbar.make(snackbarViewProvider.getSnackbarViewProvider(), R.string.network_unavailable, Snackbar.LENGTH_INDEFINITE);
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setBackgroundColor(ContextCompat.getColor(appCompatActivity, R.color.colorAccentDarker));
        annonceBeautyAdapter = new AnnonceBeautyAdapter(onClickListener, onClickListenerShare, onClickListenerFavorite, null, this);

        recyclerView.setAdapter(annonceBeautyAdapter);

        actionBar = appCompatActivity.getSupportActionBar();

        if (savedInstanceState != null) {
            annoncePhotosList = savedInstanceState.getParcelableArrayList(SAVE_LIST_ANNONCE);
            updateListAdapter();
        }

        swipeRefreshLayout.setOnRefreshListener(this);

        viewModel.getLiveSort().observe(appCompatActivity, this::changeSortAndUpdateList);

        viewModel.getIsNetworkAvailable().observe(appCompatActivity, atomicBoolean -> {
            if (atomicBoolean != null && !atomicBoolean.get()) {
                snackbar.show();
            } else {
                snackbar.dismiss();
            }
        });

        initAccordingToAction();

        if (savedInstanceState == null) {
            changeSortAndUpdateList(SharedPreferencesHelper.getInstance(appCompatActivity).getPrefSort());
        }

        return view;
    }

    private boolean checkPermissionMversion() {
        return Build.VERSION.SDK_INT >= 23 &&
                viewModel.getMediaUtility().isExternalStorageAvailable() &&
                viewModel.getMediaUtility().allPermissionsAreGranted(appCompatActivity.getApplicationContext(), Collections.singletonList(Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    private void callAddOrRemoveFromFavorite() {
        viewModel.addOrRemoveFromFavorite(uidUser, annonceFullToSaveTofavorite).observeOnce(addRemoveFromFavorite -> {
            if (addRemoveFromFavorite != null) {
                switch (addRemoveFromFavorite) {
                    case ONE_OF_YOURS:
                        Toast.makeText(getContext(), R.string.action_impossible_own_this_annonce, Toast.LENGTH_LONG).show();
                        break;
                    case ADD_SUCCESSFUL:
                        Snackbar.make(snackbarViewProvider.getSnackbarViewProvider(), R.string.AD_ADD_TO_FAVORITE, Snackbar.LENGTH_LONG)
                                .setAction(R.string.MY_FAVORITE, v12 -> callFavoriteAnnonceActivity())
                                .show();
                        break;
                    case REMOVE_SUCCESSFUL:
                        Snackbar.make(snackbarViewProvider.getSnackbarViewProvider(), R.string.annonce_remove_from_favorite, Snackbar.LENGTH_LONG).show();
                        break;
                    case REMOVE_FAILED:
                        Toast.makeText(getContext(), R.string.remove_from_favorite_failed, Toast.LENGTH_LONG).show();
                        break;
                    default:
                        break;
                }
            }
            if (viewToEnabled != null) {
                viewToEnabled.setEnabled(true);
            }
        });
    }

    private void callFavoriteAnnonceActivity() {
        Intent intent = new Intent();
        intent.setClass(appCompatActivity, FavoritesActivity.class);
        intent.putExtra(ARG_UID_USER, uidUser);
        startActivity(intent);
        appCompatActivity.overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }

    private void clearActualSearch() {
        if (actualSearch != null && !actualSearch.isDisposed()) actualSearch.dispose();
    }

    private void displayTimeoutViews(boolean timeoutActive) {
        imageViewTimeout.setVisibility(timeoutActive ? View.VISIBLE : View.GONE);
        textViewTimeout.setVisibility(timeoutActive ? View.VISIBLE : View.GONE);
        swipeRefreshLayout.setVisibility(timeoutActive ? View.GONE : View.VISIBLE);
    }

    private void makeNewSearch() {
        swipeRefreshLayout.setRefreshing(true);
        if (toastIfNoConnectivity()) return;
        displayTimeoutViews(false);
        clearActualSearch();
        from = 0;
        totalLoaded = 0L;
        annoncePhotosList.clear();
        loadMoreDatas();
    }

    private void initAccordingToAction() {
        if (actionBar != null) {
            actionBar.setTitle(R.string.RECENT_ADS);
        }
        RecyclerView.LayoutManager layoutManager = Utility.initGridLayout(appCompatActivity, recyclerView);
        scrollListener = new EndlessRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onLoadMore() {
                // Tant que la liste en cours est plus petite que le nombre total
                if (annoncePhotosList.size() < totalLoaded) {
                    from = annoncePhotosList.size() + 1;
                    loadMoreDatas();
                }
            }
        };
        recyclerView.addOnScrollListener(scrollListener);
    }

    private void changeSortAndUpdateList(Integer newSort) {
        if (newSort != null && newSort != actualSort) {
            actualSort = newSort;
            switch (newSort) {
                case 0:
                    sortSelected = SORT_PRICE;
                    directionSelected = DESC;
                    break;
                case 1:
                    sortSelected = SORT_PRICE;
                    directionSelected = ASC;
                    break;
                case 2:
                    sortSelected = SORT_DATE;
                    directionSelected = DESC;
                    break;
                case 3:
                default:
                    sortSelected = SORT_DATE;
                    directionSelected = ASC;
                    break;
            }
            makeNewSearch();
        }

        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void loadMoreDatas() {
        if (toastIfNoConnectivity()) return;
        clearActualSearch();
        List<String> listCategorie = Collections.emptyList();
        if (categorieSelected != null && !categorieSelected.getId().equals(ID_ALL_CATEGORY)) {
            listCategorie = Collections.singletonList(categorieSelected.getName());
        }
        actualSearch = viewModel.getSearchEngine().searchMaybe(listCategorie, false, 0, 0, null, Constants.PER_PAGE_REQUEST, from, sortSelected, directionSelected)
                .subscribeOn(Schedulers.io())
                .timeout(delay, TimeUnit.SECONDS, AndroidSchedulers.mainThread(), Maybe.empty())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorComplete(throwable -> throwable instanceof TimeoutException)
                .doOnError(throwable -> {
                    try {
                        dismissLoading();
                    } catch (RuntimeException e) {
                        Log.e(TAG, throwable.getLocalizedMessage(), throwable);
                    } finally {
                        dismissLoading();
                    }
                })
                .doOnSuccess(this::doOnSuccessSearch)
                .doOnComplete(this::doOnCompleteSearch)
                .doAfterTerminate(this::dismissLoading)
                .subscribe();
    }

    private boolean toastIfNoConnectivity() {
        if (!checkConnectivity()) {
            dismissLoading();
            Toast.makeText(appCompatActivity, "Aucune recherche possible sans connexion", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private boolean checkConnectivity() {
        return NetworkReceiver.checkConnection(appCompatActivity);
    }

    private void updateListWithFavorite(ArrayList<AnnonceFull> listAnnonces, List<String> listUidFavorites) {
        if (listAnnonces == null || listAnnonces.isEmpty()) return;
        for (AnnonceFull annonceFull : listAnnonces) {
            if (listUidFavorites != null && listUidFavorites.contains(annonceFull.getAnnonce().getUid())) {
                annonceFull.getAnnonce().setFavorite(1);
            } else {
                annonceFull.getAnnonce().setFavorite(0);
            }
        }
    }

    private void updateListAdapter() {
        annonceBeautyAdapter.setListAnnonces(annoncePhotosList);
        annonceBeautyAdapter.notifyDataSetChanged();
    }

    private void doOnSuccessSearch(ElasticsearchHitsResult elasticsearchHitsResult) {
        ArrayList<AnnonceFull> listResultSearch = new ArrayList<>();
        if (elasticsearchHitsResult != null && elasticsearchHitsResult.getHits() != null && !elasticsearchHitsResult.getHits().isEmpty()) {
            totalLoaded = elasticsearchHitsResult.getTotal();
            for (ElasticsearchResult<AnnonceFirebase> result : elasticsearchHitsResult.getHits()) {
                AnnonceFull annonceFull = AnnonceConverter.convertDtoToAnnonceFull(result.get_source());
                listResultSearch.add(annonceFull);
            }
        }

        updateListWithFavorite(listResultSearch, listUidFavorites);
        annoncePhotosList.addAll(listResultSearch);
        annonceBeautyAdapter.setListAnnonces(annoncePhotosList);
        annonceBeautyAdapter.notifyDataSetChanged();
    }

    private void doOnCompleteSearch() {
        dismissLoading();
        annonceBeautyAdapter.setListAnnonces(annoncePhotosList);
        annonceBeautyAdapter.notifyDataSetChanged();
        displayTimeoutViews(true);
    }

    private void dismissLoading() {
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
