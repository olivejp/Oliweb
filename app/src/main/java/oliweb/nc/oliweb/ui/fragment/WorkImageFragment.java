package oliweb.nc.oliweb.ui.fragment;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.media.MediaUtility;

/**
 * Created by 2761oli on 12/02/2018.
 */

public class WorkImageFragment extends Fragment {

    public static final String BUNDLE_URI = "BUNDLE_URI";

    @BindView(R.id.frag_work_image_photo)
    ImageView photo;

    @BindView(R.id.frag_work_image_button_delete_photo)
    ImageButton buttonDelete;

    @BindView(R.id.frag_work_image_button_valid_photo)
    ImageButton buttonValid;

    private Uri uriPhoto;
    private Bitmap pBitmap;

    public static WorkImageFragment newInstance(Uri uriPhoto) {
        WorkImageFragment fragment = new WorkImageFragment();
        Bundle args = new Bundle();
        args.putParcelable(BUNDLE_URI, uriPhoto);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(BUNDLE_URI)) {
            this.uriPhoto = getArguments().getParcelable(BUNDLE_URI);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_work_image, container, false);
        ButterKnife.bind(this, rootView);
        pBitmap = MediaUtility.getBitmapFromUri(getContext(), uriPhoto);
        photo.setImageBitmap(pBitmap);
        return rootView;
    }

    @OnClick(R.id.frag_work_image_button_rotate_photo)
    public void rotateImage(View v) {
        if (pBitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(pBitmap, pBitmap.getWidth(), pBitmap.getHeight(), true);
            pBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
            scaledBitmap.recycle();
            photo.setImageBitmap(pBitmap);
        }
    }

    @OnClick(R.id.frag_work_image_button_delete_photo)
    public void deleteImage(View v) {
        // TODO : Do something here to delete the Image.
    }

    @OnClick(R.id.frag_work_image_button_valid_photo)
    public void validateImage(View v) {
        // TODO : Do something here to delete the Image.
    }
}
