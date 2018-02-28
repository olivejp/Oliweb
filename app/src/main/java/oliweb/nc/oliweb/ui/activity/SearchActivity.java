package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.SharedPreferencesHelper;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.SearchActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapter;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class SearchActivity extends AppCompatActivity {

    @BindView(R.id.recycler_search_annonce)
    RecyclerView recyclerView;

    @BindView(R.id.empty_search_linear)
    LinearLayout linearLayout;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    private String query;

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
            AnnonceEntity annonce = (AnnonceEntity) v.getTag();
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            intent.setClass(this, PostAnnonceActivity.class);
            bundle.putString(PostAnnonceActivity.BUNDLE_KEY_MODE, Constants.PARAM_VIS);
            bundle.putLong(PostAnnonceActivity.BUNDLE_KEY_ID_ANNONCE, annonce.getIdAnnonce());
            intent.putExtras(bundle);
            startActivity(intent);
        };

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        // Recherche du mode display actuellement dans les préférences.
        boolean displayBeautyMode = SharedPreferencesHelper.getInstance(getApplicationContext()).getDisplayBeautyMode();
        AnnonceAdapter annonceAdapter = new AnnonceAdapter(displayBeautyMode ? AnnonceAdapter.DisplayType.BEAUTY : AnnonceAdapter.DisplayType.RAW, onClickListener);
        recyclerView.setAdapter(annonceAdapter);
        if (!displayBeautyMode) {
            // En mode Raw uniquement
            RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            recyclerView.addItemDecoration(itemDecoration);
        }

        // Fait apparaitre un spinner pendant l'attente
        // ToDo ne marche pas pour le moment
        searchActivityViewModel.getLoading().observe(this, atomicBoolean -> {
                    if (atomicBoolean != null){
                        if (atomicBoolean.get()){
                            progressBar.setVisibility(View.VISIBLE);
                            linearLayout.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
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
