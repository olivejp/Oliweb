package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.helper.SharedPreferencesHelper;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapter;

public class AnnonceEntityFragment extends Fragment {

    private static final String ARG_UID_USER = "ARG_UID_USER";
    private static final String ARG_ACTION = "ARG_ACTION";

    @BindView(R.id.recycler_list_annonces)
    RecyclerView recyclerView;

    @BindView(R.id.empty_favorite_linear)
    LinearLayout linearLayout;

    private String uidUser;
    private String action;
    private AppCompatActivity appCompatActivity;
    private MainActivityViewModel viewModel;

    public AnnonceEntityFragment() {
        // Empty constructor
    }

    public static AnnonceEntityFragment getInstance(String uidUtilisateur, String action) {
        AnnonceEntityFragment annonceEntityFragment = new AnnonceEntityFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_UID_USER, uidUtilisateur);
        bundle.putString(ARG_ACTION, action);
        annonceEntityFragment.setArguments(bundle);
        return annonceEntityFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            uidUser = getArguments().getString(ARG_UID_USER);
            action = getArguments().getString(ARG_ACTION);
        }

        viewModel = ViewModelProviders.of(appCompatActivity).get(MainActivityViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_annonceentity_list, container, false);

        ButterKnife.bind(this, view);

        boolean displayBeauty = SharedPreferencesHelper.getInstance(getContext()).getDisplayBeautyMode();
        AnnonceAdapter.DisplayType displayType = displayBeauty ? AnnonceAdapter.DisplayType.BEAUTY : AnnonceAdapter.DisplayType.RAW;
        RecyclerView.LayoutManager layoutManager;
        if (SharedPreferencesHelper.getInstance(getContext()).getGridMode()) {
            layoutManager = new GridLayoutManager(appCompatActivity, 2);
        } else {
            layoutManager = new LinearLayoutManager(appCompatActivity);
        }
        recyclerView.setLayoutManager(layoutManager);

        AnnonceAdapter annonceAdapter = new AnnonceAdapter(displayType, null, null, null);
        recyclerView.setAdapter(annonceAdapter);

        if (uidUser != null) {
            viewModel.getFavoritesByUidUser(uidUser).observe(this, annoncePhotos -> {
                if (annoncePhotos != null && !annoncePhotos.isEmpty()) {
                    annonceAdapter.setListAnnonces(annoncePhotos);
                } else {
                    linearLayout.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
            });
        }

        return view;
    }
}
