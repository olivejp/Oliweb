package oliweb.nc.oliweb.ui.activity;

import android.os.Bundle;
import android.view.View;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fotoapparat.Fotoapparat;
import io.fotoapparat.log.LoggersKt;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.result.BitmapPhoto;
import io.fotoapparat.result.PhotoResult;
import io.fotoapparat.selector.FlashSelectorsKt;
import io.fotoapparat.selector.FocusModeSelectorsKt;
import io.fotoapparat.selector.LensPositionSelectorsKt;
import io.fotoapparat.selector.ResolutionSelectorsKt;
import io.fotoapparat.selector.SelectorsKt;
import io.fotoapparat.view.CameraRenderer;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.activity.viewmodel.ShootingActivityViewModel;

public class ShootingActivity extends AppCompatActivity {

    @BindView(R.id.camera_view)
    CameraRenderer cameraRenderer;

    private Fotoapparat fotoapparat;
    private ShootingActivityViewModel viewModel;

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
        File file = viewModel.generateNewFile();

        // Sauvegarde de notre photo
        photoResult.saveToFile(file);
        photoResult.toBitmap().whenDone(this::addToList);
    }

    private void addToList(BitmapPhoto bitmapPhoto) {

    }
}
