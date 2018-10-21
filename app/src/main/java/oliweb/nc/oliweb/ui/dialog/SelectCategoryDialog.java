package oliweb.nc.oliweb.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by 2761oli on 21/03/2018.
 */

public class SelectCategoryDialog extends DialogFragment {

    private static final String TAG = SelectCategoryDialog.class.getCanonicalName();
    public static final String BUNDLE_LIST_CATEGORIE = "BUNDLE_LIST_CATEGORIE";
    public static final String BUNDLE_CHECKED_CATEGORIE = "BUNDLE_CHECKED_CATEGORIE";

    private AppCompatActivity appCompatActivity;
    private SelectCategoryDialogListener listener;
    private String[] listCategories;
    private boolean[] checkedCategories;

    public static SelectCategoryDialog createInstance(String[] listCategories, boolean[] checkedCategories) {
        Bundle bundle = new Bundle();
        bundle.putStringArray(BUNDLE_LIST_CATEGORIE, listCategories);
        bundle.putBooleanArray(BUNDLE_CHECKED_CATEGORIE, checkedCategories);

        SelectCategoryDialog instance = new SelectCategoryDialog();
        instance.setArguments(bundle);
        return instance;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (SelectCategoryDialogListener) context;
        } catch (ClassCastException e) {
            Log.e("ClassCastException", e.getMessage(), e);
            throw new ClassCastException(context.toString()
                    + " doit étendre SelectCategoryDialogListener");
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
        if (getArguments() != null) {
            this.listCategories = getArguments().getStringArray(BUNDLE_LIST_CATEGORIE);
            this.checkedCategories = getArguments().getBooleanArray(BUNDLE_CHECKED_CATEGORIE);
            if (this.checkedCategories == null) {
                this.checkedCategories = new boolean[this.listCategories.length];
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(appCompatActivity);
        builder.setTitle("Catégories");
        builder.setMultiChoiceItems(listCategories, checkedCategories, (dialogInterface, i, isChecked) ->
                this.checkedCategories[i] = isChecked
        );
        builder.setPositiveButton("Choisir", (dialogInterface, i) -> this.listener.choose(checkedCategories));
        return builder.create();
    }

    public interface SelectCategoryDialogListener {
        void choose(boolean[] checkedCat);
    }
}
