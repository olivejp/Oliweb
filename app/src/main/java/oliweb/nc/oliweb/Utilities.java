package oliweb.nc.oliweb;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import oliweb.nc.oliweb.ui.dialog.NoticeDialogFragment;


/**
 * Created by orlanth23 on 13/08/2017.
 */

public class Utilities {

    private Utilities() {
    }

    /**
     * Hide the keyboard
     *
     * @param ctx
     */
    public static void hideKeyboard(Context ctx) {
        InputMethodManager inputManager = (InputMethodManager) ctx
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        View v = ((Activity) ctx).getCurrentFocus();
        if (v == null)
            return;

        if (inputManager != null) {
            inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    /**
     * @param fragmentManager Get from the context
     * @param message         The message to be send
     * @param type            From NoticeDialogFragment
     * @param img             From NoticeDialogFragment
     * @param tag             A text to be a tag
     */
    public static void sendDialogByFragmentManager(FragmentManager fragmentManager, String message, int type, int img, @Nullable String tag, @Nullable Bundle bundlePar, NoticeDialogFragment.DialogListener listener) {
        NoticeDialogFragment dialogErreur = new NoticeDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NoticeDialogFragment.P_MESSAGE, message);
        bundle.putInt(NoticeDialogFragment.P_TYPE, type);
        bundle.putInt(NoticeDialogFragment.P_IMG, img);
        bundle.putBundle(NoticeDialogFragment.P_BUNDLE, bundlePar);
        dialogErreur.setListener(listener);
        dialogErreur.setArguments(bundle);
        dialogErreur.show(fragmentManager, tag);
    }

    /**
     * @param fragmentManager Get from the context
     * @param message         The message to be send
     * @param type            From NoticeDialogFragment
     * @param idDrawable      From NoticeDialogFragment
     * @param tag             A text to be a tag
     */
    public static void sendDialogByFragmentManagerWithRes(FragmentManager fragmentManager, String message, int type, @DrawableRes int idDrawable, @Nullable String tag, @Nullable Bundle bundlePar, NoticeDialogFragment.DialogListener listener) {
        NoticeDialogFragment dialogErreur = new NoticeDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NoticeDialogFragment.P_MESSAGE, message);
        bundle.putInt(NoticeDialogFragment.P_TYPE, type);
        bundle.putInt(NoticeDialogFragment.P_IMG, idDrawable);
        bundle.putBundle(NoticeDialogFragment.P_BUNDLE, bundlePar);
        dialogErreur.setListener(listener);
        dialogErreur.setArguments(bundle);
        dialogErreur.show(fragmentManager, tag);
    }

}
