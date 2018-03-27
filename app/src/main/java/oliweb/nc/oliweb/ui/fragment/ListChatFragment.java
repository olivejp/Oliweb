package oliweb.nc.oliweb.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import butterknife.BindView;
import butterknife.ButterKnife;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.ui.adapter.ChatFirebaseAdapter;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_CHATS_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ListChatFragment extends Fragment {
    private static final String TAG = ListChatFragment.class.getName();

    private static final String ARG_UID_USER = "ARG_UID_USER";
    private static final String ARG_UID_ANNONCE = "ARG_UID_ANNONCE";

    private AppCompatActivity appCompatActivity;
    private String uidAnnonce;
    private String uidUtilisateur;
    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_CHATS_REF);
    private FirebaseRecyclerOptions<ChatFirebase> options;
    private ChatFirebaseAdapter adapter;
    private View.OnClickListener clickListener;

    @BindView(R.id.recycler_list_chats)
    RecyclerView recyclerView;

    public static ListChatFragment getInstance(@Nullable String uidUtilisateur, @Nullable String uidAnnonce, @NonNull View.OnClickListener clickListener) {
        ListChatFragment listChatFragment = new ListChatFragment();
        Bundle bundle = new Bundle();
        if (uidUtilisateur != null) {
            bundle.putString(ARG_UID_USER, uidUtilisateur);
        }
        if (uidAnnonce != null) {
            bundle.putString(ARG_UID_ANNONCE, uidAnnonce);
        }
        listChatFragment.setArguments(bundle);
        listChatFragment.setClickListener(clickListener);
        return listChatFragment;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
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
            Query query = null;
            if (getArguments().containsKey(ARG_UID_ANNONCE)) {
                uidAnnonce = getArguments().getString(ARG_UID_ANNONCE);
                query = reference.orderByChild("uidAnnonce").equalTo(uidAnnonce);
            }
            if (getArguments().containsKey(ARG_UID_USER)) {
                uidUtilisateur = getArguments().getString(ARG_UID_USER);
                query = reference.orderByChild("members/" + uidUtilisateur).equalTo(true);
            }

            options = new FirebaseRecyclerOptions.Builder<ChatFirebase>()
                    .setQuery(query, ChatFirebase.class)
                    .build();

        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_chat, container, false);

        ButterKnife.bind(this, view);

        adapter = new ChatFirebaseAdapter(options, clickListener);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(appCompatActivity));

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    public void setClickListener(View.OnClickListener clickListener) {
        this.clickListener = clickListener;
    }
}
