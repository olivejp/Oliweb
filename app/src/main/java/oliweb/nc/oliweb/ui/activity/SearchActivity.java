package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.helper.SharedPreferencesHelper;
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class SearchActivity extends AppCompatActivity {

    private static final String LOADING_DIALOG = "LOADING_DIALOG";

    @BindView(R.id.recycler_search_annonce)
    RecyclerView recyclerView;

    @BindView(R.id.empty_search_linear)
    LinearLayout linearLayout;

    private String query;
    private LoadingDialogFragment loadingDialogFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ButterKnife.bind(this);

        SearchActivityViewModel searchActivityViewModel = ViewModelProviders.of(this).get(SearchActivityViewModel.class);

        // Get the intent, verify the action and get the query string
        Intent intentParam = getIntent();
        if (Intent.ACTION_SEARCH.equals(intentParam.getAction())) {
            query = intentParam.getStringExtra(SearchManager.QUERY);
        }

        setTitle("Recherche \"" + query + "\"");

        // Ouvre l'activité PostAnnonceActivity en mode Visualisation
        View.OnClickListener onClickListener = v -> {
            // TODO appeler un nouveau fragment ici pour visualiser l'annonce
            //            AnnonceEntity annonce = (AnnonceEntity) v.getTag();
            //            Intent intent = new Intent();
            //            Bundle bundle = new Bundle();
            //            intent.setClass(this, PostAnnonceActivity.class);
            //            bundle.putString(PostAnnonceActivity.BUNDLE_KEY_MODE, Constants.PARAM_VIS);
            //            bundle.putParcelable(PostAnnonceActivity.BUNDLE_KEY_ANNONCE, annonce);
            //            intent.putExtras(bundle);
            //            startActivity(intent);
        };

        // TODO tester le nouveau gridlayout
        RecyclerView.LayoutManager layoutManager;
        boolean displayBeautyMode = SharedPreferencesHelper.getInstance(getApplicationContext()).getDisplayBeautyMode();
        boolean gridMode = SharedPreferencesHelper.getInstance(getApplicationContext()).getGridMode();

        if (gridMode) {
            layoutManager = new LinearLayoutManager(this);
            ((LinearLayoutManager) layoutManager).setOrientation(LinearLayoutManager.VERTICAL);
        } else {
            layoutManager = new GridLayoutManager(this, 2);
        }
        recyclerView.setLayoutManager(layoutManager);

        // Recherche du mode display actuellement dans les préférences.
        AnnonceAdapter annonceAdapter = new AnnonceAdapter(displayBeautyMode ? AnnonceAdapter.DisplayType.BEAUTY : AnnonceAdapter.DisplayType.RAW, onClickListener);
        recyclerView.setAdapter(annonceAdapter);
        if (!displayBeautyMode) {
            // En mode Raw uniquement
            RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            recyclerView.addItemDecoration(itemDecoration);
        }

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
                annonceAdapter.setListAnnonces(annonceWithPhotos);
            } else {
                linearLayout.setVisibility(View.VISIBLE);
            }
        });

        if (!searchActivityViewModel.makeASearch(query)) {
            Toast.makeText(this, "Can't search without internet connection", Toast.LENGTH_LONG).show();
        }
    }
}
