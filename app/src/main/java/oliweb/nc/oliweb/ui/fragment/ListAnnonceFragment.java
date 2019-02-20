package oliweb.nc.oliweb.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
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
import androidx.recyclerview.widget.LinearLayoutManager;
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
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchHitsResult;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchResult;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.ui.EndlessRecyclerOnScrollListener;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.FavoritesActivity;
import oliweb.nc.oliweb.ui.activity.SnackbarViewProvider;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.dialog.ListAnnonceBottomSheetDialog;
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
import static oliweb.nc.oliweb.ui.dialog.ListAnnonceBottomSheetDialog.ANNONCE_FULL;
import static oliweb.nc.oliweb.ui.fragment.ListCategorieFragment.ID_ALL_CATEGORY;
import static oliweb.nc.oliweb.utility.Constants.REMOTE_DELAY;

public class ListAnnonceFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final String TAG = ListAnnonceFragment.class.getName();

    private static final String SAVE_LIST_ANNONCE = "SAVE_LIST_ANNONCE";
    private static final String SAVE_SORT = "SAVE_SORT";
    private static final String SAVE_DIRECTION = "SAVE_DIRECTION";
    private static final String SAVE_CATEGORY_SELECTED = "SAVE_CATEGORY_SELECTED";
    private static final String SAVE_TOTAL_LOADED = "SAVE_TOTAL_LOADED";
    private static final String SAVE_ACTUAL_SORT = "SAVE_ACTUAL_SORT";
    private static final String SAVE_IS_SEARCHING = "SAVE_IS_SEARCHING";

    @BindView(R.id.swipe_refresh_layout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_list_annonces)
    RecyclerView recyclerView;

    @BindView(R.id.constraint_list_annonce)
    ConstraintLayout constraintLayout;

    @BindView(R.id.coordinator_layout_list_annonce)
    CoordinatorLayout coordinatorLayout;

    @BindView(R.id.list_annonce_image_timeout)
    ImageView imageViewTimeout;

    @BindView(R.id.text_annonce_timeout)
    TextView textViewTimeout;

    private String uidUser;
    private AppCompatActivity appCompatActivity;
    private MainActivityViewModel viewModel;
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private ArrayList<AnnonceFull> annoncePhotosList = new ArrayList<>();
    private int sortSelected = SORT_DATE;
    private int directionSelected;
    private ActionBar actionBar;
    private EndlessRecyclerOnScrollListener scrollListener;
    private List<String> listUidFavorites = new ArrayList<>();
    private CategorieEntity categorieSelected;
    private int from = 0;
    private Long totalLoaded = 0L;
    private int actualSort;
    private Long delay;
    private Disposable disposableActualSearch;
    private SnackbarViewProvider snackbarViewProvider;
    private boolean isSearching = false;

    /**
     * OnClickListener that should open AnnonceDetailActivity
     */
    private View.OnClickListener onClickListener = (View v) -> {
        AnnonceBeautyAdapter.CommonViewHolder viewHolder = (AnnonceBeautyAdapter.CommonViewHolder) v.getTag();
        Intent intent = new Intent(appCompatActivity, AnnonceDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_ANNONCE, viewHolder.getAnnonceFull());
        intent.putExtras(bundle);
        Pair<View, String> pairImage = null;
        if (viewHolder instanceof AnnonceBeautyAdapter.ViewHolderBeauty) {
            pairImage = new Pair<>(((AnnonceBeautyAdapter.ViewHolderBeauty) viewHolder).getImageView(), getString(R.string.image_detail_transition));
        }
        Pair<View, String> pairImageUser = new Pair<>(viewHolder.getImageUserBeauty(), getString(R.string.image_detail_transition_user));

        ActivityOptionsCompat options;
        if (pairImage != null) {
            options = makeSceneTransitionAnimation(appCompatActivity, pairImage, pairImageUser);
        } else {
            options = makeSceneTransitionAnimation(appCompatActivity, pairImageUser);
        }

        startActivity(intent, options.toBundle());
    };

    /**
     * OnClickListener that open the Bottom sheet
     */
    private View.OnClickListener onClickListenerMore = v -> {
        AnnonceBeautyAdapter.CommonViewHolder viewHolder = (AnnonceBeautyAdapter.CommonViewHolder) v.getTag();
        ListAnnonceBottomSheetDialog listAnnonceBottomSheetDialog = new ListAnnonceBottomSheetDialog();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ANNONCE_FULL, viewHolder.getAnnonceFull());
        listAnnonceBottomSheetDialog.setArguments(bundle);
        listAnnonceBottomSheetDialog.show(appCompatActivity.getSupportFragmentManager(), "TAG");
    };

    public ListAnnonceFragment() {
        // Empty constructor
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
            isSearching = savedInstanceState.getBoolean(SAVE_IS_SEARCHING);
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
                launchNewSearch();
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
        swipeRefreshLayout.setOnRefreshListener(null);
        clearDisposableActualSearch();
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVE_LIST_ANNONCE, annoncePhotosList);
        outState.putInt(SAVE_SORT, sortSelected);
        outState.putInt(SAVE_DIRECTION, directionSelected);
        outState.putParcelable(SAVE_CATEGORY_SELECTED, categorieSelected);
        outState.putLong(SAVE_TOTAL_LOADED, totalLoaded);
        outState.putInt(SAVE_ACTUAL_SORT, actualSort);
        outState.putBoolean(SAVE_IS_SEARCHING, isSearching);
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        launchNewSearch();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_annonce, container, false);

        ButterKnife.bind(this, view);

        Snackbar snackbar = Snackbar.make(snackbarViewProvider.getSnackbarViewProvider(), R.string.network_unavailable, Snackbar.LENGTH_INDEFINITE);
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setBackgroundColor(ContextCompat.getColor(appCompatActivity, R.color.colorAccentDarker));

        annonceBeautyAdapter = new AnnonceBeautyAdapter(onClickListener, onClickListenerMore, this);
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

        if (isSearching) {
            launchNewSearch();
        }

        return view;
    }


    private void callFavoriteAnnonceActivity() {
        Intent intent = new Intent();
        intent.setClass(appCompatActivity, FavoritesActivity.class);
        intent.putExtra(ARG_UID_USER, uidUser);
        startActivity(intent);
        appCompatActivity.overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }

    private void clearDisposableActualSearch() {
        if (disposableActualSearch != null && !disposableActualSearch.isDisposed())
            disposableActualSearch.dispose();
    }

    private void displayTimeoutViews(boolean timeoutActive) {
        imageViewTimeout.setVisibility(timeoutActive ? View.VISIBLE : View.INVISIBLE);
        textViewTimeout.setVisibility(timeoutActive ? View.VISIBLE : View.INVISIBLE);
        // Fix : On ne peut pas swipper si le swipeRefresh est invisible
        // swipeRefreshLayout.setVisibility(timeoutActive ? View.INVISIBLE : View.VISIBLE);
    }

    private void launchNewSearch() {
        if (showToastIfNoConnectivity()) return;

        isSearching = true;
        swipeRefreshLayout.setRefreshing(true);
        displayTimeoutViews(false);
        clearDisposableActualSearch();
        from = 0;
        totalLoaded = 0L;
        annoncePhotosList.clear();
        loadMoreDatas();
    }

    private void initAccordingToAction() {
        if (actionBar != null) {
            actionBar.setTitle(R.string.RECENT_ADS);
        }
        LinearLayoutManager layoutManager = Utility.initLinearLayout(appCompatActivity, recyclerView);
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
            launchNewSearch();
        }

        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void loadMoreDatas() {
        if (showToastIfNoConnectivity()) return;
        clearDisposableActualSearch();
        List<String> listCategorie = Collections.emptyList();
        if (categorieSelected != null && !categorieSelected.getId().equals(ID_ALL_CATEGORY)) {
            listCategorie = Collections.singletonList(categorieSelected.getName());
        }
        disposableActualSearch = viewModel.getSearchEngine().searchMaybe(listCategorie, false, 0, 0, null, Constants.PER_PAGE_REQUEST, from, sortSelected, directionSelected)
                .subscribeOn(Schedulers.io())
                .timeout(delay, TimeUnit.SECONDS, AndroidSchedulers.mainThread(), Maybe.empty())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorComplete(throwable -> throwable instanceof TimeoutException)
                .doOnError(throwable -> dismissLoading())
                .doOnSuccess(this::doOnSuccessSearch)
                .doOnComplete(this::doOnCompleteSearch)
                .doAfterTerminate(this::dismissLoading)
                .subscribe();
    }

    private boolean showToastIfNoConnectivity() {
        if (!NetworkReceiver.checkConnection(appCompatActivity)) {
            dismissLoading();
            Toast.makeText(appCompatActivity, "Aucune recherche possible sans connexion", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
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
        annonceBeautyAdapter.setListAnnonces(annoncePhotosList);
        annonceBeautyAdapter.notifyDataSetChanged();
        displayTimeoutViews(true);
    }

    private void dismissLoading() {
        isSearching = false;
        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
}
