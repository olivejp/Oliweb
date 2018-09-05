package oliweb.nc.oliweb.ui.activity;


import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
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
import butterknife.OnTextChanged;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.AdvancedSearchActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.SpinnerAdapter;

import static oliweb.nc.oliweb.ui.activity.SearchActivity.ACTION_ADVANCED_SEARCH;
import static oliweb.nc.oliweb.ui.activity.SearchActivity.CATEGORIE;
import static oliweb.nc.oliweb.ui.activity.SearchActivity.HIGHER_PRICE;
import static oliweb.nc.oliweb.ui.activity.SearchActivity.KEYWORD;
import static oliweb.nc.oliweb.ui.activity.SearchActivity.LOWER_PRICE;
import static oliweb.nc.oliweb.ui.activity.SearchActivity.WITH_PHOTO_ONLY;

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

    private CategorieEntity currentCategorie;

    public AdvancedSearchActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AdvancedSearchActivityViewModel viewModel = ViewModelProviders.of(this).get(AdvancedSearchActivityViewModel.class);
        setContentView(R.layout.activity_advanced_search);
        ButterKnife.bind(this);

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

    @OnTextChanged(R.id.lower_price)
    public void onTextChangeLower(View v) {
        if (Integer.valueOf(lowerPrice.getText().toString()) > Integer.valueOf((higherPrice.getText().toString()))) {
            lowerPrice.setError("Le minimum doit être inférieur au maximum");
        }
    }

    @OnTextChanged(R.id.higher_price)
    public void onTextChangeHigher(View v) {
        if (Integer.valueOf(lowerPrice.getText().toString()) < Integer.valueOf((higherPrice.getText().toString()))) {
            higherPrice.setError("Le maximum doit être supérieur au minimum");
        }
    }

    @OnClick(R.id.fab_advanced_search)
    public void onClickSearch(View v) {
        Integer priceLow = Integer.valueOf(lowerPrice.getText().toString());
        Integer priceHigh = Integer.valueOf(higherPrice.getText().toString());
        boolean isPhoto = withPhoto.isChecked();
        String keywordSearched = keyword.getText().toString();

        Intent intent = new Intent();
        intent.putExtra(CATEGORIE, currentCategorie);
        intent.putExtra(LOWER_PRICE, priceLow);
        intent.putExtra(HIGHER_PRICE, priceHigh);
        intent.putExtra(WITH_PHOTO_ONLY, isPhoto);
        intent.putExtra(KEYWORD, keywordSearched);

        intent.setAction(ACTION_ADVANCED_SEARCH);

        intent.setClass(this, SearchActivity.class);

        startActivity(intent);
    }

    private void defineSpinnerCategorie(List<CategorieEntity> categorieEntities) {
        SpinnerAdapter adapter = new SpinnerAdapter(this, categorieEntities);
        spinnerCategorie.setAdapter(adapter);
        spinnerCategorie.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentCategorie = (CategorieEntity) parent.getItemAtPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
}
