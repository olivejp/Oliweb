package oliweb.nc.oliweb.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ListMessageFragment extends Fragment {
    private static final String TAG = ListMessageFragment.class.getName();

    private static final String ARG_UID_USER = "ARG_UID_USER";
    private static final String ARG_UID_ANNONCE = "ARG_UID_ANNONCE";
    private static final String ARG_ACTION = "ARG_ACTION";

    private AppCompatActivity appCompatActivity;
    private String uidAnnonce;
    private String uidUtilisateur;

    public static ListMessageFragment getInstance(@Nullable String uidUtilisateur, @Nullable String uidAnnonce) {
        ListMessageFragment listMessageFragment = new ListMessageFragment();
        Bundle bundle = new Bundle();
        if (uidUtilisateur != null) {
            bundle.putString(ARG_UID_USER, uidUtilisateur);
        }
        if (uidAnnonce != null) {
            bundle.putString(ARG_UID_ANNONCE, uidAnnonce);
        }
        listMessageFragment.setArguments(bundle);
        return listMessageFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().containsKey(ARG_UID_ANNONCE)) {
                uidAnnonce = getArguments().getString(ARG_UID_ANNONCE);
            }
            if (getArguments().containsKey(ARG_UID_USER)) {
                uidUtilisateur = getArguments().getString(ARG_UID_USER);
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message_list, container, false);

        ButterKnife.bind(this, view);

        return view;
    }
}
