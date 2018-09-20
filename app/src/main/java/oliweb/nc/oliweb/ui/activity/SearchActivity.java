package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.service.sharing.DynamicLynksGenerator;
import oliweb.nc.oliweb.ui.EndlessRecyclerOnScrollListener;
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;

import static android.support.v4.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.MainActivity.RC_SIGN_IN;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.ASC;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.DESC;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_DATE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_PRICE;
import static oliweb.nc.oliweb.ui.fragment.ListAnnonceFragment.SORT_TITLE;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class SearchActivity extends AppCompatActivity {

    private static final String TAG = SearchActivity.class.getName();

    private static final String LOADING_DIALOG = "LOADING_DIALOG";
    private static final String SAVED_CURRENT_PAGE = "SAVED_CURRENT_PAGE";
    private static final String SAVED_LIST_ANNONCE = "SAVED_LIST_ANNONCE";
    private static final String SAVED_TRI = "SAVED_TRI";
    private static final String SAVED_DIRECTION = "SAVED_DIRECTION";

    public static final String CATEGORIE = "CATEGORIE";
    public static final String LOWER_PRICE = "LOWER_PRICE";
    public static final String HIGHER_PRICE = "HIGHER_PRICE";
    public static final String WITH_PHOTO_ONLY = "WITH_PHOTO_ONLY";
    public static final String KEYWORD = "KEYWORD";
    public static final String ACTION_ADVANCED_SEARCH = "ACTION_ADVANCED_SEARCH";


    @BindView(R.id.recycler_search_annonce)
    RecyclerView recyclerView;

    @BindView(R.id.empty_search_linear)
    LinearLayout linearLayout;

    @BindView(R.id.toolbar_activity_search)
    Toolbar toolbar;

    SearchView searchView;

    private String query;
    private ArrayList<AnnonceFull> listAnnonce;
    private LoadingDialogFragment loadingDialogFragment;
    private SearchActivityViewModel viewModel;
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private List<String> listUidFavorites = new ArrayList<>();
    private int tri;
    private int direction;
    private int currentPage = 0;
    private String action;

    private CategorieEntity categorieEntity;
    private int lowerPrice;
    private int higherPrice;
    private boolean withPhotoOnly;

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
            setTitle("Recherche avancée");
            if (intentParam.hasExtra(KEYWORD)) {
                query = intentParam.getStringExtra(KEYWORD);
            }
            if (intentParam.hasExtra(CATEGORIE)) {
                categorieEntity = intentParam.getParcelableExtra(CATEGORIE);
            }
            if (intentParam.hasExtra(LOWER_PRICE)) {
                lowerPrice = intentParam.getIntExtra(LOWER_PRICE,0);
            }
            if (intentParam.hasExtra(HIGHER_PRICE)) {
                higherPrice = intentParam.getIntExtra(HIGHER_PRICE,0);
            }
            if (intentParam.hasExtra(WITH_PHOTO_ONLY)) {
                withPhotoOnly = intentParam.getBooleanExtra(WITH_PHOTO_ONLY, false);
            }
        }

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        annonceBeautyAdapter = new AnnonceBeautyAdapter(getResources().getColor(R.color.colorPrimary),
                onClickListener,
                onClickListenerShare,
                onClickListenerFavorite);

        RecyclerView.LayoutManager layoutManager = Utility.initGridLayout(this, recyclerView, annonceBeautyAdapter);
        EndlessRecyclerOnScrollListener endlessRecyclerOnScrollListener = new EndlessRecyclerOnScrollListener(layoutManager) {
            @Override
            public void onLoadMore() {
                currentPage++;
                launchNewSearch(currentPage);
            }
        };
        recyclerView.addOnScrollListener(endlessRecyclerOnScrollListener);

        recyclerView.setAdapter(annonceBeautyAdapter);

        initViewModelObservers();

        // Récupération des données sauvegardées
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SAVED_CURRENT_PAGE)) {
                currentPage = savedInstanceState.getInt(SAVED_CURRENT_PAGE);
            }
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
        } else {
            currentPage = 0;
            launchNewSearch(currentPage);
        }
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_CURRENT_PAGE, currentPage);
        outState.putParcelableArrayList(SAVED_LIST_ANNONCE, listAnnonce);
        outState.putInt(SAVED_TRI, tri);
        outState.putInt(SAVED_DIRECTION, direction);
    }

    private void launchNewSearch(int currentPage) {
        if (!viewModel.isConnected()) {
            Toast.makeText(this, R.string.connection_required_to_search, Toast.LENGTH_LONG).show();
        } else {
            int from = currentPage * Constants.PER_PAGE_REQUEST;
            if (Intent.ACTION_SEARCH.equals(action)) {
                viewModel.makeASearch(query, Constants.PER_PAGE_REQUEST, from, tri, direction);
            } else if (ACTION_ADVANCED_SEARCH.equals(action)) {
                viewModel.makeAnAdvancedSearch(categorieEntity.getName(), withPhotoOnly, lowerPrice, higherPrice, query, Constants.PER_PAGE_REQUEST, from, tri, direction);
            }
        }
    }

    private void initViewModelObservers() {
        // Fait apparaitre un spinner pendant le chargement des annonces
        viewModel.getLoading().observe(this, atomicBoolean -> {
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
        if (!annonceWithPhotos.isEmpty()) {
            linearLayout.setVisibility(View.GONE);
            annonceBeautyAdapter.setListAnnonces(this.listAnnonce);
            annonceBeautyAdapter.notifyDataSetChanged();
        } else {
            linearLayout.setVisibility(View.VISIBLE);
        }
    }

    private View.OnClickListener onClickListener = (View v) -> {
        AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
        Intent intent = new Intent(this, AnnonceDetailActivity.class);
        intent.putExtra(ARG_ANNONCE, viewHolder.getAnnonceFull());
        Pair pairImage = new Pair<View, String>(viewHolder.getImageView(), getString(R.string.image_detail_transition));
        ActivityOptionsCompat options = makeSceneTransitionAnimation(this, pairImage);
        startActivity(intent, options.toBundle());
    };

    private View.OnClickListener onClickListenerShare = v -> {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uidCurrentUser = FirebaseAuth.getInstance().getUid();
            AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
            AnnonceFull annoncePhotos = viewHolder.getAnnonceFull();
            AnnonceEntity annonceEntity = annoncePhotos.getAnnonce();

            DynamicLynksGenerator.generateShortLink(uidCurrentUser, annonceEntity, annoncePhotos.getPhotos(), new DynamicLynksGenerator.DynamicLinkListener() {
                @Override
                public void getLink(Uri shortLink, Uri flowchartLink) {
                    Intent sendIntent = new Intent();
                    String msg = getString(R.string.default_text_share_link) + shortLink;
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                }

                @Override
                public void getLinkError() {
                    Snackbar.make(recyclerView, R.string.dynamic_link_failed, Snackbar.LENGTH_LONG).show();
                }
            });
        } else {
            Snackbar.make(recyclerView, R.string.sign_in_required, Snackbar.LENGTH_LONG).show();
        }
    };

    private View.OnClickListener onClickListenerFavorite = v -> {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Snackbar.make(recyclerView, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.sign_in), v1 -> Utility.signIn(this, RC_SIGN_IN))
                    .show();
        } else {
            AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
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
                    });
        }
    };

    private void updateListWithFavorite(ArrayList<AnnonceFull> listAnnonces, List<String> listUidFavorites) {
        for (AnnonceFull annonceFull : listAnnonces) {
            if (listUidFavorites != null && listUidFavorites.contains(annonceFull.getAnnonce().getUid())) {
                annonceFull.getAnnonce().setFavorite(1);
            } else {
                annonceFull.getAnnonce().setFavorite(0);
            }
        }
    }
}
