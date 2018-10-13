package oliweb.nc.oliweb.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import oliweb.nc.oliweb.R;

/**
 * Created by 2761oli on 21/03/2018.
 */

public class SortDialog extends DialogFragment {

    private AppCompatActivity appCompatActivity;
    private UpdateSortDialogListener listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (UpdateSortDialogListener) context;
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
        AlertDialog.Builder builder = new AlertDialog.Builder(appCompatActivity);
        builder.setTitle("Trier");
        builder.setItems(R.array.pref_sort, (dialog, which) -> listener.sortHasBeenUpdated(which));
        return builder.create();
    }

    public interface UpdateSortDialogListener {
        void sortHasBeenUpdated(int sort);
    }
}
