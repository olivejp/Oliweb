package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.service.sharing.DynamicLynksGenerator;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.ui.EndlessRecyclerOnScrollListener;
import oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity;
import oliweb.nc.oliweb.ui.activity.FavoritesActivity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MainActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceBeautyAdapter;
import oliweb.nc.oliweb.ui.dialog.LoadingDialogFragment;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.ui.task.LoadMoreTaskBundle;
import oliweb.nc.oliweb.ui.task.LoadMostRecentAnnonceTask;
import oliweb.nc.oliweb.ui.task.TaskListener;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static android.support.v4.app.ActivityOptionsCompat.makeSceneTransitionAnimation;
import static oliweb.nc.oliweb.ui.activity.AnnonceDetailActivity.ARG_ANNONCE;
import static oliweb.nc.oliweb.ui.activity.MainActivity.RC_SIGN_IN;
import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_ANNONCE_REF;

public class ListAnnonceFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener
        , NetworkReceiver.NetworkChangeListener {
    private static final String TAG = ListAnnonceFragment.class.getName();

    private static final String LOADING_DIALOG = "LOADING_DIALOG";

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

    @BindView(R.id.constraint_list_annonce)
    ConstraintLayout constraintLayout;

    @BindView(R.id.coordinator_layout_list_annonce)
    CoordinatorLayout coordinatorLayout;

    private String uidUser;
    private String action;
    private AppCompatActivity appCompatActivity;
    private MainActivityViewModel viewModel;
    private AnnonceBeautyAdapter annonceBeautyAdapter;
    private ArrayList<AnnonceFull> annoncePhotosList = new ArrayList<>();
    private DatabaseReference annoncesReference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_ANNONCE_REF);
    private int sortSelected = SORT_DATE;
    private int directionSelected;
    private ActionBar actionBar;
    private LoadingDialogFragment loadingDialogFragment;
    private EndlessRecyclerOnScrollListener scrollListener;

    /**
     * OnClickListener that should open AnnonceDetailActivity
     */
    private View.OnClickListener onClickListener = (View v) -> {
        AnnonceBeautyAdapter.ViewHolderBeauty viewHolderBeauty = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
        Intent intent = new Intent(appCompatActivity, AnnonceDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARG_ANNONCE, viewHolderBeauty.getAnnoncePhotos());
        intent.putExtras(bundle);
        Pair<View, String> pairImage = new Pair<>(viewHolderBeauty.getImageView(), getString(R.string.image_detail_transition));
        ActivityOptionsCompat options = makeSceneTransitionAnimation(appCompatActivity, pairImage);
        startActivity(intent, options.toBundle());
    };

    /**
     * OnClickListener that share an annonce with a DynamicLink
     */
    private View.OnClickListener onClickListenerShare = v -> {
        if (uidUser != null && !uidUser.isEmpty()) {
            AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
            AnnonceFull annoncePhotos = viewHolder.getAnnoncePhotos();
            AnnonceEntity annonceEntity = annoncePhotos.getAnnonce();

            // Display a loading spinner
            loadingDialogFragment = new LoadingDialogFragment();
            loadingDialogFragment.setText(getString(R.string.dynamic_link_creation));
            loadingDialogFragment.show(appCompatActivity.getSupportFragmentManager(), LOADING_DIALOG);

            DynamicLynksGenerator.generateShortLink(uidUser, annonceEntity, annoncePhotos.photos, new DynamicLynksGenerator.DynamicLinkListener() {
                @Override
                public void getLink(Uri shortLink, Uri flowchartLink) {
                    loadingDialogFragment.dismiss();
                    Intent sendIntent = new Intent();
                    String msg = getString(R.string.default_text_share_link) + shortLink;
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
                    sendIntent.setType("text/plain");
                    startActivity(sendIntent);
                }

                @Override
                public void getLinkError() {
                    loadingDialogFragment.dismiss();
                    Snackbar.make(coordinatorLayout, R.string.link_share_error, Snackbar.LENGTH_LONG).show();
                }
            });
        } else {
            Snackbar.make(coordinatorLayout, R.string.sign_in_required, Snackbar.LENGTH_LONG)
                    .setAction(R.string.sign_in, v1 -> Utility.signIn(appCompatActivity, RC_SIGN_IN))
                    .show();
        }
    };

    /**
     * OnClickListener that adds an annonce and all of this photo into the favorite.
     * This save all the photos of the annonce in the device and the annonce into the local database
     * If the annonce was already into the database it remove all the photo from the device,
     * delete all the photos from the database,
     * delete the annonce from the database.
     */
    private View.OnClickListener onClickListenerFavorite = (View v) -> {
        if (uidUser == null || uidUser.isEmpty()) {
            Snackbar.make(coordinatorLayout, getString(R.string.sign_in_required), Snackbar.LENGTH_LONG)
                    .setAction(getString(R.string.sign_in), v1 -> Utility.signIn(appCompatActivity, RC_SIGN_IN))
                    .show();
        } else {
            AnnonceBeautyAdapter.ViewHolderBeauty viewHolder = (AnnonceBeautyAdapter.ViewHolderBeauty) v.getTag();
            viewModel.addOrRemoveFromFavorite(FirebaseAuth.getInstance().getUid(), viewHolder.getAnnoncePhotos())
                    .observeOnce(addRemoveFromFavorite -> {
                        if (addRemoveFromFavorite != null) {
                            switch (addRemoveFromFavorite) {
                                case ONE_OF_YOURS:
                                    Toast.makeText(appCompatActivity, R.string.action_impossible_own_this_annonce, Toast.LENGTH_LONG).show();
                                    break;
                                case ADD_SUCCESSFUL:
                                    Snackbar.make(coordinatorLayout, R.string.AD_ADD_TO_FAVORITE, Snackbar.LENGTH_LONG)
                                            .setAction(R.string.MY_FAVORITE, v12 -> callFavoriteAnnonceActivity())
                                            .show();
                                    break;
                                case REMOVE_SUCCESSFUL:
                                    Snackbar.make(recyclerView, R.string.annonce_remove_from_favorite, Snackbar.LENGTH_LONG).show();
                                    break;
                                case REMOVE_FAILED:
                                    Toast.makeText(appCompatActivity, R.string.remove_from_favorite_failed, Toast.LENGTH_LONG).show();
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
        }
    };

    private void callFavoriteAnnonceActivity() {
        Intent intent = new Intent();
        intent.setClass(appCompatActivity, FavoritesActivity.class);
        intent.putExtra(ARG_UID_USER, uidUser);
        startActivity(intent);
        appCompatActivity.overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }

    public ListAnnonceFragment() {
        // Empty constructor
    }

    public static synchronized ListAnnonceFragment getInstance(String uidUtilisateur, String
            action) {
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
        try {
            appCompatActivity = (AppCompatActivity) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "Context should be an AppCompatActivity and implements SignInActivity", e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            uidUser = getArguments().getString(ARG_UID_USER);
            action = getArguments().getString(ARG_ACTION);
        }
        viewModel = ViewModelProviders.of(appCompatActivity).get(MainActivityViewModel.class);
        viewModel.getLiveUserConnected().observe(this, userEntity ->
                uidUser = (userEntity != null) ? userEntity.getUid() : null
        );

        NetworkReceiver.getInstance().listen(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_annonce, container, false);

        ButterKnife.bind(this, view);

        Snackbar snackbar = Snackbar.make(coordinatorLayout, R.string.network_unavailable, BaseTransientBottomBar.LENGTH_INDEFINITE);
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setBackgroundColor(appCompatActivity.getResources().getColor(R.color.colorAccentDarker));
        annonceBeautyAdapter = new AnnonceBeautyAdapter(appCompatActivity.getResources().getColor(R.color.colorPrimary),
                onClickListener,
                onClickListenerShare,
                onClickListenerFavorite);

        recyclerView.setAdapter(annonceBeautyAdapter);

        actionBar = appCompatActivity.getSupportActionBar();

        if (savedInstanceState != null) {
            annoncePhotosList = savedInstanceState.getParcelableArrayList(SAVE_LIST_ANNONCE);
            annonceBeautyAdapter.setListAnnonces(annoncePhotosList);
        }

        swipeRefreshLayout.setOnRefreshListener(this);

        viewModel.sortingUpdated().observe(this, this::changeSortAndUpdateList);

        viewModel.getIsNetworkAvailable().observe(this, atomicBoolean -> {
            if (atomicBoolean != null && !atomicBoolean.get()) {
                snackbar.show();
            } else {
                snackbar.dismiss();
            }
        });

        initAccordingToAction();

        if (savedInstanceState == null) {
            changeSortAndUpdateList(SharedPreferencesHelper.getInstance(appCompatActivity).getPrefSort());
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        GlideApp.get(appCompatActivity).clearMemory();
        recyclerView.setAdapter(null);
        recyclerView.removeOnScrollListener(scrollListener);
        annoncesReference.removeEventListener(loadSortListener);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(SAVE_LIST_ANNONCE, annoncePhotosList);
        outState.putInt(SAVE_SORT, sortSelected);
        outState.putInt(SAVE_DIRECTION, directionSelected);
    }

    @Override
    public void onPause() {
        super.onPause();
        NetworkReceiver.getInstance().removeListener(this);
    }

    @Override
    public void onRefresh() {
        swipeRefreshLayout.setRefreshing(true);
        changeSortAndUpdateList(SharedPreferencesHelper.getInstance(appCompatActivity).getPrefSort());
    }

    private void initAccordingToAction() {
        switch (action) {
            case ACTION_FAVORITE:
                if (uidUser != null) {
                    if (actionBar != null) {
                        actionBar.setTitle(R.string.MY_FAVORITE);
                    }
                    viewModel.getFavoritesByUidUser(uidUser).observe(this, annoncePhotos -> {
                        if (annoncePhotos != null && !annoncePhotos.isEmpty()) {
                            Utility.initGridLayout(appCompatActivity, recyclerView, annonceBeautyAdapter);
                            annoncePhotosList = (ArrayList<AnnonceFull>) annoncePhotos;
                            annonceBeautyAdapter.setListAnnonces(annoncePhotosList);
                            linearLayout.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            linearLayout.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    });
                }
                break;
            case ACTION_MOST_RECENT:
                if (actionBar != null) {
                    actionBar.setTitle(R.string.RECENT_ADS);
                }
                RecyclerView.LayoutManager layoutManager = Utility.initGridLayout(appCompatActivity, recyclerView, annonceBeautyAdapter);
                scrollListener = new EndlessRecyclerOnScrollListener(layoutManager) {
                    @Override
                    public void onLoadMore() {
                        loadMoreDatas();
                    }
                };
                recyclerView.addOnScrollListener(scrollListener);
                break;
            default:
                break;
        }
    }

    private void changeSortAndUpdateList(Integer newSort) {
        if (newSort != null) {
            switch (newSort) {
                case 0:
                    sortSelected = SORT_PRICE;
                    directionSelected = DESC;
                    break;
                case 1:
                    sortSelected = SORT_PRICE;
                    directionSelected = ASC;
                    break;
                case 2:
                    sortSelected = SORT_DATE;
                    directionSelected = ASC;
                    break;
                case 3:
                default:
                    sortSelected = SORT_DATE;
                    directionSelected = DESC;
                    break;
            }

            if (action.equals(ACTION_MOST_RECENT)) {
                ArrayList<AnnonceFull> list = new ArrayList<>();
                annonceBeautyAdapter.setListAnnonces(list);
                annonceBeautyAdapter.notifyDataSetChanged();
                annoncePhotosList = list;
                loadMoreDatas();
            } else if (action.equals(ACTION_FAVORITE)) {
                LoadMostRecentAnnonceTask.sortList(annoncePhotosList, this.sortSelected, directionSelected);
                annonceBeautyAdapter.notifyDataSetChanged();
            }
        }

        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void loadMoreDatas() {
        switch (sortSelected) {
            case SORT_DATE:
                loadSortDate().addListenerForSingleValueEvent(loadSortListener);
                break;
            case SORT_PRICE:
                loadSortPrice().addListenerForSingleValueEvent(loadSortListener);
            default:
        }
    }

    private Query loadSortPrice() {
        Query query = annoncesReference.orderByChild("prix");
        if (directionSelected == ASC) {
            Integer lastPrice = 0;
            for (AnnonceFull annoncePhotos : annoncePhotosList) {
                if (annoncePhotos.getAnnonce().getPrix() > lastPrice) {
                    lastPrice = annoncePhotos.getAnnonce().getPrix();
                }
            }
            query.startAt(lastPrice).limitToFirst(Constants.PER_PAGE_REQUEST);
        } else if (directionSelected == DESC) {
            Integer lastPrice = Integer.MAX_VALUE;
            for (AnnonceFull annoncePhotos : annoncePhotosList) {
                if (lastPrice < annoncePhotos.getAnnonce().getPrix()) {
                    lastPrice = annoncePhotos.getAnnonce().getPrix();
                }
            }
            query.endAt(lastPrice).limitToLast(Constants.PER_PAGE_REQUEST);
        }
        return query;
    }

    private Query loadSortDate() {
        Query query = annoncesReference.orderByChild("datePublication");
        if (directionSelected == ASC) {
            Long lastDate = 0L;
            for (AnnonceFull annoncePhotos : annoncePhotosList) {
                if (annoncePhotos.getAnnonce().getDatePublication() > lastDate) {
                    lastDate = annoncePhotos.getAnnonce().getDatePublication();
                }
            }
            query.startAt(lastDate).limitToFirst(Constants.PER_PAGE_REQUEST);
        } else if (directionSelected == DESC) {
            Long lastDate = Long.MAX_VALUE;
            for (AnnonceFull annoncePhotos : annoncePhotosList) {
                if (lastDate < annoncePhotos.getAnnonce().getDatePublication()) {
                    lastDate = annoncePhotos.getAnnonce().getDatePublication();
                }
            }
            query.endAt(lastDate).limitToLast(Constants.PER_PAGE_REQUEST);
        }
        return query;
    }

    private ValueEventListener loadSortListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            if (dataSnapshot.getValue() != null) {
                LoadMostRecentAnnonceTask loadMoreTask = new LoadMostRecentAnnonceTask(taskListener);
                loadMoreTask.execute(new LoadMoreTaskBundle(annoncePhotosList, dataSnapshot, sortSelected, directionSelected));
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            // Do nothing
        }
    };

    private TaskListener<ArrayList<AnnonceFull>> taskListener = listAnnoncePhotos -> {
        annonceBeautyAdapter.setListAnnonces(listAnnoncePhotos);
        annoncePhotosList = listAnnoncePhotos;
        annoncesReference.removeEventListener(loadSortListener);
    };

    @Override
    public void onNetworkEnable() {
        viewModel.setIsNetworkAvailable(true);
    }

    @Override
    public void onNetworkDisable() {
        viewModel.setIsNetworkAvailable(false);
    }
}
