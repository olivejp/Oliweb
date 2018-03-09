package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
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
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.adapter.AnnonceRawAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.utility.Utility;

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

    private String query;
    private boolean displayBeautyMode;
    private LoadingDialogFragment loadingDialogFragment;
    private SearchActivityViewModel searchActivityViewModel;
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private AnnonceRawAdapter annonceRawAdapter;

    // Ouvre l'activité PostAnnonceActivity en mode Visualisation
    private View.OnClickListener onClickListener = v -> {
        // TODO appeler un nouveau fragment ici pour visualiser l'annonce
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

        // On repose les termes de la requête dans le searchView
        searchView.setQuery(query, false);

        displayBeautyMode = SharedPreferencesHelper.getInstance(getApplicationContext()).getDisplayBeautyMode();
        boolean gridMode = SharedPreferencesHelper.getInstance(getApplicationContext()).getGridMode();

        RecyclerView.LayoutManager layoutManager;
        if (gridMode) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
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
            layoutManager = gridLayoutManager;
        } else {
            layoutManager = new LinearLayoutManager(this);
            ((LinearLayoutManager) layoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        }
        recyclerView.setLayoutManager(layoutManager);

        // Recherche du mode display actuellement dans les préférences.
        if (displayBeautyMode) {
            annonceBeautyAdapter = new AnnonceBeautyAdapter(onClickListener, onFavoriteClickListener, onShareClickListener);
            recyclerView.setAdapter(annonceBeautyAdapter);
        } else {
            annonceRawAdapter = new AnnonceRawAdapter(onClickListener, onFavoriteClickListener, onShareClickListener);
            recyclerView.setAdapter(annonceRawAdapter);
            RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            recyclerView.addItemDecoration(itemDecoration);
        }

        initViewModelObservers();

        if (!searchActivityViewModel.makeASearch(query)) {
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
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            recyclerView.setVisibility(View.VISIBLE);
                            if (loadingDialogFragment != null) {
                                loadingDialogFragment.dismiss();
                            }
                        }
                    }
                }
        );

        // On écoute les changements sur la liste des annonces retournées par la recherche
        searchActivityViewModel.getListAnnonce().observe(this, annonceWithPhotos -> {
            if (annonceWithPhotos != null && !annonceWithPhotos.isEmpty()) {
                linearLayout.setVisibility(View.GONE);
                if (displayBeautyMode) {
                    annonceBeautyAdapter.setListAnnonces(annonceWithPhotos);
                    annonceBeautyAdapter.notifyDataSetChanged();
                } else {
                    annonceRawAdapter.setListAnnonces(annonceWithPhotos);
                    annonceRawAdapter.notifyDataSetChanged();
                }
            } else {
                linearLayout.setVisibility(View.VISIBLE);
            }
        });
    }
}
