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
import android.widget.LinearLayout;

import com.google.firebase.auth.FirebaseAuth;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.DialogInfos;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.AnnonceRepository;
import oliweb.nc.oliweb.helper.RecyclerItemTouchHelper;
import oliweb.nc.oliweb.helper.SharedPreferencesHelper;
import oliweb.nc.oliweb.network.NetworkReceiver;
import oliweb.nc.oliweb.service.SyncService;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyAnnoncesViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceAdapter;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;
import oliweb.nc.oliweb.utility.Utility;

import static oliweb.nc.oliweb.ui.activity.PostAnnonceActivity.BUNDLE_KEY_MODE;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MyAnnoncesActivity extends AppCompatActivity implements RecyclerItemTouchHelper.SwipeListener, NoticeDialogFragment.DialogListener {

    private static final String TAG = MyAnnoncesActivity.class.getName();

    @BindView(R.id.recycler_annonces)
    RecyclerView recyclerView;

    @BindView(R.id.empty_linear)
    LinearLayout linearLayout;

    public static final String ARG_NOTICE_BUNDLE_ID_ANNONCE = "ARG_NOTICE_BUNDLE_ID_ANNONCE";
    public static final String ARG_NOTICE_BUNDLE_POSITION = "ARG_NOTICE_BUNDLE_POSITION";
    public static final String DIALOG_TAG_DELETE = "DIALOG_TAG_DELETE";

    public static final int REQUEST_CODE_POST = 548;

    private MyAnnoncesViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewModel = ViewModelProviders.of(this).get(MyAnnoncesViewModel.class);

        setContentView(R.layout.activity_my_annonces);
        ButterKnife.bind(this);

        // Récupération du UID de l'utilisateur connecté.
        String uidUser = SharedPreferencesHelper.getInstance(this).getUidFirebaseUser();
        if (uidUser == null || uidUser.isEmpty()) {
            Log.e(TAG, "Missing mandatory parameter");
            finish();
        }

        // TODO modifier les deux listeners à la fin. Dans le cas de nos propres annonces, on ne doit pas pouvoir mettre en favoris Mais on doit pouvoir partager une annonce.
        AnnonceAdapter annonceAdapter = new AnnonceAdapter(AnnonceAdapter.DisplayType.RAW, v -> {
            AnnonceEntity annonce = (AnnonceEntity) v.getTag();
            callActivityToUpdateAnnonce(annonce);
        }, null, null);
        recyclerView.setAdapter(annonceAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);

        // Ajout d'un swipe listener pour pouvoir supprimer l'annonce
        RecyclerItemTouchHelper recyclerItemTouchHelper = new RecyclerItemTouchHelper(0, ItemTouchHelper.LEFT, this);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(recyclerItemTouchHelper);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        viewModel.findActiveAnnonceByUidUtilisateur(uidUser)
                .observe(this, annonceWithPhotos -> {
                    if (annonceWithPhotos == null || annonceWithPhotos.isEmpty()) {
                        linearLayout.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        linearLayout.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        annonceAdapter.setListAnnonces(annonceWithPhotos);
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (NetworkReceiver.checkConnection(this) && requestCode == REQUEST_CODE_POST && resultCode == RESULT_OK) {
            SyncService.launchSynchroForAll(getApplicationContext());
        }
    }

    /**
     * This method is only available for AnnonceAdapter in Raw mode.
     * In Beauty mode we can't swipe to delete element.
     *
     * @param view
     * @param direction
     */
    @Override
    public void onSwipe(RecyclerView.ViewHolder view, int direction) {
        try {
            AnnonceAdapter.ViewHolderRaw viewHolderRaw = (AnnonceAdapter.ViewHolderRaw) view;
            AnnonceEntity annonce = viewHolderRaw.getSingleAnnonce();

            // Création d'un bundle dans lequel on va passer nos items
            Bundle bundle = new Bundle();
            bundle.putLong(ARG_NOTICE_BUNDLE_ID_ANNONCE, annonce.getIdAnnonce());
            bundle.putInt(ARG_NOTICE_BUNDLE_POSITION, viewHolderRaw.getAdapterPosition());

            if (direction == ItemTouchHelper.LEFT) {
                DialogInfos dialogInfos = new DialogInfos();
                dialogInfos.setMessage(String.format("Supprimer l'annonce %s ?%n%nLe numéro de suivi ainsi que toutes ses étapes seront perdues.", annonce.getTitre()))
                        .setButtonType(NoticeDialogFragment.TYPE_BOUTON_YESNO)
                        .setIdDrawable(R.drawable.ic_delete_grey_900_24dp)
                        .setTag(DIALOG_TAG_DELETE)
                        .setBundlePar(bundle);

                NoticeDialogFragment.sendDialog(getSupportFragmentManager(), dialogInfos);
            }
        } catch (ClassCastException e) {
            Log.e(TAG, "Cette méthode n'est applicable que pour le mode Display Raw et devrait donc contenir un AnnonceAdapter.ViewHolderRaw");
        }
    }

    @Override
    public void onDialogPositiveClick(NoticeDialogFragment dialog) {
        if (dialog.getTag() != null && dialog.getTag().equals(DIALOG_TAG_DELETE)
                && dialog.getBundle() != null && dialog.getBundle().containsKey(ARG_NOTICE_BUNDLE_ID_ANNONCE)) {
            long idAnnonce = dialog.getBundle().getLong(ARG_NOTICE_BUNDLE_ID_ANNONCE);
            if (idAnnonce != 0) {
                viewModel.deleteAnnonceById(idAnnonce, dataReturn -> {
                    if (dataReturn.getNb() > 0) {
                        Snackbar.make(recyclerView, "Annonce supprimée", Snackbar.LENGTH_LONG).show();
                        SyncService.launchSynchroForAll(getApplicationContext());
                    }
                });
            }
        }
    }

    @Override
    public void onDialogNegativeClick(NoticeDialogFragment dialog) {
        // Do nothing
    }

    @OnClick(R.id.fab_post_annonce)
    public void callPostAnnonceCreate(View v) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_MODE, Constants.PARAM_CRE);
        intent.putExtras(bundle);
        intent.setClass(this, PostAnnonceActivity.class);
        startActivityForResult(intent, REQUEST_CODE_POST);
    }

    // TODO supprimer cette méthode une fois les tests terminés
    @OnClick(R.id.fab_post_mass)
    public void insertMassAnnonce(View v) {
        for (int i = 0; i < 25; i++) {
            AnnonceEntity annonce = new AnnonceEntity();
            annonce.setUUID(UUID.randomUUID().toString());
            annonce.setTitre("Titre " + i);
            annonce.setDescription("Description " + i);
            annonce.setPrix(i * 3);
            annonce.setDatePublication(Utility.getNowInEntityFormat());
            annonce.setIdCategorie(1L);
            annonce.setStatut(StatusRemote.TO_SEND);
            annonce.setFavorite(0);
            annonce.setUuidUtilisateur(FirebaseAuth.getInstance().getUid());
            AnnonceRepository.getInstance(this).save(annonce, dataReturn -> {
                if (dataReturn.isSuccessful()) {
                    Log.d(TAG, "Insertion test réussi");
                }
            });
        }
    }

    private void callActivityToUpdateAnnonce(AnnonceEntity annonce) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        intent.setClass(this, PostAnnonceActivity.class);
        bundle.putString(PostAnnonceActivity.BUNDLE_KEY_MODE, Constants.PARAM_MAJ);
        bundle.putLong(PostAnnonceActivity.BUNDLE_KEY_ID_ANNONCE, annonce.getIdAnnonce());
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_CODE_POST);
    }
}
