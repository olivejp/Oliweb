package oliweb.nc.oliweb.ui.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.RecyclerItemTouchHelper;
import oliweb.nc.oliweb.SharedPreferencesHelper;
import oliweb.nc.oliweb.Utilities;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyAnnoncesViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapterRaw;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapterSingle;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;

public class MyAnnoncesActivity extends AppCompatActivity implements RecyclerItemTouchHelper.SwipeListener, NoticeDialogFragment.DialogListener {

    @BindView(R.id.recycler_annonces)
    RecyclerView recyclerView;

    private static final String TAG = MyAnnoncesActivity.class.getName();
    public static final String ARG_NOTICE_BUNDLE_ID_ANNONCE = "ARG_NOTICE_BUNDLE_ID_ANNONCE";
    public static final String ARG_NOTICE_BUNDLE_POSITION = "ARG_NOTICE_BUNDLE_POSITION";
    public static final String DIALOG_TAG_DELETE = "DIALOG_TAG_DELETE";

    private MyAnnoncesViewModel viewModel;
    private AnnonceAdapterRaw annonceAdapterRaw;
    private AnnonceAdapterSingle annonceAdapterSingle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = ViewModelProviders.of(this).get(MyAnnoncesViewModel.class);

        setContentView(R.layout.activity_my_annonces);
        ButterKnife.bind(this);

        // Ouvre l'activité pour modifier l'annonce.
        View.OnClickListener onClickListener = v -> {
            AnnonceEntity annonce = (AnnonceEntity) v.getTag();
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            intent.setClass(this, PostAnnonceActivity.class);
            bundle.putString(PostAnnonceActivity.BUNDLE_KEY_MODE, Constants.PARAM_MAJ);
            bundle.putLong(PostAnnonceActivity.BUNDLE_KEY_ID_ANNONCE, annonce.getIdAnnonce());
            intent.putExtras(bundle);
            startActivity(intent);
        };

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        // Recherche du mode display actuellement dans les préférences.
        int displayMode = SharedPreferencesHelper.getInstance(getApplicationContext(), Constants.SHARED_PREFERENCE_NAME).getSharedPreferences().getInt(Constants.DISPLAY_MODE, Constants.DISPLAY_MODE_RAW);

        if (displayMode == Constants.DISPLAY_MODE_RAW) {
            // En mode Raw
            RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
            annonceAdapterRaw = new AnnonceAdapterRaw(onClickListener);
            recyclerView.setAdapter(annonceAdapterRaw);
            recyclerView.addItemDecoration(itemDecoration);

            // Ajout d'un swipe listener pour pouvoir supprimer l'annonce
            RecyclerItemTouchHelper recyclerItemTouchHelper = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(recyclerItemTouchHelper);
            itemTouchHelper.attachToRecyclerView(recyclerView);
        } else {
            // En mode Beauty
            annonceAdapterSingle = new AnnonceAdapterSingle(onClickListener);
            recyclerView.setAdapter(annonceAdapterSingle);
        }

        viewModel.findByUuidUtilisateur(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .observe(this, annonceWithPhotos -> {
                    if (displayMode == Constants.DISPLAY_MODE_RAW) {
                        annonceAdapterRaw.setListAnnonces(annonceWithPhotos);
                    } else {
                        annonceAdapterSingle.setListAnnonces(annonceWithPhotos);
                    }
                });
    }

    @Override
    public void onSwipe(RecyclerView.ViewHolder view, int direction) {
        try {
            AnnonceAdapterRaw.ViewHolder viewHolder = (AnnonceAdapterRaw.ViewHolder) view;
            AnnonceEntity annonce = viewHolder.getSingleAnnonce();

            // Création d'un bundle dans lequel on va passer nos items
            Bundle bundle = new Bundle();
            bundle.putLong(ARG_NOTICE_BUNDLE_ID_ANNONCE, annonce.getIdAnnonce());
            bundle.putInt(ARG_NOTICE_BUNDLE_POSITION, viewHolder.getAdapterPosition());

            if (direction == ItemTouchHelper.LEFT) {
                // Appel d'un fragment qui va demander à l'utilisateur s'il est sûr de vouloir supprimer le colis.
                Utilities.sendDialogByFragmentManagerWithRes(getSupportFragmentManager(),
                        String.format("Supprimer l'annonce %s ?\n\nLe numéro de suivi ainsi que toutes ses étapes seront perdues.", annonce.getTitre()),
                        NoticeDialogFragment.TYPE_BOUTON_YESNO,
                        R.drawable.ic_delete_grey_900_24dp,
                        DIALOG_TAG_DELETE,
                        bundle,
                        this);
            }
        } catch (ClassCastException e) {
            Log.e(TAG, "La vue doit contenir un ColisEntity comme Tag");
        }
    }

    @Override
    public void onDialogPositiveClick(NoticeDialogFragment dialog) {
        if (dialog.getTag() != null && dialog.getTag().equals(DIALOG_TAG_DELETE)) {
            if (dialog.getBundle() != null && dialog.getBundle().containsKey(ARG_NOTICE_BUNDLE_ID_ANNONCE)) {
                long idAnnonce = dialog.getBundle().getLong(ARG_NOTICE_BUNDLE_ID_ANNONCE);
                if (idAnnonce != 0) {
                    viewModel.deleteAnnonceById(idAnnonce, dataReturn -> {
                        if (dataReturn.getNb() > 0)
                            Snackbar.make(recyclerView, "Annonce supprimée", Snackbar.LENGTH_LONG).show();
                    });
                }
            }
        }
    }

    @Override
    public void onDialogNegativeClick(NoticeDialogFragment dialog) {

    }
}
