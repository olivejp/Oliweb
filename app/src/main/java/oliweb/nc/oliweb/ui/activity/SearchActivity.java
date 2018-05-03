package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.EndlessRecyclerOnScrollListener;
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
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

    SearchView searchView;

    @BindView(R.id.toolbar_activity_search)
    Toolbar toolbar;

    private String query;
    private LoadingDialogFragment loadingDialogFragment;
    private SearchActivityViewModel searchActivityViewModel;
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private int tri;
    private int direction;
    private int currentPage = 0;
    private EndlessRecyclerOnScrollListener endlessRecyclerOnScrollListener;

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

        setTitle("Recherche " + query);

        // Recherche du mode display actuellement dans les préférences.
        annonceBeautyAdapter = new AnnonceBeautyAdapter(getResources().getColor(R.color.colorPrimary),
                onClickListener,
                onClickListenerShare,
                onClickListenerFavorite);

        RecyclerView.LayoutManager layoutManager = Utility.initGridLayout(this, recyclerView, annonceBeautyAdapter);
        endlessRecyclerOnScrollListener = new EndlessRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onLoadMore() {
                currentPage++;
                launchNewSearch(currentPage);
            }
        };
        recyclerView.addOnScrollListener(endlessRecyclerOnScrollListener);

        recyclerView.setAdapter(annonceBeautyAdapter);


        initViewModelObservers();

        currentPage = 0;
        launchNewSearch(currentPage);
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

        this.currentPage = 0;
        launchNewSearch(currentPage);

        return true;
    }

    private void launchNewSearch(int currentPage) {
        if (searchActivityViewModel.isConnected()) {
            int from = currentPage * Constants.PER_PAGE_REQUEST;
            searchActivityViewModel.makeASearch(query, Constants.PER_PAGE_REQUEST, from, tri, direction);
        } else {
            Toast.makeText(this, "Can't search without internet connection", Toast.LENGTH_LONG).show();
        }
    }

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

    private View.OnClickListener onClickListener = v -> {
        AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
        Intent intent = new Intent(this, AnnonceDetailActivity.class);
        intent.putExtra(ARG_ANNONCE, viewHolder.getAnnoncePhotos());
        Pair<View, String> pairImage = new Pair<>(viewHolder.getImageView(), getString(R.string.image_detail_transition));
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, pairImage);
        startActivity(intent, options.toBundle());
    };

    private View.OnClickListener onClickListenerShare = v -> {
        AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        String shareBody = viewHolder.getAnnoncePhotos().getAnnonceEntity().getDescription();
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, viewHolder.getAnnoncePhotos().getAnnonceEntity().getTitre());
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Partager via"));
    };

    private View.OnClickListener onClickListenerFavorite = v -> {
        AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
        Log.d(TAG, "Click on add to favorite");
        searchActivityViewModel.isAnnonceFavorite(viewHolder.getAnnoncePhotos().getAnnonceEntity().getUuid())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(integer -> {
                    if (integer == null || integer == 0) {
                        searchActivityViewModel.addToFavorite(viewHolder.getAnnoncePhotos());
                    }
                });
    };
}
