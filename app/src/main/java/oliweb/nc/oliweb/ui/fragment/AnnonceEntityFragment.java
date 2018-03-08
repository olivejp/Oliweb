package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.helper.SharedPreferencesHelper;
import oliweb.nc.oliweb.ui.EndlessRecyclerOnScrollListener;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapter;
import oliweb.nc.oliweb.ui.task.LoadMostRecentAnnonceTask;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_ANNONCE_REF;

public class AnnonceEntityFragment extends Fragment {
    private static final String TAG = AnnonceEntityFragment.class.getName();
    private static final String ARG_UID_USER = "ARG_UID_USER";
    private static final String ARG_ACTION = "ARG_ACTION";

    public static final String ACTION_FAVORITE = "ACTION_FAVORITE";
    public static final String ACTION_MOST_RECENT = "ACTION_MOST_RECENT";

    public static final int SORT_DATE = 1;
    public static final int SORT_TITLE = 2;
    public static final int SORT_PRICE = 3;

    @BindView(R.id.recycler_list_annonces)
    RecyclerView recyclerView;

    @BindView(R.id.empty_favorite_linear)
    LinearLayout linearLayout;

    @BindView(R.id.bottom_navigation_sort)
    BottomNavigationView bottomNavigationView;

    private String uidUser;
    private String action;
    private AppCompatActivity appCompatActivity;
    private MainActivityViewModel viewModel;
    private AnnonceAdapter annonceAdapter;
    private List<AnnoncePhotos> annoncePhotosList = new ArrayList<>();
    private int pagingNumber = 10;
    private EndlessRecyclerOnScrollListener endlessRecyclerOnScrollListener;
    private DatabaseReference annoncesReference;
    private int tri = SORT_DATE;
    private int sens;

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
        annoncesReference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_ANNONCE_REF);
        viewModel = ViewModelProviders.of(appCompatActivity).get(MainActivityViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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

        annonceAdapter = new AnnonceAdapter(displayType, null, null, null);
        recyclerView.setAdapter(annonceAdapter);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            boolean raz = false;
            switch (item.getItemId()) {
                case R.id.action_sort_date:
                    if (tri == SORT_DATE) {
                        if (sens == 1) {
                            sens = 2;
                        } else {
                            sens = 1;
                        }
                    } else {
                        raz = true;
                        tri = SORT_DATE;
                        sens = 1;
                    }
                    break;
                case R.id.action_sort_title:
                    if (tri == SORT_TITLE) {
                        if (sens == 1) {
                            sens = 2;
                        } else {
                            sens = 1;
                        }
                    } else {
                        raz = true;
                        tri = SORT_TITLE;
                        sens = 1;
                    }
                    break;
                case R.id.action_sort_price:
                    if (tri == SORT_PRICE) {
                        if (sens == 1) {
                            sens = 2;
                        } else {
                            sens = 1;
                        }
                    } else {
                        raz = true;
                        tri = SORT_PRICE;
                        sens = 1;
                    }
                    break;
            }
            if (raz) {
                List<AnnoncePhotos> list = new ArrayList<>();
                annonceAdapter.setListAnnonces(list);
                annonceAdapter.notifyDataSetChanged();
                annoncePhotosList = list;
                loadMoreDatas();
            }
            return true;
        });

        switch (action) {
            case ACTION_FAVORITE:
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
                break;
            case ACTION_MOST_RECENT:
                endlessRecyclerOnScrollListener = new EndlessRecyclerOnScrollListener((LinearLayoutManager) layoutManager) {
                    @Override
                    public void onLoadMore() {
                        loadMoreDatas();
                    }
                };
                recyclerView.addOnScrollListener(endlessRecyclerOnScrollListener);
                loadMoreDatas();
                break;
        }
        return view;
    }

    private void loadMoreDatas() {
        switch (tri) {
            case SORT_DATE:
                loadSortDate().addListenerForSingleValueEvent(valueEventListener);
            case SORT_TITLE:
                loadSortTitle().addListenerForSingleValueEvent(valueEventListener);
            case SORT_PRICE:
                loadSortPrice().addListenerForSingleValueEvent(valueEventListener);
        }
    }

    private Query loadSortPrice() {
        // Recherche du prix le plus élevé
        Integer lastPrice = 999999999;
        for (AnnoncePhotos annoncePhotos : annoncePhotosList) {
            if (lastPrice > annoncePhotos.getAnnonceEntity().getPrix()) {
                lastPrice = annoncePhotos.getAnnonceEntity().getPrix();
            }
        }
        return annoncesReference.orderByChild("prix").endAt(lastPrice).limitToLast(pagingNumber);
    }

    private Query loadSortTitle() {
        // Recherche du titre le plus haut
        // TODO trouver la valeur max d'un string
        String lastTitle = "ZZZZZZZZZZZZ";
        for (AnnoncePhotos annoncePhotos : annoncePhotosList) {
            if (lastTitle.compareTo(annoncePhotos.getAnnonceEntity().getTitre()) == 1) {
                lastTitle = annoncePhotos.getAnnonceEntity().getTitre();
            }
        }
        return annoncesReference.orderByChild("titre").endAt(lastTitle).limitToLast(pagingNumber);
    }

    private Query loadSortDate() {
        // Recherche de la date de publication la plus éloignée
        Long lastDate = Utility.getNowInEntityFormat();
        for (AnnoncePhotos annoncePhotos : annoncePhotosList) {
            if (lastDate > annoncePhotos.getAnnonceEntity().getDatePublication()) {
                lastDate = annoncePhotos.getAnnonceEntity().getDatePublication();
            }
        }
        return annoncesReference.orderByChild("datePublication").endAt(lastDate).limitToLast(pagingNumber);
    }

    private ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot != null && dataSnapshot.getValue() != null) {

                // Lancement d'une tache pour aller vérifier les annonces déjà reçues
                LoadMostRecentAnnonceTask loadMoreTask = new LoadMostRecentAnnonceTask();
                loadMoreTask.setListener(listAnnoncePhotos -> {
                    annonceAdapter.setListAnnonces(listAnnoncePhotos);
                    annoncePhotosList = listAnnoncePhotos;
                });
                loadMoreTask.execute(new Pair(annoncePhotosList, dataSnapshot));
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Do nothing
        }
    };
}
