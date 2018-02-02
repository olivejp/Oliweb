package oliweb.nc.oliweb.ui.dialog;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;


public class PhotoSourceDialog extends DialogFragment {

    private PhotoSourceListener listener;

    @BindView(R.id.dialog_button_gallery_photo)
    Button buttonGalery;
    @BindView(R.id.dialog_button_new_photo)
    Button buttonNewPhoto;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (PhotoSourceListener) context;
        } catch (ClassCastException e) {
            Log.e("ClassCastException", e.getMessage(), e);
            throw new ClassCastException(context.toString()
                    + " doit implementer l'interface PhotoSourceListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_photo_choice, container);
        ButterKnife.bind(this, view);
        buttonGalery.setOnClickListener(v -> listener.onGalleryClick());
        buttonNewPhoto.setOnClickListener(v -> listener.onNewPictureClick());
        return view;
    }

    /**
     * Interface que doit implémenter la classe appelante pour récupérer quel bouton a été appuyé.
     */
    public interface PhotoSourceListener {
        void onNewPictureClick();

        void onGalleryClick();
    }
}
