package oliweb.nc.oliweb.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.ui.glide.GlideApp;

public class ListAnnonceBottomSheetDialog extends BottomSheetDialogFragment {

    private AnnonceFull annonceFull;

    @BindView(R.id.bottom_sheet_annonce_image)
    ImageView imageView;

    @BindView(R.id.bottom_sheet_titre_annonce)
    TextView titreAnnonce;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.list_annonce_fragment_bottom_sheet, container, false);
        ButterKnife.bind(this, rootView);

        if (annonceFull != null) {
            if (annonceFull.getPhotos() != null && !annonceFull.getPhotos().isEmpty() && annonceFull.getPhotos().get(0) != null && annonceFull.getPhotos().get(0).getFirebasePath() != null && !annonceFull.getPhotos().get(0).getFirebasePath().isEmpty()) {
                GlideApp.with(rootView)
                        .load(this.annonceFull.getPhotos().get(0).getFirebasePath())
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .error(R.mipmap.ic_banana_launcher_foreground)
                        .into(imageView);
            }
            titreAnnonce.setText(annonceFull.getAnnonce().getTitre());
        }

        return rootView;
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    public void setAnnonceFull(AnnonceFull annonceFull) {
        this.annonceFull = annonceFull;
    }
}
