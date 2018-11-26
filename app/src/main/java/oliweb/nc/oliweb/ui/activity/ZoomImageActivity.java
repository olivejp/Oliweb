package oliweb.nc.oliweb.ui.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.glide.GlideApp;

public class ZoomImageActivity extends AppCompatActivity {
    public static final String ZOOM_IMAGE_ACTIVITY_ARG_URI_IMAGE = "ZOOM_IMAGE_ACTIVITY_ARG_URI_IMAGE";
    private String uriImage;

    @BindView(R.id.image_zoom)
    ImageViewTouch imageZoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Récupération des paramètres
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            uriImage = extras.getString(ZOOM_IMAGE_ACTIVITY_ARG_URI_IMAGE);
        }

        // Instanciation de la vue
        setContentView(R.layout.activity_zoom_image);
        ButterKnife.bind(this);

        // Chargement de l'image
        GlideApp.with(this)
                .load(uriImage)
                .error(R.drawable.ic_error_grey_900_48dp)
                .fitCenter()
                .into(imageZoom);
    }

    @OnClick(R.id.fab_close)
    public void close(View view) {
        finish();
    }
}
