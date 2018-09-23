package oliweb.nc.oliweb.ui.activity;


import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.activity.viewmodel.AdvancedSearchActivityViewModel;
import oliweb.nc.oliweb.ui.dialog.SelectCategoryDialog;

import static oliweb.nc.oliweb.ui.activity.SearchActivity.ACTION_ADVANCED_SEARCH;
import static oliweb.nc.oliweb.ui.activity.SearchActivity.CATEGORIE;
import static oliweb.nc.oliweb.ui.activity.SearchActivity.HIGHER_PRICE;
import static oliweb.nc.oliweb.ui.activity.SearchActivity.KEYWORD;
import static oliweb.nc.oliweb.ui.activity.SearchActivity.LOWER_PRICE;
import static oliweb.nc.oliweb.ui.activity.SearchActivity.WITH_PHOTO_ONLY;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AdvancedSearchActivity extends AppCompatActivity implements SelectCategoryDialog.SelectCategoryDialogListener {

    public static final String TAG = AdvancedSearchActivity.class.getCanonicalName();
    public static final String DIALOG_SELECT_CATEGORY = "DIALOG_SELECT_CATEGORY";

    @BindView(R.id.text_categorie)
    EditText textCategorie;

    @BindView(R.id.photo_switch)
    CheckBox withPhotoOnly;

    @BindView(R.id.higher_price)
    EditText higherPrice;

    @BindView(R.id.lower_price)
    EditText lowerPrice;

    @BindView(R.id.keyword)
    EditText keyword;

    @BindView(R.id.constraint_advanced_search)
    ConstraintLayout constraintLayout;

    private boolean[] checkedCategorie;
    private String[] listCategorieComplete;
    private ArrayList<String> listCategorieSelected = new ArrayList<>();
    private AdvancedSearchActivityViewModel viewModel;
    private boolean error;

    public AdvancedSearchActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this).get(AdvancedSearchActivityViewModel.class);
        setContentView(R.layout.activity_advanced_search);
        ButterKnife.bind(this);
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

    @OnClick(R.id.with_photos_only)
    public void changePhotoOnly(View v) {
        withPhotoOnly.setChecked(!withPhotoOnly.isChecked());
    }

    @OnClick(R.id.text_categorie)
    public void chooseCategory(View v) {
        viewModel.getListCategorieLibelle()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(list -> {
                    listCategorieComplete = list.toArray(new String[0]);
                    SelectCategoryDialog dialog = SelectCategoryDialog.createInstance(listCategorieComplete, checkedCategorie);
                    dialog.show(getSupportFragmentManager(), DIALOG_SELECT_CATEGORY);
                })
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }

    @OnTextChanged({R.id.higher_price, R.id.lower_price})
    public void onTextChangeHigher(CharSequence s, int start, int before, int count) {
        error = false;
        String higher = higherPrice.getText().toString();
        String lower = lowerPrice.getText().toString();
        if (!higher.isEmpty() && !lower.isEmpty() && Integer.valueOf(lower) > Integer.valueOf((higher))) {
            higherPrice.setError(getString(R.string.error_max_gte_min));
            error = true;
        }

        if (higher.isEmpty() && !lower.isEmpty() || !higher.isEmpty() && lower.isEmpty()) {
            higherPrice.setError(getString(R.string.error_price_interval_incorrect));
            error = true;
        }
    }

    @OnClick(R.id.fab_advanced_search)
    public void onClickSearch(View v) {
        if (error) {
            Snackbar.make(constraintLayout, R.string.search_error, Snackbar.LENGTH_LONG).show();
        } else {
            String higher = higherPrice.getText().toString();
            String lower = lowerPrice.getText().toString();
            String keywordSearch = keyword.getText().toString();

            Intent intent = new Intent();
            intent.putStringArrayListExtra(CATEGORIE, listCategorieSelected);
            intent.putExtra(WITH_PHOTO_ONLY, withPhotoOnly.isChecked());
            intent.putExtra(LOWER_PRICE, StringUtils.isBlank(lower) ? null : Integer.valueOf(lower));
            intent.putExtra(HIGHER_PRICE, StringUtils.isBlank(higher) ? null : Integer.valueOf(higher));
            intent.putExtra(KEYWORD, StringUtils.isBlank(keywordSearch) ? null : keywordSearch);

            intent.setAction(ACTION_ADVANCED_SEARCH);

            intent.setClass(this, SearchActivity.class);

            startActivity(intent);
        }
    }

    @Override
    public void choose(boolean[] checkedCat) {
        String libelleConcat = "";
        listCategorieSelected.clear();
        checkedCategorie = checkedCat;
        for (int i = 0; i <= listCategorieComplete.length - 1; i++) {
            if (checkedCategorie[i]) {
                libelleConcat = libelleConcat.concat(listCategorieComplete[i]).concat((i < checkedCategorie.length - 1) ? ", " : "");
                listCategorieSelected.add(listCategorieComplete[i]);
            }
        }
        textCategorie.setText(libelleConcat);
    }
}
