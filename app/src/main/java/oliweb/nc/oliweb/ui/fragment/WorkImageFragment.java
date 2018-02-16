package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.media.MediaUtility;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;

/**
 * Created by 2761oli on 12/02/2018.
 */

public class WorkImageFragment extends Fragment {

    @BindView(R.id.frag_work_image_photo)
    ImageView photo;

    private Bitmap pBitmap;
    private AppCompatActivity activity;
    private PostAnnonceActivityViewModel viewModel;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = (AppCompatActivity) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this.activity).get(PostAnnonceActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        if (this.activity.getSupportActionBar() != null) {
            this.activity.getSupportActionBar().hide();
        }
        View rootView = inflater.inflate(R.layout.fragment_work_image, container, false);
        ButterKnife.bind(this, rootView);
        pBitmap = MediaUtility.getBitmapFromUri(getContext(), Uri.parse(viewModel.getUpdatedPhoto().getUriLocal()));
        photo.setImageBitmap(pBitmap);
        return rootView;
    }

    @OnClick(R.id.frag_work_image_button_rotate_photo)
    public void onRotate(View v) {
        if (pBitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(pBitmap, pBitmap.getWidth(), pBitmap.getHeight(), true);
            pBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
            scaledBitmap.recycle();
            photo.setImageBitmap(pBitmap);
        }
    }

    @OnClick(R.id.frag_work_image_button_valid_photo)
    public void onValid(View v) {

    }

    @OnClick(R.id.frag_work_image_button_delete_photo)
    public void onDelete(View v) {
        viewModel.removePhotoToCurrentList(viewModel.getUpdatedPhoto());
        this.activity.getSupportFragmentManager().popBackStackImmediate();
    }
}