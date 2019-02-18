package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
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
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchHitsResult;
import oliweb.nc.oliweb.dto.elasticsearch.ElasticsearchResult;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.service.firebase.DynamicLinkService;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.ui.EndlessRecyclerOnScrollListener;
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;

import static androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static oliweb.nc.oliweb.service.search.SearchEngine.ASC;
import static oliweb.nc.oliweb.service.search.SearchEngine.DESC;
import static oliweb.nc.oliweb.service.search.SearchEngine.SORT_DATE;
import static oliweb.nc.oliweb.service.search.SearchEngine.SORT_PRICE;
import static oliweb.nc.oliweb.service.search.SearchEngine.SORT_TITLE;
import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.MainActivity.RC_SIGN_IN;
import static oliweb.nc.oliweb.utility.Constants.REMOTE_DELAY;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class SearchActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = SearchActivity.class.getName();

    private static final String LOADING_DIALOG = "LOADING_DIALOG";
    private static final String SAVED_LIST_ANNONCE = "SAVED_LIST_ANNONCE";
    private static final String SAVE_FRAGMENT_LOADING = "SAVE_FRAGMENT_LOADING";
    private static final String SAVED_TRI = "SAVED_TRI";
    private static final String SAVED_DIRECTION = "SAVED_DIRECTION";

    public static final String CATEGORIE = "CATEGORIE";
    public static final String LOWER_PRICE = "LOWER_PRICE";
    public static final String HIGHER_PRICE = "HIGHER_PRICE";
    public static final String WITH_PHOTO_ONLY = "WITH_PHOTO_ONLY";
    public static final String KEYWORD = "KEYWORD";
    public static final String ACTION_ADVANCED_SEARCH = "ACTION_ADVANCED_SEARCH";
    public static final String SAVE_LIST_CATEGORIE = "SAVE_LIST_CATEGORIE";
    public static final String SAVE_WITH_PHOTO = "SAVE_WITH_PHOTO";
    public static final String SAVE_PRICE_HIGH = "SAVE_PRICE_HIGH";
    public static final String SAVE_PRICE_LOW = "SAVE_PRICE_LOW";
    public static final String SAVE_QUERY = "SAVE_QUERY";
    public static final String SAVE_FROM = "SAVE_FROM";
    public static final String SAVE_TOTAL_LOADED = "SAVE_TOTAL_LOADED";
    public static final String SAVE_IS_SEARCHING = "SAVE_IS_SEARCHING";


    @BindView(R.id.recycler_search_annonce)
    RecyclerView recyclerView;

    @BindView(R.id.empty_search_linear)
    LinearLayout emptyLinearView;

    @BindView(R.id.empty_search_text)
    TextView emptyTextView;

    @BindView(R.id.activity_search_image_timeout)
    ImageView imageTimeout;

    @BindView(R.id.activity_search_text_timeout)
    TextView textTimeout;

    @BindView(R.id.activity_search_swipe_refreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;

    SearchView searchView;

    private ArrayList<AnnonceFull> listAnnonce = new ArrayList<>();

    private EndlessRecyclerOnScrollListener endlessRecyclerOnScrollListener;
    private LoadingDialogFragment loadingDialogFragment;
    private SearchActivityViewModel viewModel;
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private List<String> listUidFavorites = new ArrayList<>();
    private int tri;
    private int direction;
    private String action;
    private int from = 0;
    private Long totalLoaded = 0L;
    private Disposable disposableActualSearch;

    private String query;
    private boolean isSearching;
    private int lowerPrice;
    private int higherPrice;
    private boolean withPhotoOnly;
    private ArrayList<String> listCategorieSelected;
    private Long delay;

    private View.OnClickListener onClickListener = (View v) -> {
        AnnonceBeautyAdapter.CommonViewHolder viewHolder = (AnnonceBeautyAdapter.CommonViewHolder) v.getTag();
        Intent intent = new Intent(this, AnnonceDetailActivity.class);
        intent.putExtra(ARG_ANNONCE, viewHolder.getAnnonceFull());
        if (viewHolder instanceof AnnonceBeautyAdapter.ViewHolderBeauty) {
            Pair pairImage = new Pair<View, String>(((AnnonceBeautyAdapter.ViewHolderBeauty)viewHolder).getImageView(), getString
            (R.string.image_detail_transition));
            ActivityOptionsCompat options = makeSceneTransitionAnimation(this, pairImage);
            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }

    };

    private View.OnClickListener onClickListenerShare = v -> {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

            // Display a loading spinner
            loadingDialogFragment = new LoadingDialogFragment();
            loadingDialogFragment.setText(getString(R.string.dynamic_link_creation));
            loadingDialogFragment.show(getSupportFragmentManager(), LOADING_DIALOG);

            String uidCurrentUser = FirebaseAuth.getInstance().getUid();
            AnnonceBeautyAdapter.CommonViewHolder viewHolder = (AnnonceBeautyAdapter.CommonViewHolder) v.getTag();
            AnnonceFull annonceFull = viewHolder.getAnnonceFull();

            // Partage du lien dynamique
            DynamicLinkService.shareDynamicLink(this, annonceFull, uidCurrentUser, loadingDialogFragment, recyclerView);
        } else {
            Snackbar.make(recyclerView, R.string.sign_in_required, Snackbar.LENGTH_LONG).show();
        }
    };

    private View.OnClickListener onClickListenerFavorite = v -> {
        v.setEnabled(false);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Snackbar.make(recyclerView, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.sign_in), v1 -> Utility.signIn(this, RC_SIGN_IN))
                    .show();
            v.setEnabled(true);
        } else {
            AnnonceBeautyAdapter.CommonViewHolder viewHolder = (AnnonceBeautyAdapter.CommonViewHolder) v.getTag();
            viewModel.addOrRemoveFromFavorite(FirebaseAuth.getInstance().getUid(), viewHolder.getAnnonceFull())
                    .observeOnce(addRemoveFromFavorite -> {
                        if (addRemoveFromFavorite != null) {
                            switch (addRemoveFromFavorite) {
                                case ONE_OF_YOURS:
                                    Toast.makeText(this, R.string.action_impossible_own_this_annonce, Toast.LENGTH_LONG).show();
                                    break;
                                case ADD_SUCCESSFUL:
                                    Snackbar.make(recyclerView, R.string.annonce_correctly_recorded_as_favorite, Snackbar.LENGTH_LONG).show();
                                    break;
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ButterKnife.bind(this);

        viewModel = ViewModelProviders.of(this).get(SearchActivityViewModel.class);

        Utility.hideKeyboard(this);

        // Get the intent, verify the action and get the query string
        Intent intentParam = getIntent();
        action = intentParam.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {
            query = intentParam.getStringExtra(SearchManager.QUERY);
            setTitle(String.format("%s %s", getString(R.string.looking_for), query));
        } else if (ACTION_ADVANCED_SEARCH.equals(action)) {
            setTitle(getString(R.string.activity_name_advanced_search));
            if (intentParam.hasExtra(KEYWORD)) {
                query = intentParam.getStringExtra(KEYWORD);
            }
            if (intentParam.hasExtra(CATEGORIE)) {
                listCategorieSelected = intentParam.getStringArrayListExtra(CATEGORIE);
            }
            if (intentParam.hasExtra(LOWER_PRICE)) {
                lowerPrice = intentParam.getIntExtra(LOWER_PRICE, 0);
            }
            if (intentParam.hasExtra(HIGHER_PRICE)) {
                higherPrice = intentParam.getIntExtra(HIGHER_PRICE, 0);
            }
            if (intentParam.hasExtra(WITH_PHOTO_ONLY)) {
                withPhotoOnly = intentParam.getBooleanExtra(WITH_PHOTO_ONLY, false);
            }
        }

        // Récupération du délai configuré avant d'envoyer un timeoutexception
        this.delay = FirebaseRemoteConfig.getInstance().getLong(REMOTE_DELAY);

        initRecyclerView();

        initViewModelObservers();

        // Récupération des données sauvegardées
        if (savedInstanceState != null) {
            retrieveDataFromBundle(savedInstanceState);
            if (isSearching) {
                makeNewSearch();
            }
        } else {
            makeNewSearch();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        swipeRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    protected void onPause() {
        GlideApp.get(this).clearMemory();
        swipeRefreshLayout.setOnRefreshListener(null);
        clearDisposableActualSearch();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search, menu);

        // On attache la searchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        // On repose les termes de la requête dans le searchView
        searchView.setQuery(query, false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        int newTri;
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.sort_date:
                newTri = SORT_DATE;
                break;
            case R.id.sort_title:
                newTri = SORT_TITLE;
                break;
            case R.id.sort_price:
                newTri = SORT_PRICE;
                break;
            default:
                newTri = SORT_DATE;
                break;
        }
        if (tri == newTri) {
            if (direction == ASC) {
                direction = DESC;
            } else {
                direction = ASC;
            }
        } else {
            tri = newTri;
            direction = ASC;
        }
        makeNewSearch();
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVED_LIST_ANNONCE, listAnnonce);
        outState.putInt(SAVED_TRI, tri);
        outState.putInt(SAVED_DIRECTION, direction);
        outState.putStringArrayList(SAVE_LIST_CATEGORIE, listCategorieSelected);
        outState.putBoolean(SAVE_WITH_PHOTO, withPhotoOnly);
        outState.putInt(SAVE_PRICE_HIGH, higherPrice);
        outState.putInt(SAVE_PRICE_LOW, lowerPrice);
        outState.putString(SAVE_QUERY, query);
        outState.putInt(SAVE_FROM, from);
        outState.putLong(SAVE_TOTAL_LOADED, totalLoaded);
        outState.putBoolean(SAVE_IS_SEARCHING, isSearching);
        if (loadingDialogFragment != null) {
            getSupportFragmentManager().putFragment(outState, SAVE_FRAGMENT_LOADING, loadingDialogFragment);
        }
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        makeNewSearch();
    }

    private void retrieveDataFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState.containsKey(SAVED_LIST_ANNONCE)) {
            listAnnonce = savedInstanceState.getParcelableArrayList(SAVED_LIST_ANNONCE);
            if (listAnnonce != null) {
                initAdapter(listAnnonce);
            }
        }
        if (savedInstanceState.containsKey(SAVED_DIRECTION)) {
            direction = savedInstanceState.getInt(SAVED_DIRECTION);
        }
        if (savedInstanceState.containsKey(SAVED_TRI)) {
            tri = savedInstanceState.getInt(SAVED_TRI);
        }
        if (savedInstanceState.containsKey(SAVE_LIST_CATEGORIE)) {
            listCategorieSelected = savedInstanceState.getStringArrayList(SAVE_LIST_CATEGORIE);
        }
        if (savedInstanceState.containsKey(SAVE_WITH_PHOTO)) {
            withPhotoOnly = savedInstanceState.getBoolean(SAVE_WITH_PHOTO);
        }
        if (savedInstanceState.containsKey(SAVE_PRICE_HIGH)) {
            higherPrice = savedInstanceState.getInt(SAVE_PRICE_HIGH);
        }
        if (savedInstanceState.containsKey(SAVE_PRICE_LOW)) {
            lowerPrice = savedInstanceState.getInt(SAVE_PRICE_LOW);
        }
        if (savedInstanceState.containsKey(SAVE_QUERY)) {
            query = savedInstanceState.getString(SAVE_QUERY);
        }
        if (savedInstanceState.containsKey(SAVE_FROM)) {
            from = savedInstanceState.getInt(SAVE_FROM);
        }
        if (savedInstanceState.containsKey(SAVE_TOTAL_LOADED)) {
            totalLoaded = savedInstanceState.getLong(SAVE_TOTAL_LOADED);
        }
        if (savedInstanceState.containsKey(SAVE_FRAGMENT_LOADING)) {
            loadingDialogFragment = (LoadingDialogFragment) getSupportFragmentManager().getFragment(savedInstanceState, SAVE_FRAGMENT_LOADING);
            loadingDialogFragment.show(getSupportFragmentManager(), LOADING_DIALOG);
        }
        if (savedInstanceState.containsKey(SAVE_IS_SEARCHING)) {
            isSearching = savedInstanceState.getBoolean(SAVE_IS_SEARCHING);
        }
    }

    private void initRecyclerView() {
        annonceBeautyAdapter = new AnnonceBeautyAdapter(onClickListener, onClickListenerShare, onClickListenerFavorite, this);

        GridLayoutManager layoutManager = Utility.initGridLayout(this, recyclerView);
        endlessRecyclerOnScrollListener = new EndlessRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onLoadMore() {
                if (listAnnonce.size() < totalLoaded && !viewModel.isLoading()) {
                    from = listAnnonce.size() + 1;
                    loadMoreData();
                }
            }
        };
        recyclerView.addOnScrollListener(endlessRecyclerOnScrollListener);

        recyclerView.setAdapter(annonceBeautyAdapter);
    }

    private void makeNewSearch() {
        if (showToastIfNoConnectivity()) return;

        viewModel.updateLoadingStatus(true);
        isSearching = true;
        clearDisposableActualSearch();
        from = 0;
        totalLoaded = 0L;
        listAnnonce.clear();
        loadMoreData();
    }

    private boolean showToastIfNoConnectivity() {
        if (!NetworkReceiver.checkConnection(this)) {
            viewModel.updateLoadingStatus(false);
            Toast.makeText(this, "Aucune recherche possible sans connexion", Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private void clearDisposableActualSearch() {
        if (disposableActualSearch != null && !disposableActualSearch.isDisposed())
            disposableActualSearch.dispose();
    }

    private void loadMoreData() {
        if (!viewModel.isConnected()) {
            Toast.makeText(this, R.string.connection_required_to_search, Toast.LENGTH_LONG).show();
        } else {
            Maybe<ElasticsearchHitsResult> maybe = null;
            if (Intent.ACTION_SEARCH.equals(action)) {
                maybe = viewModel.getSearchEngine().searchMaybe(null, false, 0, 0, query, Constants.PER_PAGE_REQUEST, from, tri, direction);
            }
            if (ACTION_ADVANCED_SEARCH.equals(action)) {
                maybe = viewModel.getSearchEngine().searchMaybe(listCategorieSelected, withPhotoOnly, lowerPrice, higherPrice, query, Constants.PER_PAGE_REQUEST, from, tri, direction);
            }
            if (maybe != null) {
                disposableActualSearch = maybe.subscribeOn(Schedulers.io())
                        .timeout(delay, TimeUnit.SECONDS, AndroidSchedulers.mainThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorComplete(throwable -> throwable instanceof TimeoutException)
                        .doOnError(this::doOnErrorSearch)
                        .doOnSuccess(this::doOnSuccessSearch)
                        .doOnComplete(this::doOnCompleteSearch)
                        .doAfterTerminate(this::doAfterTerminate)
                        .subscribe();
            }
        }
    }

    private void doOnErrorSearch(Throwable throwable) {
        Crashlytics.logException(throwable);
    }

    private void doOnSuccessSearch(ElasticsearchHitsResult elasticsearchHitsResult) {
        if (elasticsearchHitsResult != null && elasticsearchHitsResult.getHits() != null && !elasticsearchHitsResult.getHits().isEmpty()) {
            totalLoaded = elasticsearchHitsResult.getTotal();
            ArrayList<AnnonceFull> listResultSearch = new ArrayList<>();
            for (ElasticsearchResult<AnnonceFirebase> elasticsearchResult : elasticsearchHitsResult.getHits()) {
                AnnonceFull annonceFull = AnnonceConverter.convertDtoToAnnonceFull(elasticsearchResult.get_source());
                listResultSearch.add(annonceFull);
            }
            listAnnonce.addAll(listResultSearch);
            initAdapter(listAnnonce);
        } else if (listAnnonce != null && listAnnonce.isEmpty()) {
            displayEmptyList(true);
        }
    }

    private void doOnCompleteSearch() {
        annonceBeautyAdapter.setListAnnonces(listAnnonce);
        annonceBeautyAdapter.notifyDataSetChanged();
        displayTimeoutViews(true);
    }

    private void doAfterTerminate() {
        viewModel.updateLoadingStatus(false);
        isSearching = false;
    }

    private void displayTimeoutViews(boolean timeoutActive) {
        imageTimeout.setVisibility(timeoutActive ? View.VISIBLE : View.INVISIBLE);
        textTimeout.setVisibility(timeoutActive ? View.VISIBLE : View.INVISIBLE);
        recyclerView.setVisibility(timeoutActive ? View.INVISIBLE : View.VISIBLE);
    }

    private void displayEmptyList(boolean shouldDisplay) {
        emptyLinearView.setVisibility(shouldDisplay ? View.VISIBLE : View.INVISIBLE);
        emptyTextView.setVisibility(shouldDisplay ? View.VISIBLE : View.INVISIBLE);
    }

    private void initViewModelObservers() {
        viewModel.getLoading().observe(this, atomicBoolean -> {
                    if (atomicBoolean == null) return;

                    // Fait apparaitre un spinner pendant le chargement
                    swipeRefreshLayout.setRefreshing(atomicBoolean.get());

                    // Pendant les phases de chargement, on affiche pas les messages "Liste vide" ou "Délai expiré"
                    if (atomicBoolean.get()) {
                        displayEmptyList(false);
                        displayTimeoutViews(false);
                    }
                }
        );

        // On écoute les changements sur la liste des annonces retournées par la recherche
        viewModel.getLiveListAnnonce().observe(this, this::initAdapter);

        // Récupération de la liste des UID des annonces favorites de l'utilisateur en cours.
        String uidUser = FirebaseAuth.getInstance().getUid();
        if (StringUtils.isNotBlank(uidUser)) {
            viewModel.getFavoritesByUidUser(uidUser).observe(this, annonceFullsFavorites -> {
                listUidFavorites.clear();
                if (annonceFullsFavorites != null) {
                    for (AnnonceFull annonceFull : annonceFullsFavorites) {
                        listUidFavorites.add(annonceFull.getAnnonce().getUid());
                    }
                    initAdapter(listAnnonce);
                }
            });
        }
    }

    private void initAdapter(ArrayList<AnnonceFull> annonceWithPhotos) {
        listAnnonce = annonceWithPhotos;
        updateListWithFavorite(listAnnonce, listUidFavorites);
        if (!listAnnonce.isEmpty()) {
            emptyLinearView.setVisibility(View.GONE);
            annonceBeautyAdapter.setListAnnonces(this.listAnnonce);
            annonceBeautyAdapter.notifyDataSetChanged();
        } else {
            emptyLinearView.setVisibility(View.VISIBLE);
        }
    }

    private void updateListWithFavorite
            (ArrayList<AnnonceFull> listAnnonces, List<String> listUidFavorites) {
        for (AnnonceFull annonceFull : listAnnonces) {
            if (listUidFavorites != null && listUidFavorites.contains(annonceFull.getAnnonce().getUid())) {
                annonceFull.getAnnonce().setFavorite(1);
            } else {
                annonceFull.getAnnonce().setFavorite(0);
            }
        }
    }
}
