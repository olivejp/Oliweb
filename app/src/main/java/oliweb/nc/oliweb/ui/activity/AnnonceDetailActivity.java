package oliweb.nc.oliweb.ui.activity;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import me.relex.circleindicator.CircleIndicator;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.network.CallLoginUi;
import oliweb.nc.oliweb.ui.adapter.AnnonceViewPagerAdapter;

public class AnnonceDetailActivity extends AppCompatActivity {

    public static final String ARG_ANNONCE = "ARG_ANNONCE";
    private static final int REQUEST_CODE_LOGIN = 100;

    @BindView(R.id.collapsing_toolbar_detail)
    CollapsingToolbarLayout collapsingToolbarLayout;

    @BindView(R.id.view_pager_detail)
    ViewPager viewPager;

    @BindView(R.id.indicator_detail)
    CircleIndicator indicator;

    @BindView(R.id.text_description_detail)
    TextView description;

    @BindView(R.id.text_view_prix_detail)
    TextView prix;

    @BindView(R.id.detail_toolbar)
    Toolbar toolbar;

    @BindView(R.id.fab_main_action)
    FloatingActionButton fabMainAction;

    private AnnoncePhotos annoncePhotos;

    public AnnonceDetailActivity() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_annonce_detail);
        ButterKnife.bind(this);

        Bundle arguments = getIntent().getExtras();

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (arguments != null) {
            annoncePhotos = arguments.getParcelable(ARG_ANNONCE);
        }

        if (annoncePhotos != null) {
            prix.setText(String.valueOf(String.format(Locale.FRANCE, "%,d", annoncePhotos.getAnnonceEntity().getPrix()) + " XPF"));
            description.setText(annoncePhotos.getAnnonceEntity().getDescription());
            collapsingToolbarLayout.setTitle(annoncePhotos.getAnnonceEntity().getTitre());
            if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
                viewPager.setAdapter(new AnnonceViewPagerAdapter(this, annoncePhotos.getPhotos()));
                indicator.setViewPager(viewPager);
            }
        }

        // Désactive le bouton de contact s'il s'agit de notre annonce
        if (annoncePhotos.getAnnonceEntity().getUuidUtilisateur().equals(FirebaseAuth.getInstance().getUid())) {
            fabMainAction.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.fab_main_action)
    public void mainAction() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            CallLoginUi.callLoginUi(this, REQUEST_CODE_LOGIN);
        } else {
            callListMessageFragment();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_LOGIN && resultCode == RESULT_OK) {
            callListMessageFragment();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void callListMessageFragment() {
        Intent intent = new Intent();
        intent.setClass(AnnonceDetailActivity.this, AnnonceMessageActivity.class);
        intent.putExtra(ARG_ANNONCE, annoncePhotos.getAnnonceEntity());
        startActivity(intent);
    }
}
