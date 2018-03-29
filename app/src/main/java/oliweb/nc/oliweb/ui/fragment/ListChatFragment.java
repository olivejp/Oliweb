package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.ChatFirebaseAdapter;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_CHATS_REF;
import static oliweb.nc.oliweb.ui.activity.MyChatsActivity.TAG_DETAIL_FRAGMENT;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ListChatFragment extends Fragment {

    private static final String ARG_UID_ANNONCE = "ARG_UID_ANNONCE";

    private AppCompatActivity appCompatActivity;
    private String uidAnnonce;
    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_CHATS_REF);
    private FirebaseRecyclerOptions<ChatFirebase> options;
    private ChatFirebaseAdapter adapter;

    private MyChatsActivityViewModel viewModel;

    @BindView(R.id.recycler_list_chats)
    RecyclerView recyclerView;

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

        viewModel = ViewModelProviders.of(appCompatActivity).get(MyChatsActivityViewModel.class);

        Query query = null;
        if (getArguments() != null && getArguments().containsKey(ARG_UID_ANNONCE)) {
            uidAnnonce = getArguments().getString(ARG_UID_ANNONCE);
            query = reference.orderByChild("uidAnnonce").equalTo(uidAnnonce);
        }
        if (viewModel.getFirebaseUidUser() != null) {
            query = reference.orderByChild("members/" + viewModel.getFirebaseUidUser()).equalTo(true);
        }

        options = new FirebaseRecyclerOptions.Builder<ChatFirebase>()
                .setQuery(query, ChatFirebase.class)
                .build();


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_chat, container, false);

        ButterKnife.bind(this, view);

        adapter = new ChatFirebaseAdapter(options, v -> displayListMessage((String) v.getTag()));

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

    private void displayListMessage(String uidChat) {
        viewModel.setSelectedUidChat(uidChat);
        if (getFragmentManager() != null) {
            ListMessageFragment listMessageFragment = new ListMessageFragment();
            if (viewModel.isTwoPane()) {
                getFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frame_messages, listMessageFragment, TAG_DETAIL_FRAGMENT)
                        .commit();
            } else {
                getFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left, android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.frame_chats, listMessageFragment, TAG_DETAIL_FRAGMENT)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }
}
