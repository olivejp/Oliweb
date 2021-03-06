package oliweb.nc.oliweb.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fotoapparat.Fotoapparat;
import io.fotoapparat.configuration.CameraConfiguration;
import io.fotoapparat.configuration.UpdateConfiguration;
import io.fotoapparat.log.LoggersKt;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.selector.FocusModeSelectorsKt;
import io.fotoapparat.selector.LensPositionSelectorsKt;
import io.fotoapparat.selector.ResolutionSelectorsKt;
import io.fotoapparat.selector.SelectorsKt;
import io.fotoapparat.view.CameraRenderer;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.activity.viewmodel.ShootingActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.ShootingAdapter;

import static io.fotoapparat.selector.FlashSelectorsKt.off;
import static io.fotoapparat.selector.FlashSelectorsKt.on;
import static io.fotoapparat.selector.LensPositionSelectorsKt.back;
import static io.fotoapparat.selector.LensPositionSelectorsKt.front;
import static oliweb.nc.oliweb.utility.Constants.REMOTE_NUMBER_PICTURES;

public class ShootingActivity extends AppCompatActivity {

    private static final String TAG = ShootingActivity.class.getCanonicalName();
    public static final String RESULT_DATA_LIST_PAIR = "RESULT_DATA_LIST_PAIR";
    public static final String EXTRA_NBR_PHOTO = "EXTRA_NBR_PHOTO";

    @BindView(R.id.camera_view)
    CameraRenderer cameraRenderer;

    @BindView(R.id.recycler_shooting_photos)
    RecyclerView recyclerView;

    @BindView(R.id.fab_flash)
    FloatingActionButton fabFlash;

    private Fotoapparat fotoapparat;
    private ShootingActivityViewModel viewModel;
    private ShootingAdapter shootingAdapter;
    private Long remoteNbrMaxPictures;

    private View.OnLongClickListener onLongClickPhotoListener = v -> {
        if (v.getTag() != null) {
            AlertDialog.Builder builder = viewModel.getMediaUtility().getBuilder(this);
            builder.setTitle(R.string.delete_photo)
                    .setMessage(R.string.delete_photo_are_you_sure)
                    .setPositiveButton(R.string.yes, (dialog, which) -> viewModel.removePhotoFromCurrentList((Uri) v.getTag()))
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                    })
                    .setIcon(R.drawable.ic_add_a_photo_black_48dp)
                    .show();
            return true;
        }
        return false;
    };

    @Override
    protected void onStart() {
        super.onStart();
        fotoapparat.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        fotoapparat.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Récupère le nombre de photos que je peux prendre
        Long nbrPhotoPlaceAvailable = getIntent().getLongExtra(EXTRA_NBR_PHOTO, 0);

        // Récupération du nombre maximale de photo autorisée
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteNbrMaxPictures = remoteConfig.getLong(REMOTE_NUMBER_PICTURES);

        setContentView(R.layout.activity_shooting);
        ButterKnife.bind(this);

        initFotoapparat();
        initRecylcerView();

        viewModel = ViewModelProviders.of(this).get(ShootingActivityViewModel.class);
        viewModel.setNbrShootAvailable(nbrPhotoPlaceAvailable);
        viewModel.getLiveListPairFileUri().observe(this, this::setAdapterListPairs);
        viewModel.getLiveFlashIsOn().observe(this, this::changeFlashIcon);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.shooting_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        if (item.getItemId() == R.id.menu_shooting_save) {
            Intent resultIntent = new Intent();
            resultIntent.putParcelableArrayListExtra(RESULT_DATA_LIST_PAIR, (ArrayList<? extends Parcelable>) viewModel.getListPairs());
            setResult(RESULT_OK, resultIntent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.fab_shoot)
    public void photoShoot(View v) {
        if (viewModel.isAbleToAddNewPicture()) {
            // Prise de la photo
            PhotoResult photoResult = fotoapparat.takePicture();

            // Recherche d'un nom de fichier
            Pair<Uri, File> pair = viewModel.generateNewPairUriFile();

            // Sauvegarde de notre photo, puis envoi à la liste courante des photos de l'annonce
            photoResult.saveToFile(pair.second).whenDone(unit -> viewModel.addPhotoToCurrentList(pair));
        } else {
            Toast.makeText(this, "Nombre maximal (" + remoteNbrMaxPictures + ") de photo atteint", Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.fab_switch)
    public void onSwitchClick(View v) {
        viewModel.setSwitchIsOn(!viewModel.isSwitchIsOn());
        fotoapparat.switchTo(viewModel.isSwitchIsOn() ? front() : back(), CameraConfiguration.builder().flash(viewModel.isFlashIsOn() ? on() : off()).build());
    }

    @OnClick(R.id.fab_flash)
    public void onFlashClick(View v) {
        viewModel.setFlashIsOn(!viewModel.isFlashIsOn());
        fotoapparat.updateConfiguration(UpdateConfiguration.builder().flash(viewModel.isFlashIsOn() ? on() : off()).build());
    }

    private void changeFlashIcon(AtomicBoolean atomicBoolean) {
        fabFlash.setImageResource(atomicBoolean.get() ? R.drawable.ic_flash_on_grey_900_48dp : R.drawable.ic_flash_off_grey_900_48dp);
    }

    private void initRecylcerView() {
        shootingAdapter = new ShootingAdapter(onLongClickPhotoListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(shootingAdapter);
    }

    private void setAdapterListPairs(List<Pair<Uri, File>> pairs) {
        shootingAdapter.setListPairs(pairs);
    }

    private void initFotoapparat() {
        fotoapparat = Fotoapparat
                .with(this)
                .into(cameraRenderer)
                .previewScaleType(ScaleType.CenterCrop)  // we want the preview to fill the view
                .photoResolution(ResolutionSelectorsKt.highestResolution())   // we want to have the biggest photo possible
                .lensPosition(LensPositionSelectorsKt.back())       // we want back camera
                .focusMode(SelectorsKt.firstAvailable(  // (optional) use the first focus mode which is supported by device
                        FocusModeSelectorsKt.continuousFocusPicture(),
                        FocusModeSelectorsKt.autoFocus(),        // in case if continuous focus is not available on device, auto focus will be used
                        FocusModeSelectorsKt.fixed()             // if even auto focus is not available - fixed focus mode will be used
                ))
                .logger(LoggersKt.loggers(            // (optional) we want to log camera events in 2 places at once
                        LoggersKt.logcat(),           // ... in logcat
                        LoggersKt.fileLogger(this)    // ... and to file
                ))
                .build();
    }
}
