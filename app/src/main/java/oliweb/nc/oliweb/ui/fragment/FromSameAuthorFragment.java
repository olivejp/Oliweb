package oliweb.nc.oliweb.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.converter.AnnonceConverter;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.dto.firebase.AnnonceFirebase;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.AnnonceDetailActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceMiniAdapter;

import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;


public class FromSameAuthorFragment extends androidx.fragment.app.Fragment {
    private static final String TAG = FromSameAuthorFragment.class.getName();

    private static final String ARG_UID_USER = "ARG_UID_USER";
    private static final String ARG_UID_ANNONCE_TO_AVOID = "ARG_UID_ANNONCE_TO_AVOID";

    private String uidUser;
    private String uidAnnonceToAvoid;
    private AppCompatActivity appCompatActivity;
    private AnnonceDetailActivityViewModel viewModel;
    private AnnonceMiniAdapter annonceMiniAdapter;

    @BindView(R.id.recycler_from_same_salesman)
    RecyclerView recyclerView;

    private View.OnClickListener onClickListener = (View v) -> {
        AnnonceFull annonceFull = (AnnonceFull) v.getTag();
        Intent intent = new Intent(appCompatActivity, AnnonceDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_ANNONCE, annonceFull);
        intent.putExtras(bundle);
        startActivity(intent);
    };

    public FromSameAuthorFragment() {
        // Required empty public constructor
    }

    public static FromSameAuthorFragment newInstance(String uidUser, String uidAnnonceToAvoid) {
        FromSameAuthorFragment fragment = new FromSameAuthorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_UID_USER, uidUser);
        args.putString(ARG_UID_ANNONCE_TO_AVOID, uidAnnonceToAvoid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            uidUser = getArguments().getString(ARG_UID_USER);
            uidAnnonceToAvoid = getArguments().getString(ARG_UID_ANNONCE_TO_AVOID);
        }
    }

    private void setListAnnonces(List<AnnonceFirebase> annonces) {
        List<AnnonceFirebase> newList = new ArrayList<>();
        if (uidAnnonceToAvoid != null) {
            for (AnnonceFirebase annonce : annonces) {
                if (!annonce.getUuid().equals(uidAnnonceToAvoid)) {
                    newList.add(annonce);
                }
            }
            annonces = newList; // On remplace notre liste de départ par notre liste épurée
        }
        annonceMiniAdapter.setListAnnonces(AnnonceConverter.convertDtosToAnnonceFulls(annonces));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_from_same_author, container, false);
        ButterKnife.bind(this, view);

        // Création d'un adapter pour les annonces
        annonceMiniAdapter = new AnnonceMiniAdapter(onClickListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(appCompatActivity);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(annonceMiniAdapter);

        // Instanciation d'un viewmodel
        viewModel = ViewModelProviders.of(this).get(AnnonceDetailActivityViewModel.class);
        viewModel.getListAnnonceByUidUser(uidUser)
                .doOnSuccess(this::setListAnnonces)
                .doOnError(throwable -> recyclerView.setVisibility(View.GONE))
                .subscribe();

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            appCompatActivity = (AppCompatActivity) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "Context should be an AppCompatActivity and implements SignInActivity", e);
        }
    }
}
