package oliweb.nc.oliweb.ui.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.SharedPreferencesHelper;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.network.retrofit.RetrofitElasticClient;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapterRaw;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapterSingle;

public class SearchActivity extends AppCompatActivity {

    @BindView(R.id.recycler_search_annonce)
    RecyclerView recyclerView;

    private AnnonceAdapterRaw annonceAdapterRaw;
    private AnnonceAdapterSingle annonceAdapterSingle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        ButterKnife.bind(this);

        // Ouvre l'activité PostAnnonceActivity en mode Modification
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
        if (displayBeautyMode) {
            // En mode Raw
            RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            annonceAdapterRaw = new AnnonceAdapterRaw(onClickListener);
            recyclerView.setAdapter(annonceAdapterRaw);
            recyclerView.addItemDecoration(itemDecoration);
        } else {
            // En mode Beauty
            annonceAdapterSingle = new AnnonceAdapterSingle(onClickListener);
            recyclerView.setAdapter(annonceAdapterSingle);
        }

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);

            // Call Elasticsearch to get the annonce DTO list
            RetrofitElasticClient.searchText(query)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(annoncesWithPhotos -> {
                if (displayBeautyMode) {
                    annonceAdapterRaw.setListAnnonces(annoncesWithPhotos);
                } else {
                    annonceAdapterSingle.setListAnnonces(annoncesWithPhotos);
                }
            });
        }
    }
}
