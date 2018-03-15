package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.helper.SharedPreferencesHelper;
import oliweb.nc.oliweb.ui.BottomNavigationViewBehavior;
import oliweb.nc.oliweb.ui.EndlessRecyclerOnScrollListener;
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.ui.fragment.AnnonceDetailFragment;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ASC;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.DESC;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_DATE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_PRICE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_TITLE;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class SearchActivity extends AppCompatActivity {
    private static final String TAG = SearchActivity.class.getName();
    private static final String LOADING_DIALOG = "LOADING_DIALOG";

    @BindView(R.id.recycler_search_annonce)
    RecyclerView recyclerView;

    @BindView(R.id.empty_search_linear)
    LinearLayout linearLayout;

    @BindView(R.id.search_view_activity_search)
    SearchView searchView;

    @BindView(R.id.toolbar_activity_search)
    Toolbar toolbar;

    @BindView(R.id.bottom_navigation_sort)
    BottomNavigationView bottomNavigationView;

    private String query;
    private boolean displayBeautyMode;
    private LoadingDialogFragment loadingDialogFragment;
    private SearchActivityViewModel searchActivityViewModel;
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private int tri;
    private int direction;
    private int currentPage = 0;
    private int pagingSize = 20;
    private EndlessRecyclerOnScrollListener endlessRecyclerOnScrollListener;

    // Ouvre l'activité PostAnnonceActivity en mode Visualisation
    private View.OnClickListener onClickListener = v -> {
        AnnoncePhotos annoncePhotos = (AnnoncePhotos) v.getTag();
        if (getFragmentManager() != null) {
            AnnonceDetailFragment annonceDetailFragment = AnnonceDetailFragment.getInstance(annoncePhotos);
            getSupportFragmentManager().beginTransaction().replace(R.id.search_frame, annonceDetailFragment).addToBackStack(null).commit();
        }
    };

    private View.OnClickListener onFavoriteClickListener = v -> {
        Log.d(TAG, "Click on add to favorite");
        if (v.getTag() != null) {
            AnnoncePhotos annonce = (AnnoncePhotos) v.getTag();
            searchActivityViewModel.isAnnonceFavorite(annonce.getAnnonceEntity().getUUID())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(integer -> {
                        if (integer == null || integer == 0) {
                            searchActivityViewModel.addToFavorite(annonce);
                        }
                    });
        }
    };

    private View.OnClickListener onShareClickListener = v -> {
        if (v.getTag() != null) {
            // TODO pas génial ce partage faudrait peut être revoir cette fonctionnalité
            AnnoncePhotos annonce = (AnnoncePhotos) v.getTag();
            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = annonce.getAnnonceEntity().getDescription();
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, annonce.getAnnonceEntity().getTitre());
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Partager via"));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ButterKnife.bind(this);

        searchActivityViewModel = ViewModelProviders.of(this).get(SearchActivityViewModel.class);

        Utility.hideKeyboard(this);

        // Get the intent, verify the action and get the query string
        Intent intentParam = getIntent();
        if (Intent.ACTION_SEARCH.equals(intentParam.getAction())) {
            query = intentParam.getStringExtra(SearchManager.QUERY);
        }

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // On attache la searchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        }

        setTitle("Recherche " + query);

        // On repose les termes de la requête dans le searchView
        searchView.setQuery(query, false);

        displayBeautyMode = SharedPreferencesHelper.getInstance(getApplicationContext()).getDisplayBeautyMode();
        boolean gridMode = SharedPreferencesHelper.getInstance(getApplicationContext()).getGridMode();

        RecyclerView.LayoutManager layoutManager;
        if (gridMode) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
            layoutManager = gridLayoutManager;
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    switch (annonceBeautyAdapter.getItemViewType(position)) {
                        case 1:
                            return 1;
                        case 2:
                            return 2;
                        default:
                            return 1;
                    }
                }
            });
            endlessRecyclerOnScrollListener = new EndlessRecyclerOnScrollListener(gridLayoutManager) {
                @Override
                public void onLoadMore() {
                    currentPage++;
                    launchNewSearch(currentPage);
                }
            };
        } else {
            layoutManager = new LinearLayoutManager(this);
            ((LinearLayoutManager) layoutManager).setOrientation(LinearLayoutManager.VERTICAL);
            endlessRecyclerOnScrollListener = new EndlessRecyclerOnScrollListener(layoutManager) {
                @Override
                public void onLoadMore() {
                    currentPage++;
                    launchNewSearch(currentPage);
                }
            };
        }
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.addOnScrollListener(endlessRecyclerOnScrollListener);

        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) bottomNavigationView.getLayoutParams();
        layoutParams.setBehavior(new BottomNavigationViewBehavior());

        // Recherche du mode display actuellement dans les préférences.
        if (displayBeautyMode) {
            annonceBeautyAdapter = new AnnonceBeautyAdapter(onClickListener, onFavoriteClickListener, onShareClickListener);
            recyclerView.setAdapter(annonceBeautyAdapter);
        }

        initViewModelObservers();

        currentPage = 0;
        launchNewSearch(currentPage);
    }

    private void launchNewSearch(int currentPage) {
        if (searchActivityViewModel.isConnected()) {
            int from = currentPage * pagingSize;
            searchActivityViewModel.makeASearch(query, pagingSize, from, tri, direction);
        } else {
            Toast.makeText(this, "Can't search without internet connection", Toast.LENGTH_LONG).show();
        }
    }

    private BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener = item -> {
        int newTri;
        switch (item.getItemId()) {
            case R.id.action_sort_date:
                newTri = SORT_DATE;
                break;
            case R.id.action_sort_title:
                newTri = SORT_TITLE;
                break;
            case R.id.action_sort_price:
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

        this.currentPage = 0;
        launchNewSearch(currentPage);

        return true;
    };

    private void initViewModelObservers() {
        // Fait apparaitre un spinner pendant l'attente
        searchActivityViewModel.getLoading().observe(this, atomicBoolean -> {
                    if (atomicBoolean != null) {
                        if (atomicBoolean.get()) {
                            loadingDialogFragment = new LoadingDialogFragment();
                            loadingDialogFragment.show(this.getSupportFragmentManager(), LOADING_DIALOG);
                            linearLayout.setVisibility(View.GONE);
                        } else {
                            if (loadingDialogFragment != null) {
                                loadingDialogFragment.dismiss();
                            }
                        }
                    }
                }
        );

        // On écoute les changements sur la liste des annonces retournées par la recherche
        searchActivityViewModel.getLiveListAnnonce().observe(this, annonceWithPhotos -> {
            if (annonceWithPhotos != null && !annonceWithPhotos.isEmpty()) {
                linearLayout.setVisibility(View.GONE);
                annonceBeautyAdapter.setListAnnonces(annonceWithPhotos);
                annonceBeautyAdapter.notifyDataSetChanged();
            } else {
                linearLayout.setVisibility(View.VISIBLE);
            }
        });
    }
}
