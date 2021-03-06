package oliweb.nc.oliweb.ui.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.ui.activity.viewmodel.PostAnnonceActivityViewModel;
import oliweb.nc.oliweb.ui.glide.GlideApp;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by 2761oli on 12/02/2018.
 */

public class WorkImageFragment extends Fragment {

    private static final String TAG = WorkImageFragment.class.getCanonicalName();

    @BindView(R.id.frag_work_image_photo)
    ImageViewTouch photo;

    private Bitmap pBitmap;
    private AppCompatActivity appCompatActivity;
    private PostAnnonceActivityViewModel viewModel;
    private boolean hasBeenUpdated = false;
    private Matrix matrix;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.appCompatActivity = (AppCompatActivity) context;
        matrix = new Matrix();
        matrix.postRotate(-90);
        if (this.appCompatActivity.getSupportActionBar() != null) {
            this.appCompatActivity.getSupportActionBar().hide();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (this.appCompatActivity.getSupportActionBar() != null) {
            this.appCompatActivity.getSupportActionBar().show();
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(this.appCompatActivity).get(PostAnnonceActivityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_work_image, container, false);
        ButterKnife.bind(this, rootView);

        Uri uriBitmap = Uri.parse(viewModel.getUpdatedPhoto().getUriLocal());

        // Récupération de bitmap d'origine
        Single<Bitmap> singleBitmap = Single.create(e -> {
                    try {
                        Bitmap bitmap = GlideApp.with(appCompatActivity)
                                .asBitmap()
                                .load(uriBitmap)
                                .submit()
                                .get();
                        e.onSuccess(bitmap);
                    } catch (Exception e1) {
                        e.onError(e1);
                    }
                }
        );

        singleBitmap.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(this::displayBitmap)
                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage(), throwable))
                .subscribe();

        Utility.hideKeyboard(this.appCompatActivity);
        return rootView;
    }

    @OnClick(R.id.frag_work_image_button_rotate_photo)
    public void onClickRotate(View v) {
        if (pBitmap != null) {
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(pBitmap, pBitmap.getWidth(), pBitmap.getHeight(), true);
            pBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
            scaledBitmap.recycle();
            displayBitmap(pBitmap);
            hasBeenUpdated = true;
        }
    }

    @OnClick(R.id.frag_work_image_button_valid_photo)
    public void onClickValid(View v) {
        if (hasBeenUpdated) {

            // Création d'une nouvelle URI sur le device pour stocker la photo
            Pair<Uri, File> pair = viewModel.getMediaUtility().createNewImagePairUriFile(appCompatActivity);

            if (viewModel.getMediaUtility().saveBitmapToFileProviderUri(appCompatActivity.getContentResolver(), pBitmap, pair.first)) {
                // Suppression de l'ancienne photo du device
                viewModel.getMediaUtility().deletePhotoFromDevice(appCompatActivity.getContentResolver(), viewModel.getUpdatedPhoto().getUriLocal());

                // Mise à jour de la nouvelle photo
                viewModel.getUpdatedPhoto().setUriLocal(pair.first.toString());
                viewModel.getUpdatedPhoto().setStatut(StatusRemote.TO_SEND);
            }
            this.viewModel.updatePhotos();
        }
        this.appCompatActivity.getSupportFragmentManager().popBackStackImmediate();
    }

    @OnClick(R.id.frag_work_image_button_delete_photo)
    public void onClickDelete(View v) {
        viewModel.removePhotoFromCurrentList(viewModel.getUpdatedPhoto());
        this.appCompatActivity.getSupportFragmentManager().popBackStackImmediate();
    }

    private void displayBitmap(Bitmap bitmap) {
        pBitmap = bitmap;
        GlideApp.with(appCompatActivity)
                .load(pBitmap)
                .fitCenter()
                .into(photo);
    }
}
