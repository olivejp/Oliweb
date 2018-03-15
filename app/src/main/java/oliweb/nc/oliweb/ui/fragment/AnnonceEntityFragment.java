package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
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
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.task.LoadMoreTaskBundle;
import oliweb.nc.oliweb.ui.task.LoadMostRecentAnnonceTask;
import oliweb.nc.oliweb.ui.task.TaskListener;

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

    public static final int ASC = 1;
    public static final int DESC = 2;

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
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private List<AnnoncePhotos> annoncePhotosList = new ArrayList<>();
    private int pagingNumber = 10;
    private DatabaseReference annoncesReference;
    private int tri = SORT_DATE;
    private int direction;

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

    private BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener = item -> {
        int newTri;
        switch (item.getItemId()) {
            case R.id.action_sort_date:
                newTri = SORT_DATE;
                break;
            case R.id.action_sort_title:
                newTri = SORT_TITLE;
                break;
            case R.id.action_sort_price:
                newTri = SORT_PRICE;
                break;
            default:
                newTri = SORT_DATE;
                break;
        }

        if (tri == newTri) {
            if (direction == ASC) {
                direction = DESC;
            } else {
                direction = ASC;
            }
        } else {
            tri = newTri;
            direction = ASC;
        }

        if (action.equals(ACTION_MOST_RECENT)) {
            List<AnnoncePhotos> list = new ArrayList<>();
            annonceBeautyAdapter.setListAnnonces(list);
            annonceBeautyAdapter.notifyDataSetChanged();
            annoncePhotosList = list;
            loadMoreDatas();
            return true;
        } else if (action.equals(ACTION_FAVORITE)) {
            LoadMostRecentAnnonceTask.sortList(annoncePhotosList, tri, direction);
            annonceBeautyAdapter.notifyDataSetChanged();
            return true;
        }
        return false;
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_annonce_entity_list, container, false);

        ButterKnife.bind(this, view);

        RecyclerView.LayoutManager layoutManager;
        if (SharedPreferencesHelper.getInstance(getContext()).getGridMode()) {
            GridLayoutManager gridLayoutManager = new GridLayoutManager(appCompatActivity, 2);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    switch (annonceBeautyAdapter.getItemViewType(position)) {
                        case 1:
                            return 1;
                        case 2:
                            return 2;
                        default:
                            return 1;
                    }

                }
            });
            layoutManager = gridLayoutManager;
        } else {
            layoutManager = new LinearLayoutManager(appCompatActivity);
        }
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(false);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        annonceBeautyAdapter = new AnnonceBeautyAdapter(v -> {
            AnnoncePhotos annoncePhotos = (AnnoncePhotos) v.getTag();
            if (getFragmentManager() != null) {
                AnnonceDetailFragment annonceDetailFragment = AnnonceDetailFragment.getInstance(annoncePhotos);
                getFragmentManager().beginTransaction().replace(R.id.main_frame, annonceDetailFragment).commit();
            }
        }, null, null);

        recyclerView.setAdapter(annonceBeautyAdapter);
        bottomNavigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        ActionBar actionBar = appCompatActivity.getSupportActionBar();

        switch (action) {
            case ACTION_FAVORITE:
                if (uidUser != null) {
                    if (actionBar != null) {
                        actionBar.setTitle("Annonces favorites");
                    }
                    viewModel.getFavoritesByUidUser(uidUser).observe(this, annoncePhotos -> {
                        if (annoncePhotos != null && !annoncePhotos.isEmpty()) {
                            annoncePhotosList = annoncePhotos;
                            annonceBeautyAdapter.setListAnnonces(annoncePhotosList);
                        } else {
                            linearLayout.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    });
                }
                break;
            case ACTION_MOST_RECENT:
                if (actionBar != null) {
                    actionBar.setTitle("Dernières annonces");
                }
                recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener(layoutManager) {
                    @Override
                    public void onLoadMore() {
                        loadMoreDatas();
                    }
                });
                loadMoreDatas();
                break;
        }
        return view;
    }

    private void loadMoreDatas() {
        switch (tri) {
            case SORT_DATE:
                loadSortDate().addListenerForSingleValueEvent(valueEventListener);
                break;
            case SORT_TITLE:
                loadSortTitle().addListenerForSingleValueEvent(valueEventListener);
                break;
            case SORT_PRICE:
                loadSortPrice().addListenerForSingleValueEvent(valueEventListener);
                break;
            default:
                break;
        }
    }

    private Query loadSortPrice() {
        Query query = annoncesReference.orderByChild("prix");
        if (direction == ASC) {
            Integer lastPrice = 0;
            for (AnnoncePhotos annoncePhotos : annoncePhotosList) {
                if (annoncePhotos.getAnnonceEntity().getPrix() > lastPrice) {
                    lastPrice = annoncePhotos.getAnnonceEntity().getPrix();
                }
            }
            query.startAt(lastPrice).limitToFirst(pagingNumber);
        } else if (direction == DESC) {
            Integer lastPrice = Integer.MAX_VALUE;
            for (AnnoncePhotos annoncePhotos : annoncePhotosList) {
                if (lastPrice < annoncePhotos.getAnnonceEntity().getPrix()) {
                    lastPrice = annoncePhotos.getAnnonceEntity().getPrix();
                }
            }
            query.endAt(lastPrice).limitToLast(pagingNumber);
        }
        return query;
    }

    private Query loadSortTitle() {
        // Recherche du titre le plus haut
        // TODO trouver la valeur max d'un string
        String lastTitle = "ZZZZZZZZZZZZ";
        for (AnnoncePhotos annoncePhotos : annoncePhotosList) {
            if (lastTitle.compareTo(annoncePhotos.getAnnonceEntity().getTitre()) < 0) {
                lastTitle = annoncePhotos.getAnnonceEntity().getTitre();
            }
        }
        return annoncesReference.orderByChild("titre").endAt(lastTitle).limitToLast(pagingNumber);
    }

    private Query loadSortDate() {
        Query query = annoncesReference.orderByChild("datePublication");
        if (direction == ASC) {
            Long lastDate = 0L;
            for (AnnoncePhotos annoncePhotos : annoncePhotosList) {
                if (annoncePhotos.getAnnonceEntity().getDatePublication() > lastDate) {
                    lastDate = annoncePhotos.getAnnonceEntity().getDatePublication();
                }
            }
            query.startAt(lastDate).limitToFirst(pagingNumber);
        } else if (direction == DESC) {
            Long lastDate = Long.MAX_VALUE;
            for (AnnoncePhotos annoncePhotos : annoncePhotosList) {
                if (lastDate < annoncePhotos.getAnnonceEntity().getDatePublication()) {
                    lastDate = annoncePhotos.getAnnonceEntity().getDatePublication();
                }
            }
            query.endAt(lastDate).limitToLast(pagingNumber);
        }
        return query;
    }

    private ValueEventListener valueEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot != null && dataSnapshot.getValue() != null) {
                // Lancement d'une tache pour aller vérifier les annonces déjà reçues
                LoadMostRecentAnnonceTask loadMoreTask = new LoadMostRecentAnnonceTask();
                loadMoreTask.setListener(taskListener);
                loadMoreTask.execute(new LoadMoreTaskBundle(annoncePhotosList, dataSnapshot, tri, direction));
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Do nothing
        }
    };

    private TaskListener<List<AnnoncePhotos>> taskListener = listAnnoncePhotos -> {
        annonceBeautyAdapter.setListAnnonces(listAnnoncePhotos);
        annoncePhotosList = listAnnoncePhotos;
        annoncesReference.removeEventListener(valueEventListener);
    };
}
