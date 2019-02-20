package oliweb.nc.oliweb.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = ListAnnonceBottomSheetDialog.class.getName();
    public static final String ANNONCE_FULL = "ANNONCE_FULL";

    private AnnonceFull annonceFull;

    private BottomSheetDialogListener listener;

    @BindView(R.id.bottom_sheet_annonce_image)
    ImageView imageView;

    @BindView(R.id.bottom_sheet_titre_annonce)
    TextView titreAnnonce;

    @BindView(R.id.share)
    TextView textShare;

    @BindView(R.id.favorite)
    TextView textFavorite;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (BottomSheetDialogListener) context;
        } catch (ClassCastException e) {
            Log.e(TAG, "ListAnnonceBottomSheetDialog doit être attacher à une implémentation de BottomSheetDialogListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.list_annonce_fragment_bottom_sheet, container, false);
        ButterKnife.bind(this, rootView);

        if (getArguments() != null && getArguments().containsKey(ANNONCE_FULL)) {
            annonceFull = (AnnonceFull) getArguments().get(ANNONCE_FULL);
        }

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

        if (listener != null) {
            textFavorite.setOnClickListener(view -> {
                this.listener.onFavoriteClick(annonceFull);
                ListAnnonceBottomSheetDialog.this.dismiss();
            });
            textShare.setOnClickListener(view -> {
                this.listener.onShareClick(annonceFull);
                ListAnnonceBottomSheetDialog.this.dismiss();
            });
        }

        return rootView;
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    public interface BottomSheetDialogListener {
        void onShareClick(AnnonceFull annonceFull);

        void onFavoriteClick(AnnonceFull annonceFull);
    }
}
