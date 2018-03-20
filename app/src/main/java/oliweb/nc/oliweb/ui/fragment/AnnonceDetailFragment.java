package oliweb.nc.oliweb.ui.fragment;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.relex.circleindicator.CircleIndicator;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.ui.adapter.AnnonceViewPagerAdapter;

public class AnnonceDetailFragment extends Fragment {

    private static final String ARG_ANNONCE = "ARG_ANNONCE";

    @BindView(R.id.collapsing_toolbar_detail)
    CollapsingToolbarLayout collapsingToolbarLayout;

    @BindView(R.id.view_pager_detail)
    ViewPager viewPager;

    @BindView(R.id.default_button)
    FloatingActionButton fabDefaultButton;

    @BindView(R.id.indicator_detail)
    CircleIndicator indicator;

    @BindView(R.id.text_description_detail)
    TextView description;

    private AnnoncePhotos annoncePhotos;

    private AppCompatActivity appCompatActivity;

    public AnnonceDetailFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    public static AnnonceDetailFragment getInstance(AnnoncePhotos annoncePhotos) {
        AnnonceDetailFragment annonceDetailFragment = new AnnonceDetailFragment();

        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_ANNONCE, annoncePhotos);

        annonceDetailFragment.setArguments(bundle);
        return annonceDetailFragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_annonce_detail, container, false);
        ButterKnife.bind(this, rootView);

        if (getArguments() != null) {
            annoncePhotos = getArguments().getParcelable(ARG_ANNONCE);
        }

        if (annoncePhotos != null) {
            description.setText(annoncePhotos.getAnnonceEntity().getDescription());
            collapsingToolbarLayout.setTitle(annoncePhotos.getAnnonceEntity().getTitre());
            if (annoncePhotos.getPhotos() != null && !annoncePhotos.getPhotos().isEmpty()) {
                viewPager.setAdapter(new AnnonceViewPagerAdapter(appCompatActivity, annoncePhotos.getPhotos()));
                indicator.setViewPager(viewPager);
            }
        }
        return rootView;
    }
}
