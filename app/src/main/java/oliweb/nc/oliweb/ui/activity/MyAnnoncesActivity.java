package oliweb.nc.oliweb.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.ui.DialogInfos;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyAnnoncesViewModel;
import oliweb.nc.oliweb.ui.adapter.AnnonceRawAdapter;
import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;
import oliweb.nc.oliweb.utility.ArgumentsChecker;
import oliweb.nc.oliweb.utility.Constants;

import static oliweb.nc.oliweb.ui.activity.PostAnnonceActivity.BUNDLE_KEY_MODE;
import static oliweb.nc.oliweb.ui.activity.PostAnnonceActivity.BUNDLE_UID_USER;
import static oliweb.nc.oliweb.utility.Utility.DIALOG_FIREBASE_RETRIEVE;
import static oliweb.nc.oliweb.utility.Utility.sendNotificationToRetrieveData;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MyAnnoncesActivity extends AppCompatActivity implements NoticeDialogFragment.DialogListener {

    private static final String TAG = MyAnnoncesActivity.class.getCanonicalName();

    public static final String ARG_NOTICE_BUNDLE_ID_ANNONCE = "ARG_NOTICE_BUNDLE_ID_ANNONCE";
    public static final String ARG_UID_USER = "ARG_UID_USER";
    public static final String DIALOG_TAG_DELETE = "DIALOG_TAG_DELETE";
    public static final int REQUEST_STORAGE_PERMISSION_CODE = 5841;
    public static final int REQUEST_CODE_POST = 548;
    public static final String ACTION_CODE_POST = "ACTION_CODE_POST";

    private String uidUser;
    private MyAnnoncesViewModel viewModel;

    private View.OnClickListener onPopupClickListener = v -> {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(item -> {
            int i = item.getItemId();
            if (i == R.id.annonce_delete) {
                askToDelete((AnnoncePhotos) v.getTag());
                return true;
            } else if (i == R.id.annonce_share) {
                viewModel.shareAnnonce(MyAnnoncesActivity.this, (AnnoncePhotos) v.getTag(), uidUser);
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
        Bundle args = (savedInstanceState != null) ? savedInstanceState : getIntent().getExtras();
        ArgumentsChecker argumentsChecker = new ArgumentsChecker();
        argumentsChecker
                .setArguments(args)
                .isMandatory(ARG_UID_USER)
                .setOnFailureListener(e -> finish())
                .setOnSuccessListener(this::initActivity)
                .check();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_annonces_activity, menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_UID_USER, uidUser);
    }

    @Override
    public void onDialogPositiveClick(NoticeDialogFragment dialog) {
        if (dialog.getTag() == null) {
            return;
        }
        if (DIALOG_TAG_DELETE.equals(dialog.getTag())) {
            dialogPositiveDelete(dialog);
        } else if (DIALOG_FIREBASE_RETRIEVE.equals(dialog.getTag())) {
            dialogPositiveRetrieve();
        }
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
        if (idItem == R.id.menu_synchronyze) {
            SyncService.launchSynchroForUser(getApplicationContext(), uidUser);
            viewModel.shouldIAskQuestionToRetreiveData(uidUser).observeOnce(atomicBoolean -> {
                if (atomicBoolean != null && atomicBoolean.get()) {
                    sendNotificationToRetrieveData(getSupportFragmentManager(), this, getString(R.string.ads_found_on_network));
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
            callForFirebaseSync();
        }
    }

    private void initActivity(Bundle args) {
        uidUser = args.getString(ARG_UID_USER);
        viewModel = ViewModelProviders.of(this).get(MyAnnoncesViewModel.class);
        viewModel.findAnnonceByUidUserAndPhotos(uidUser).observe(this, annonceWithPhotos -> {
            if (annonceWithPhotos == null || annonceWithPhotos.isEmpty()) {
                initEmptyLayout();
            } else {
                initLayout(annonceWithPhotos);
            }
        });

        // On redirige tout de suite vers PostAnnonceActivity.
        if (getIntent().getAction() != null && getIntent().getAction().equals(ACTION_CODE_POST)) {
            callPostAnnonceCreate(null);
        }
    }

    private void dialogPositiveDelete(NoticeDialogFragment dialog) {
        if (dialog.getBundle() != null && dialog.getBundle().containsKey(ARG_NOTICE_BUNDLE_ID_ANNONCE)) {
            long idAnnonce = dialog.getBundle().getLong(ARG_NOTICE_BUNDLE_ID_ANNONCE);
            if (idAnnonce != 0) {
                viewModel.markToDelete(idAnnonce).observeOnce(atomicBoolean ->
                        Log.d(TAG, "Annonce successfully mark as to Delete")
                );
            }
        }
    }

    private void dialogPositiveRetrieve() {
        if (checkPermissionForMversion()) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION_CODE);
        } else {
            callForFirebaseSync();
        }
    }

    private boolean checkPermissionForMversion() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && viewModel.getMediaUtility().isExternalStorageAvailable()
                && !viewModel.getMediaUtility().allPermissionsAreGranted(getApplicationContext(), Collections.singletonList(Manifest.permission.WRITE_EXTERNAL_STORAGE));
    }

    private void initEmptyLayout() {
        setContentView(R.layout.empty_recyclerview);
        FloatingActionButton fab = findViewById(R.id.fab_empty_add);
        fab.setOnClickListener(this::callPostAnnonceCreate);
    }

    private void initLayout(List<AnnoncePhotos> annonceWithPhotos) {
        setContentView(R.layout.activity_my_annonces);
        RecyclerView recyclerView = findViewById(R.id.recycler_annonces);

        FloatingActionButton fabPostAnnonce = findViewById(R.id.fab_post_annonce);
        fabPostAnnonce.setOnClickListener(this::callPostAnnonceCreate);

        AnnonceRawAdapter annonceRawAdapter = new AnnonceRawAdapter(this,
                v -> {
                    AnnoncePhotos annoncePhotos = (AnnoncePhotos) v.getTag();
                    callActivityToUpdateAnnonce(annoncePhotos.getAnnonceEntity());
                }, onPopupClickListener);
        recyclerView.setAdapter(annonceRawAdapter);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(itemDecoration);

        annonceRawAdapter.setListAnnonces(annonceWithPhotos);
    }

    public void askToDelete(AnnoncePhotos annoncePhotos) {
        Bundle bundle = new Bundle();
        bundle.putLong(ARG_NOTICE_BUNDLE_ID_ANNONCE, annoncePhotos.getAnnonceEntity().getId());
        DialogInfos dialogInfos = new DialogInfos();
        dialogInfos.setMessage(String.format(getString(R.string.confirm_delete_annonce), annoncePhotos.getAnnonceEntity().getTitre()))
                .setButtonType(NoticeDialogFragment.TYPE_BOUTON_YESNO)
                .setIdDrawable(R.drawable.ic_delete_grey_900_24dp)
                .setTag(DIALOG_TAG_DELETE)
                .setBundlePar(bundle);
        NoticeDialogFragment.sendDialog(getSupportFragmentManager(), dialogInfos, this);
    }

    private void callForFirebaseSync() {
        SyncService.launchSynchroFromFirebase(this, uidUser);
    }

    private void callPostAnnonceCreate(@Nullable View v) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_MODE, Constants.PARAM_CRE);
        bundle.putString(BUNDLE_UID_USER, uidUser);
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
        bundle.putString(PostAnnonceActivity.BUNDLE_UID_USER, uidUser);
        bundle.putLong(PostAnnonceActivity.BUNDLE_KEY_ID_ANNONCE, annonce.getId());
        intent.putExtras(bundle);
        startActivityForResult(intent, REQUEST_CODE_POST);
        overridePendingTransition(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
    }
}
