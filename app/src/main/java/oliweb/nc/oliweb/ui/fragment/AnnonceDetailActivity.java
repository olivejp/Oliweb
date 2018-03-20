package oliweb.nc.oliweb.ui.fragment;


import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.relex.circleindicator.CircleIndicator;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.ui.adapter.AnnonceViewPagerAdapter;

public class AnnonceDetailActivity extends AppCompatActivity {

    public static final String ARG_ANNONCE = "ARG_ANNONCE";

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

        if (arguments != null) {
            annoncePhotos = arguments.getParcelable(ARG_ANNONCE);
        }

        if (annoncePhotos != null) {
            prix.setText(String.valueOf(String.format(Locale.FRANCE,"%,d", annoncePhotos.getAnnonceEntity().getPrix()) + " XPF"));
            description.setText(annoncePhotos.getAnnonceEntity().getDescription());
            collapsingToolbarLayout.setTitle(annoncePhotos.getAnnonceEntity().getTitre());
            if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
                viewPager.setAdapter(new AnnonceViewPagerAdapter(this, annoncePhotos.getPhotos()));
                indicator.setViewPager(viewPager);
            }
        }
    }
}
