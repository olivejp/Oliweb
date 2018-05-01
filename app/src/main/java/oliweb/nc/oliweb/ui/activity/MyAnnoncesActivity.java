package oliweb.nc.oliweb.ui.activity;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.List;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.ui.DialogInfos;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyAnnoncesViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceRawAdapter;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.helper.SharedPreferencesHelper;

import static oliweb.nc.oliweb.ui.activity.PostAnnonceActivity.BUNDLE_KEY_MODE;
import static oliweb.nc.oliweb.utility.Utility.DIALOG_FIREBASE_RETRIEVE;
import static oliweb.nc.oliweb.utility.Utility.sendNotificationToRetreiveData;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MyAnnoncesActivity extends AppCompatActivity implements NoticeDialogFragment.DialogListener {

    private static final String TAG = MyAnnoncesActivity.class.getName();

    private RecyclerView recyclerView;

    public static final String ARG_NOTICE_BUNDLE_ID_ANNONCE = "ARG_NOTICE_BUNDLE_ID_ANNONCE";
    public static final String DIALOG_TAG_DELETE = "DIALOG_TAG_DELETE";
    public static final String DIALOG_TAG_SYNC = "DIALOG_TAG_SYNC";
    public static final int REQUEST_STORAGE_PERMISSION_CODE = 5841;

    private String uidUser;

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
        uidUser = SharedPreferencesHelper.getInstance(this).getUidFirebaseUser();
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
        NoticeDialogFragment.sendDialog(getSupportFragmentManager(), dialogInfos, this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_annonces_activity, menu);
        return true;
    }

    @Override
    public void onDialogPositiveClick(NoticeDialogFragment dialog) {
        if (dialog.getTag() != null && dialog.getTag().equals(DIALOG_TAG_DELETE)
                && dialog.getBundle() != null && dialog.getBundle().containsKey(ARG_NOTICE_BUNDLE_ID_ANNONCE)) {
            long idAnnonce = dialog.getBundle().getLong(ARG_NOTICE_BUNDLE_ID_ANNONCE);
            if (idAnnonce != 0) {
                viewModel.deleteAnnonceById(idAnnonce)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(exception -> Log.e(TAG, "deleteAnnonceById.doOnError exception : " + exception.getLocalizedMessage(), exception))
                        .doOnSuccess(result -> {
                            Log.d(TAG, "deleteAnnonceById.doOnSuccess result : " + result);
                            if (result.get()) {
                                Snackbar.make(recyclerView, "Annonce supprimée", Snackbar.LENGTH_LONG).show();
                                if (NetworkReceiver.checkConnection(this)) {
                                    SyncService.launchSynchroForAll(getApplicationContext());
                                }
                            }
                        })
                        .subscribe();
            }
        }

        if (dialog.getTag() != null && dialog.getTag().equals(DIALOG_FIREBASE_RETRIEVE)) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION_CODE);
                } else {
                    callToSync();
                }
            } else {
                callToSync();
            }
        }
    }

    private void callToSync() {
        SyncService.launchSynchroFromFirebase(this, uidUser);
    }

    @Override
    public void onDialogNegativeClick(NoticeDialogFragment dialog) {
        dialog.dismiss();
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
        if (idItem == R.id.menu_fb_sync) {
            viewModel.shouldIAskQuestionToRetreiveData(uidUser).observe(this, atomicBoolean -> {
                if (atomicBoolean != null && atomicBoolean.get()) {
                    viewModel.shouldIAskQuestionToRetreiveData(null).removeObservers(this);
                    sendNotificationToRetreiveData(getSupportFragmentManager(), this);
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callToSync();
        }
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
