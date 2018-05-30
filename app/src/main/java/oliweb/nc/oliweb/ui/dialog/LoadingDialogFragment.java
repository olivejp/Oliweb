package oliweb.nc.oliweb.ui.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;


public class LoadingDialogFragment extends DialogFragment {

    private static final String defaultMessage = "Recherche en cours ...";

    // Use this instance of the interface to deliver action events
    private AppCompatActivity appCompatActivity;

    @BindView(R.id.progress_bar_loading)
    ProgressBar progressBarLoading;

    @BindView(R.id.text_loading)
    TextView textView;

    private String textToDisplay;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    public void setText(String text) {
        this.textToDisplay = text;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(appCompatActivity);

        LayoutInflater inflater = this.appCompatActivity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_loading_layout, null);

        ButterKnife.bind(this, view);

        if (this.textToDisplay != null && !this.textToDisplay.isEmpty()) {
            textView.setText(this.textToDisplay);
        } else {
            textView.setText(defaultMessage);
        }


        progressBarLoading.setVisibility(View.GONE);
        progressBarLoading.setVisibility(View.VISIBLE);
        builder.setView(view);

        return builder.create();
    }
}
