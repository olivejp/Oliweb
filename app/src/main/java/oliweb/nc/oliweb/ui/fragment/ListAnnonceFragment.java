package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.helper.SharedPreferencesHelper;
import oliweb.nc.oliweb.ui.EndlessRecyclerOnScrollListener;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.task.LoadMoreTaskBundle;
import oliweb.nc.oliweb.ui.task.LoadMostRecentAnnonceTask;
import oliweb.nc.oliweb.ui.task.TaskListener;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_ANNONCE_REF;
import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;

public class ListAnnonceFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, AnnonceBeautyAdapter.AnnonceAdapterListener {
    private static final String ARG_UID_USER = "ARG_UID_USER";
    private static final String ARG_ACTION = "ARG_ACTION";

    public static final String ACTION_FAVORITE = "ACTION_FAVORITE";
    public static final String ACTION_MOST_RECENT = "ACTION_MOST_RECENT";

    public static final String SAVE_LIST_ANNONCE = "SAVE_LIST_ANNONCE";
    public static final String SAVE_SORT = "SAVE_SORT";
    public static final String SAVE_DIRECTION = "SAVE_DIRECTION";

    public static final int SORT_DATE = 1;
    public static final int SORT_TITLE = 2;
    public static final int SORT_PRICE = 3;

    public static final int ASC = 1;
    public static final int DESC = 2;

    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.recycler_list_annonces)
    RecyclerView recyclerView;

    @BindView(R.id.empty_favorite_linear)
    LinearLayout linearLayout;

    private String uidUser;
    private String action;
    private AppCompatActivity appCompatActivity;
    private MainActivityViewModel viewModel;
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private ArrayList<AnnoncePhotos> annoncePhotosList = new ArrayList<>();
    private DatabaseReference annoncesReference;
    private int sort = SORT_DATE;
    private int direction;

    public ListAnnonceFragment() {
        // Empty constructor
    }

    public static ListAnnonceFragment getInstance(String uidUtilisateur, String action) {
        ListAnnonceFragment listAnnonceFragment = new ListAnnonceFragment();
        Bundle bundle = new Bundle();
        bundle.putString(ARG_UID_USER, uidUtilisateur);
        bundle.putString(ARG_ACTION, action);
        listAnnonceFragment.setArguments(bundle);
        return listAnnonceFragment;
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
        View view = inflater.inflate(R.layout.fragment_list_annonce_entity, container, false);

        ButterKnife.bind(this, view);

        annonceBeautyAdapter = new AnnonceBeautyAdapter(this);

        RecyclerView.LayoutManager layoutManager;
        layoutManager = Utility.initGridLayout(appCompatActivity, recyclerView, annonceBeautyAdapter);

        recyclerView.setAdapter(annonceBeautyAdapter);

        ActionBar actionBar = appCompatActivity.getSupportActionBar();

        if (savedInstanceState != null) {
            annoncePhotosList = savedInstanceState.getParcelableArrayList(SAVE_LIST_ANNONCE);
            annonceBeautyAdapter.setListAnnonces(annoncePhotosList);
        }

        swipeRefreshLayout.setOnRefreshListener(this);

        viewModel.sortingUpdated().observe(this, this::changeSortAndUpdateList);

        switch (action) {
            case ACTION_FAVORITE:
                if (uidUser != null) {
                    if (actionBar != null) {
                        actionBar.setTitle("Vos favoris");
                    }
                    viewModel.getFavoritesByUidUser(uidUser).observe(this, annoncePhotos -> {
                        if (annoncePhotos != null && !annoncePhotos.isEmpty()) {
                            annoncePhotosList = (ArrayList<AnnoncePhotos>) annoncePhotos;
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
                break;
        }

        if (savedInstanceState == null) {
            changeSortAndUpdateList(SharedPreferencesHelper.getInstance(appCompatActivity).getPrefSort());
        }

        return view;
    }

    private void changeSortAndUpdateList(Integer newSort) {
        if (newSort != null) {
            switch (newSort) {
                case 0:
                    sort = SORT_PRICE;
                    direction = DESC;
                    break;
                case 1:
                    sort = SORT_PRICE;
                    direction = ASC;
                    break;
                case 2:
                    sort = SORT_DATE;
                    direction = ASC;
                    break;
                case 3:
                default:
                    sort = SORT_DATE;
                    direction = DESC;
                    break;
            }

            if (action.equals(ACTION_MOST_RECENT)) {
                ArrayList<AnnoncePhotos> list = new ArrayList<>();
                annonceBeautyAdapter.setListAnnonces(list);
                annonceBeautyAdapter.notifyDataSetChanged();
                annoncePhotosList = list;
                loadMoreDatas();
            } else if (action.equals(ACTION_FAVORITE)) {
                LoadMostRecentAnnonceTask.sortList(annoncePhotosList, this.sort, direction);
                annonceBeautyAdapter.notifyDataSetChanged();
            }
        }

        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void loadMoreDatas() {
        switch (sort) {
            case SORT_DATE:
                loadSortDate().addListenerForSingleValueEvent(valueEventListener);
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
            query.startAt(lastPrice).limitToFirst(Constants.PER_PAGE_REQUEST);
        } else if (direction == DESC) {
            Integer lastPrice = Integer.MAX_VALUE;
            for (AnnoncePhotos annoncePhotos : annoncePhotosList) {
                if (lastPrice < annoncePhotos.getAnnonceEntity().getPrix()) {
                    lastPrice = annoncePhotos.getAnnonceEntity().getPrix();
                }
            }
            query.endAt(lastPrice).limitToLast(Constants.PER_PAGE_REQUEST);
        }
        return query;
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
            query.startAt(lastDate).limitToFirst(Constants.PER_PAGE_REQUEST);
        } else if (direction == DESC) {
            Long lastDate = Long.MAX_VALUE;
            for (AnnoncePhotos annoncePhotos : annoncePhotosList) {
                if (lastDate < annoncePhotos.getAnnonceEntity().getDatePublication()) {
                    lastDate = annoncePhotos.getAnnonceEntity().getDatePublication();
                }
            }
            query.endAt(lastDate).limitToLast(Constants.PER_PAGE_REQUEST);
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
                loadMoreTask.execute(new LoadMoreTaskBundle(annoncePhotosList, dataSnapshot, sort, direction));
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Do nothing
        }
    };

    private TaskListener<ArrayList<AnnoncePhotos>> taskListener = listAnnoncePhotos -> {
        annonceBeautyAdapter.setListAnnonces(listAnnoncePhotos);
        annoncePhotosList = listAnnoncePhotos;
        annoncesReference.removeEventListener(valueEventListener);
    };

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVE_LIST_ANNONCE, annoncePhotosList);
        outState.putInt(SAVE_SORT, sort);
        outState.putInt(SAVE_DIRECTION, direction);
    }

    @Override
    public void onClick(AnnoncePhotos annoncePhotos, AnnonceBeautyAdapter.ViewHolderBeauty viewHolder) {
        Intent intent = new Intent(appCompatActivity, AnnonceDetailActivity.class);
        intent.putExtra(ARG_ANNONCE, annoncePhotos);

        Pair<View, String> pairImage = new Pair<>(viewHolder.getImageView(), getString(R.string.image_detail_transition));
        Pair<View, String> pairPrix = new Pair<>(viewHolder.getTextPrixAnnonce(), getString(R.string.prix_detail_transition));

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(appCompatActivity, pairImage, pairPrix);
        startActivity(intent, options.toBundle());
    }

    @Override
    public void onShare(AnnoncePhotos annoncePhotos, AnnonceBeautyAdapter.ViewHolderBeauty viewHolder) {

    }

    @Override
    public void onLike(AnnoncePhotos annoncePhotos, AnnonceBeautyAdapter.ViewHolderBeauty viewHolder) {

    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        changeSortAndUpdateList(SharedPreferencesHelper.getInstance(appCompatActivity).getPrefSort());
    }
}
