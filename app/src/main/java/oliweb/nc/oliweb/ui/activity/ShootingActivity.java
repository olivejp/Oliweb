package oliweb.nc.oliweb.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.io.File;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fotoapparat.Fotoapparat;
import io.fotoapparat.log.LoggersKt;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.selector.FlashSelectorsKt;
import io.fotoapparat.selector.FocusModeSelectorsKt;
import io.fotoapparat.selector.LensPositionSelectorsKt;
import io.fotoapparat.selector.ResolutionSelectorsKt;
import io.fotoapparat.selector.SelectorsKt;
import io.fotoapparat.view.CameraRenderer;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.activity.viewmodel.ShootingActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.ShootingAdapter;

public class ShootingActivity extends AppCompatActivity {

    private static final String TAG = ShootingActivity.class.getCanonicalName();
    public static final String RESULT_DATA_LIST_PAIR = "RESULT_DATA_LIST_PAIR";

    @BindView(R.id.camera_view)
    CameraRenderer cameraRenderer;

    @BindView(R.id.recycler_shooting_photos)
    RecyclerView recyclerView;

    private Fotoapparat fotoapparat;
    private ShootingActivityViewModel viewModel;
    private ShootingAdapter shootingAdapter;

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
        viewModel = ViewModelProviders.of(this).get(ShootingActivityViewModel.class);
        setContentView(R.layout.activity_shooting);
        ButterKnife.bind(this);
        initFotoapparat();
        initRecylcerView();
        viewModel.getLiveListPairFileUri().observe(this, this::setAdapterListPairs);
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
            resultIntent.putParcelableArrayListExtra(RESULT_DATA_LIST_PAIR, viewModel.getListPairs());
            setResult(RESULT_OK, resultIntent);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initRecylcerView() {
        shootingAdapter = new ShootingAdapter();
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
                .flash(SelectorsKt.firstAvailable(      // (optional) similar to how it is done for focus mode, this time for flash
                        FlashSelectorsKt.autoRedEye(),
                        FlashSelectorsKt.autoFlash(),
                        FlashSelectorsKt.torch()
                ))
                .logger(LoggersKt.loggers(            // (optional) we want to log camera events in 2 places at once
                        LoggersKt.logcat(),           // ... in logcat
                        LoggersKt.fileLogger(this)    // ... and to file
                ))
                .build();
    }

    @OnClick(R.id.fab_shoot)
    public void photoShoot(View v) {
        // Prise de la photo
        PhotoResult photoResult = fotoapparat.takePicture();

        // Recherche d'un nom de fichier
        Pair<Uri, File> pair = viewModel.generateNewPair_UriFile();

        // Sauvegarde de notre photo, puis envoi à la liste courante des photos de l'annonce
        photoResult.saveToFile(pair.second).whenDone(unit -> viewModel.addPhotoToCurrentList(pair));
    }
}
