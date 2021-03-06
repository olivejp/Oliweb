package oliweb.nc.oliweb.ui.activity;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProviders;
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
    public static final String BUNDLE_SAVED_CATEGORIE = "BUNDLE_SAVED_CATEGORIE";


    @BindView(R.id.chips_group)
    ChipGroup chipsGroup;

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
        setContentView(R.layout.activity_advanced_search);
        ButterKnife.bind(this);

        viewModel = ViewModelProviders.of(this).get(AdvancedSearchActivityViewModel.class);

        // Retrieve the checked categories
        if (savedInstanceState != null && savedInstanceState.containsKey(BUNDLE_SAVED_CATEGORIE)) {
            checkedCategorie = savedInstanceState.getBooleanArray(BUNDLE_SAVED_CATEGORIE);
        }

        // Get the complete list of categories
        viewModel.getListCategorieLibelle()
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(list -> {
                    listCategorieComplete = list.toArray(new String[0]);
                    choose(checkedCategorie);
                })
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
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

    @OnClick(R.id.with_photos_only)
    public void changePhotoOnly(View v) {
        withPhotoOnly.setChecked(!withPhotoOnly.isChecked());
    }

    @OnClick(R.id.button_add_category)
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
        higherPrice.setError(null);
        lowerPrice.setError(null);

        String higher = higherPrice.getText().toString();
        String lower = lowerPrice.getText().toString();

        Integer higherInt;
        Integer lowerInt;

        try {
            higherInt = Integer.valueOf(higher);
        } catch (NumberFormatException exception) {
            Log.e(TAG, exception.getLocalizedMessage(), exception);
            higherPrice.setError(getString(R.string.number_format_invalid));
            error = true;
            return;
        }

        try {
            lowerInt = Integer.valueOf(lower);
        } catch (NumberFormatException exception) {
            Log.e(TAG, exception.getLocalizedMessage(), exception);
            lowerPrice.setError(getString(R.string.number_format_invalid));
            error = true;
            return;
        }

        if (!higher.isEmpty() && !lower.isEmpty() && lowerInt > higherInt) {
            higherPrice.setError(getString(R.string.error_max_gte_min));
            error = true;
        }

        if (!higher.isEmpty() && lower.isEmpty()) {
            lowerPrice.setError(getString(R.string.error_price_interval_incorrect));
            error = true;
        }

        if (higher.isEmpty() && !lower.isEmpty()) {
            higherPrice.setError(getString(R.string.error_price_interval_incorrect));
            error = true;
        }
    }

    @OnClick(R.id.fab_advanced_search)
    public void onClickSearch(View v) {
        if (error) {
            Snackbar.make(constraintLayout, R.string.search_error, Snackbar.LENGTH_LONG).show();
        } else {
            // Compte le nombre de recherche avancée faite par les utilisateurs
            Trace myTrace = FirebasePerformance.getInstance().newTrace("nb_advanced_search");
            myTrace.start();
            myTrace.incrementMetric("nb_advanced_search", 1);
            myTrace.stop();

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
        if (checkedCat == null) return;

        listCategorieSelected.clear();
        checkedCategorie = checkedCat;
        chipsGroup.removeAllViews();
        for (int i = 0; i <= listCategorieComplete.length - 1; i++) {
            if (checkedCategorie[i]) {
                Chip chip = new Chip(this);
                chip.setText(listCategorieComplete[i]);
                chip.setTag(listCategorieComplete[i]);
                chip.setCloseIconVisible(true);
                int finalI = i;
                chip.setOnCloseIconClickListener(view -> {
                    for (String categorieLabel : listCategorieSelected) {
                        if (categorieLabel.equals(view.getTag()) && listCategorieSelected.remove(categorieLabel)) {
                            chipsGroup.removeView(view);
                            checkedCategorie[finalI] = false;
                            break;
                        }
                    }
                });
                chipsGroup.addView(chip);
                listCategorieSelected.add(listCategorieComplete[i]);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBooleanArray(BUNDLE_SAVED_CATEGORIE, checkedCategorie);
        super.onSaveInstanceState(outState);
    }
}
