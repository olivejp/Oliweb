package oliweb.nc.oliweb.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.CategorieEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.CategorieMiniAdapter;


public class ListCategorieFragment extends Fragment {
    private static final String TAG = ListCategorieFragment.class.getName();

    private static final String ARG_LIST_CATEGORY = "ARG_LIST_CATEGORY";
    public static final Long ID_ALL_CATEGORY = -1L;

    private List<CategorieEntity> categorieEntities;
    private AppCompatActivity appCompatActivity;
    private MainActivityViewModel viewModel;
    private CategorieMiniAdapter categorieMiniAdapter;

    @BindView(R.id.recycler_by_category)
    RecyclerView recyclerView;

    private View.OnClickListener onClickListener = (View v) -> {
        CategorieEntity categorieEntity = (CategorieEntity) v.getTag();
        viewModel.setCategorySelected(categorieEntity);
    };

    public ListCategorieFragment() {
        // Required empty public constructor
    }

    public static ListCategorieFragment newInstance(List<CategorieEntity> categorieEntities) {
        ListCategorieFragment fragment = new ListCategorieFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_LIST_CATEGORY, (ArrayList<? extends Parcelable>) categorieEntities);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            categorieEntities = getArguments().getParcelableArrayList(ARG_LIST_CATEGORY);
        }

        // Récupération du viewModel
        viewModel = ViewModelProviders.of(appCompatActivity).get(MainActivityViewModel.class);
        viewModel.getCategorySelected().observe(this, categorieEntity -> {
            if (categorieEntity != null) {
                categorieMiniAdapter.setCategorieSelected(categorieEntity.getId());
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_categorie, container, false);
        ButterKnife.bind(this, view);

        // Création d'un adapter pour les annonces
        categorieMiniAdapter = new CategorieMiniAdapter(appCompatActivity, onClickListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(appCompatActivity);
        linearLayoutManager.setOrientation(RecyclerView.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(categorieMiniAdapter);
        categorieMiniAdapter.setCategorieEntities(categorieEntities);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            appCompatActivity = (AppCompatActivity) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "Context should be an AppCompatActivity", e);
        }
    }
}
