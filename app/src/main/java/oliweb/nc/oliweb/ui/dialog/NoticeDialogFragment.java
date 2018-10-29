package oliweb.nc.oliweb.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.ui.DialogInfos;


public class NoticeDialogFragment extends AppCompatDialogFragment {

    private static final String TAG = NoticeDialogFragment.class.getCanonicalName();

    public static final String P_MESSAGE = "message";
    public static final String P_TYPE = "type";
    public static final String P_IMG = "image";
    public static final String P_BUNDLE = "bundle";

    public static final int TYPE_BOUTON_YESNO = 10;
    public static final int TYPE_BOUTON_OK = 20;

    // Use this instance of the interface to deliver action events
    private Bundle mBundle;
    private DialogListener mListener;
    private AppCompatActivity appCompatActivity;
    private AlertDialog mDialog;

    public void setListener(DialogListener listener) {
        mListener = listener;
    }

    /**
     * @param fragmentManager Get from the context
     * @param message         The message to be send
     * @param buttonType      Can be TYPE_BOUTON_OK or TYPE_BOUTON_YESNO
     * @param idDrawable      Drawable that will be show in top of the window
     * @param tag             A text to be a tag
     */
    public static void sendDialogByFragmentManagerWithRes(FragmentManager fragmentManager, String message, int buttonType, @DrawableRes int idDrawable, @Nullable String tag, @Nullable Bundle bundlePar, @Nullable NoticeDialogFragment.DialogListener listener) {
        NoticeDialogFragment dialogErreur = new NoticeDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NoticeDialogFragment.P_MESSAGE, message);
        bundle.putInt(NoticeDialogFragment.P_TYPE, buttonType);
        bundle.putInt(NoticeDialogFragment.P_IMG, idDrawable);
        bundle.putBundle(NoticeDialogFragment.P_BUNDLE, bundlePar);
        dialogErreur.setListener(listener);
        dialogErreur.setArguments(bundle);
        dialogErreur.show(fragmentManager, tag);
    }

    public static void sendDialog(FragmentManager fragmentManager, DialogInfos dialogInfos, DialogListener listener) {
        NoticeDialogFragment dialogErreur = new NoticeDialogFragment();
        dialogErreur.setListener(listener);
        Bundle bundle = new Bundle();
        bundle.putString(NoticeDialogFragment.P_MESSAGE, dialogInfos.getMessage());
        bundle.putInt(NoticeDialogFragment.P_TYPE, dialogInfos.getButtonType());
        bundle.putInt(NoticeDialogFragment.P_IMG, dialogInfos.getIdDrawable());
        bundle.putBundle(NoticeDialogFragment.P_BUNDLE, dialogInfos.getBundlePar());
        dialogErreur.setArguments(bundle);
        dialogErreur.setCancelable(false);
        dialogErreur.show(fragmentManager, dialogInfos.getTag());
    }

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

        int typeBouton = 0;
        int idResource = 0;
        TextView textview;

        // Récupération des arguments
        if (getArguments() != null) {
            if (getArguments().containsKey(P_TYPE)) {
                typeBouton = getArguments().getInt(P_TYPE);
            }
            if (getArguments().containsKey(P_IMG)) {
                idResource = getArguments().getInt(P_IMG);
            }
            if (getArguments().containsKey(P_MESSAGE)) {
                textview = view.findViewById(R.id.msgDialog);
                textview.setText(getArguments().getString(P_MESSAGE));
            }
            if (getArguments().containsKey(P_BUNDLE)) {
                mBundle = getArguments().getBundle(P_BUNDLE);
            }

            textview = view.findViewById(R.id.msgDialog);

            // Fenêtre de confirmation
            // On applique le message d'erreur
            textview.setText(getArguments().getString(P_MESSAGE));

            // Récupération du bon type de bouton
            switch (typeBouton) {
                case TYPE_BOUTON_OK:
                    builder.setPositiveButton("Ok", (dialog, which) -> mListener.onDialogPositiveClick(NoticeDialogFragment.this));
                    break;
                case TYPE_BOUTON_YESNO:
                    builder.setPositiveButton(getString(R.string.yes), (dialog, which) -> mListener.onDialogPositiveClick(NoticeDialogFragment.this))
                            .setNegativeButton(getString(R.string.no), (dialog, which) -> mListener.onDialogNegativeClick(NoticeDialogFragment.this));
                    break;
                default:
            }

            // Gestion de l'image à afficher en haut de la fenêtre
            ImageView imgView = view.findViewById(R.id.imageDialog);
            imgView.setImageResource(idResource);
        }

        mDialog = builder.create();

        return mDialog;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag);
            ft.commitAllowingStateLoss();
        } catch (IllegalStateException exception) {
            Log.e(TAG, exception.getLocalizedMessage(), exception);
        }
        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(android.R.color.white));
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(android.R.color.white));

    }

    public Bundle getBundle() {
        return mBundle;
    }

    /* The activity that creates an instance of this mDialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    public interface DialogListener {
        void onDialogPositiveClick(NoticeDialogFragment dialog);

        void onDialogNegativeClick(NoticeDialogFragment dialog);
    }
}
