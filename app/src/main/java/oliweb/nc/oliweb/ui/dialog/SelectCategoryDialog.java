package oliweb.nc.oliweb.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by 2761oli on 21/03/2018.
 */

public class SelectCategoryDialog extends DialogFragment {

    private static final String TAG = SelectCategoryDialog.class.getCanonicalName();
    public static final String BUNDLE_LIST_CATEGORIE = "BUNDLE_LIST_CATEGORIE";

    private AppCompatActivity appCompatActivity;
    private SelectCategoryDialogListener listener;
    private String[] listCategories;
    private ArrayList<String> selectedCategories;

    public static SelectCategoryDialog createInstance(String[] listCategories) {
        Bundle bundle = new Bundle();
        bundle.putStringArray(BUNDLE_LIST_CATEGORIE, listCategories);

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
                    + " doit étendre UpdateSortDialogListener");
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
        }
        selectedCategories = new ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(appCompatActivity);
        builder.setTitle("Catégories");
        builder.setMultiChoiceItems(listCategories, null, (dialogInterface, i, isChecked) -> {
            String categorieUsed = listCategories[i];
            if (isChecked) {
                if (selectedCategories.contains(categorieUsed)) {
                    selectedCategories.remove(categorieUsed);
                } else {
                    selectedCategories.add(categorieUsed);
                }
            }
        });
        builder.setPositiveButton("Choisir", (dialogInterface, i) -> this.listener.choose(selectedCategories));
        return builder.create();
    }

    public interface SelectCategoryDialogListener {
        void choose(ArrayList<String> categories);
    }
}
