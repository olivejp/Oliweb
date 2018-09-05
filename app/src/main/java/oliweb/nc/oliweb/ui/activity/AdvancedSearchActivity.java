package oliweb.nc.oliweb.ui.activity;


import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.AdvancedSearchActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.SpinnerAdapter;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AdvancedSearchActivity extends AppCompatActivity {

    public static final String TAG = AdvancedSearchActivity.class.getCanonicalName();

    @BindView(R.id.categorie_spinner)
    AppCompatSpinner spinnerCategorie;

    @BindView(R.id.photo_switch)
    CheckBox withPhoto;

    @BindView(R.id.higher_price)
    EditText higherPrice;

    @BindView(R.id.lower_price)
    EditText lowerPrice;

    @BindView(R.id.keyword)
    EditText keyword;

    private AdvancedSearchActivityViewModel viewModel;

    // Evenement sur le spinner
    private AdapterView.OnItemSelectedListener spinnerItemSelected = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            viewModel.setCurrentCategorie((CategorieEntity) parent.getItemAtPosition(position));
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing
        }
    };

    public AdvancedSearchActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(AdvancedSearchActivityViewModel.class);
        setContentView(R.layout.activity_advanced_search);
        ButterKnife.bind(this);
        // Alimentation du spinner avec la liste des catégories
        viewModel.getListCategorie().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(this::defineSpinnerCategorie)
                .subscribe();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @OnClick(R.id.fab_advanced_search)
    public void onClickSearch(View v) {
        // TODO finir la recuperation des donnees dans la ui et lancer la recherche
        // viewModel.makeAnAdvancedSearch();
    }

    private void defineSpinnerCategorie(List<CategorieEntity> categorieEntities) {
        SpinnerAdapter adapter = new SpinnerAdapter(this, categorieEntities);
        spinnerCategorie.setAdapter(adapter);
        spinnerCategorie.setOnItemSelectedListener(spinnerItemSelected);
    }
}
