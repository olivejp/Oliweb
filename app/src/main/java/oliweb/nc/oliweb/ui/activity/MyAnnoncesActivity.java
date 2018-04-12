package oliweb.nc.oliweb.ui.activity;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.DialogInfos;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.helper.SharedPreferencesHelper;
import oliweb.nc.oliweb.service.SyncService;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyAnnoncesViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceRawAdapter;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;

import static oliweb.nc.oliweb.ui.activity.PostAnnonceActivity.BUNDLE_KEY_MODE;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MyAnnoncesActivity extends AppCompatActivity implements NoticeDialogFragment.DialogListener {

    private static final String TAG = MyAnnoncesActivity.class.getName();

    private RecyclerView recyclerView;

    public static final String ARG_NOTICE_BUNDLE_ID_ANNONCE = "ARG_NOTICE_BUNDLE_ID_ANNONCE";
    public static final String DIALOG_TAG_DELETE = "DIALOG_TAG_DELETE";

    public static final int REQUEST_CODE_POST = 548;

    private MyAnnoncesViewModel viewModel;

    /**
     * OnClickListener qui ouvrira le popup
     */
    private View.OnClickListener onPopupClickListener = v -> {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(item -> {
            int i = item.getItemId();
            if (i == R.id.annonce_delete) {
                askToDelete((AnnoncePhotos) v.getTag());
                return true;
            } else {
                return false;
            }
        });
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.annonce_popup_menu, popup.getMenu());
        popup.show();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Récupération du UID de l'utilisateur connecté.
        String uidUser = SharedPreferencesHelper.getInstance(this).getUidFirebaseUser();
        if (uidUser == null || uidUser.isEmpty()) {
            Log.e(TAG, "Missing mandatory parameter");
            finish();
        }

        viewModel = ViewModelProviders.of(this).get(MyAnnoncesViewModel.class);

        viewModel.findActiveAnnonceByUidUtilisateur(uidUser)
                .observe(this, annonceWithPhotos -> {
                    if (annonceWithPhotos == null || annonceWithPhotos.isEmpty()) {
                        initEmptyLayout();
                    } else {
                        initLayout(annonceWithPhotos);
                    }
                });
    }

    private void initEmptyLayout() {
        setContentView(R.layout.empty_recyclerview);
        TextView textEmpty = findViewById(R.id.text_empty);
        FloatingActionButton fab = findViewById(R.id.fab_empty_add);
        fab.setOnClickListener(this::callPostAnnonceCreate);
        textEmpty.setText("Vous n'avez encore posté aucune annonce.\nAppuyez sur le + pour saisir une annonce.");
    }

    private void initLayout(List<AnnoncePhotos> annonceWithPhotos) {
        setContentView(R.layout.activity_my_annonces);
        recyclerView = findViewById(R.id.recycler_annonces);

        FloatingActionButton fabPostAnnonce = findViewById(R.id.fab_post_annonce);
        fabPostAnnonce.setOnClickListener(this::callPostAnnonceCreate);

        AnnonceRawAdapter annonceRawAdapter = new AnnonceRawAdapter(v -> {
            AnnoncePhotos annoncePhotos = (AnnoncePhotos) v.getTag();
            callActivityToUpdateAnnonce(annoncePhotos.getAnnonceEntity());
        }, onPopupClickListener);
        recyclerView.setAdapter(annonceRawAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);

        annonceRawAdapter.setListAnnonces(annonceWithPhotos);
    }

    /**
     * Ask to delete an annonce
     *
     * @param annoncePhotos
     */
    public void askToDelete(AnnoncePhotos annoncePhotos) {
        // Création d'un bundle dans lequel on va passer nos items
        Bundle bundle = new Bundle();
        bundle.putLong(ARG_NOTICE_BUNDLE_ID_ANNONCE, annoncePhotos.getAnnonceEntity().getIdAnnonce());
        DialogInfos dialogInfos = new DialogInfos();
        dialogInfos.setMessage(String.format("Supprimer l'annonce %s ?%n%nVous perdrez tous les informations relatives à cette annonce (Chats, Messages).", annoncePhotos.getAnnonceEntity().getTitre()))
                .setButtonType(NoticeDialogFragment.TYPE_BOUTON_YESNO)
                .setIdDrawable(R.drawable.ic_delete_grey_900_24dp)
                .setTag(DIALOG_TAG_DELETE)
                .setBundlePar(bundle);
        NoticeDialogFragment.sendDialog(getSupportFragmentManager(), dialogInfos);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int idItem = item.getItemId();
        if (idItem == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void callPostAnnonceCreate(View v) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_MODE, Constants.PARAM_CRE);
        intent.putExtras(bundle);
        intent.setClass(this, PostAnnonceActivity.class);
        startActivityForResult(intent, REQUEST_CODE_POST);
        overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }

    private void callActivityToUpdateAnnonce(AnnonceEntity annonce) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        intent.setClass(this, PostAnnonceActivity.class);
        bundle.putString(PostAnnonceActivity.BUNDLE_KEY_MODE, Constants.PARAM_MAJ);
        bundle.putLong(PostAnnonceActivity.BUNDLE_KEY_ID_ANNONCE, annonce.getIdAnnonce());
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_CODE_POST);
        overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }
}
