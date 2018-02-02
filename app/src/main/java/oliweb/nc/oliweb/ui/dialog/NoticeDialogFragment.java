package oliweb.nc.oliweb.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import oliweb.nc.oliweb.R;


public class NoticeDialogFragment extends DialogFragment {

    public static final String P_MESSAGE = "message";
    public static final String P_TYPE = "type";
    public static final String P_IMG = "image";
    public static final int TYPE_BOUTON_YESNO = 10;
    public static final int TYPE_BOUTON_OK = 20;
    public static final int TYPE_IMAGE_CAUTION = 100;
    public static final int TYPE_IMAGE_ERROR = 110;
    public static final int TYPE_IMAGE_INFORMATION = 120;

    // Use this instance of the interface to deliver action events
    private DialogListener mListener;
    private AppCompatActivity appCompatActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mListener = (DialogListener) context;
        } catch (ClassCastException e) {
            Log.e("ClassCastException", e.getMessage(), e);
            throw new ClassCastException(context.toString()
                    + " doit implementer l'interface DialogListener");
        }
        try {
            this.appCompatActivity = (AppCompatActivity) context;
        } catch (ClassCastException e) {
            Log.e("ClassCastException", e.getMessage(), e);
            throw new ClassCastException(context.toString()
                    + " doit étendre AppCompatActivity");
        }

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(appCompatActivity);

        LayoutInflater inflater = this.appCompatActivity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_layout, null);
        builder.setView(view);

        int typeBouton;
        int typeImage;
        TextView textview;

        if (getArguments() != null) {
            typeBouton = getArguments().getInt(P_TYPE);
            typeImage = getArguments().getInt(P_IMG);
            textview = view.findViewById(R.id.msgDialog);

            // Fenêtre de confirmation
            // On applique le message d'erreur
            textview.setText(getArguments().getString(P_MESSAGE));

            switch (typeBouton) {
                case TYPE_BOUTON_OK:
                    builder.setPositiveButton("Ok", (dialog, which) -> mListener.onDialogPositiveClick(NoticeDialogFragment.this));
                    break;
                case TYPE_BOUTON_YESNO:
                    builder.setPositiveButton("Oui", (dialog, which) -> mListener.onDialogPositiveClick(NoticeDialogFragment.this))
                            .setNegativeButton("Non", (dialog, which) -> mListener.onDialogNegativeClick(NoticeDialogFragment.this));
                    break;
            }

            ImageView imgView = view.findViewById(R.id.imageDialog);
            switch (typeImage) {
                case TYPE_IMAGE_CAUTION:
                    imgView.setImageResource(R.drawable.ic_warning_white_48dp);
                    break;
                case TYPE_IMAGE_ERROR:
                    imgView.setImageResource(R.drawable.ic_error_white_48dp);
                    break;
                case TYPE_IMAGE_INFORMATION:
                    imgView.setImageResource(R.drawable.ic_info_white_48dp);
                    break;
                default:
                    imgView.setImageResource(R.drawable.ic_info_white_48dp);
                    break;
            }
        }
        return builder.create();
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DialogListener {
        void onDialogPositiveClick(DialogFragment dialog);

        void onDialogNegativeClick(DialogFragment dialog);
    }
}
