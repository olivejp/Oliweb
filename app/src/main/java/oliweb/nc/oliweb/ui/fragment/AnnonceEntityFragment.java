package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.fragment.dummy.DummyContent;

public class AnnonceEntityFragment extends Fragment {

    // TODO: Customize parameters
    private int mColumnCount = 2;

    private static final String ARG_UID_USER = "ARG_UID_USER";
    private static final String ARG_ACTION = "ARG_ACTION";

    @BindView(R.id.recycler_list_annonces)
    private RecyclerView recyclerView;

    private String uidUser;
    private String action;
    private AppCompatActivity appCompatActivity;

    public AnnonceEntityFragment() {
        // Empty constructor
    }

    public AnnonceEntityFragment getInstance(String uidUtilisateur, String action) {
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

        ViewModelProviders.of(this).get();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_annonceentity_list, container, false);

        ButterKnife.bind(this, view);

        // Set the adapter
        recyclerView.setLayoutManager(new GridLayoutManager(appCompatActivity, mColumnCount));
        recyclerView.setAdapter(new MyAnnonceEntityRecyclerViewAdapter(DummyContent.ITEMS));
        return view;
    }
}
