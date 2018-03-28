package oliweb.nc.oliweb.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.ui.adapter.MessageFirebaseAdapter;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_MESSAGES_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ListMessageFragment extends Fragment {
    private static final String TAG = ListMessageFragment.class.getName();

    private static final String ARG_UID_CHAT = "ARG_UID_CHAT";
    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_MESSAGES_REF);
    private FirebaseRecyclerOptions<MessageFirebase> options;
    private AppCompatActivity appCompatActivity;
    private String uidChat;
    private MessageFirebaseAdapter adapter;

    @BindView(R.id.recycler_list_message)
    RecyclerView recyclerView;

    @BindView(R.id.text_to_send)
    EditText textToSend;

    @BindView(R.id.button_send_message)
    ImageButton buttonMessage;

    public static ListMessageFragment getInstance(@Nullable String uidChat) {
        ListMessageFragment listMessageFragment = new ListMessageFragment();
        Bundle bundle = new Bundle();
        if (uidChat != null) {
            bundle.putString(ARG_UID_CHAT, uidChat);
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
        if (getArguments() != null && getArguments().containsKey(ARG_UID_CHAT)) {
            uidChat = getArguments().getString(ARG_UID_CHAT);
            Query query = reference.child(uidChat);
            options = new FirebaseRecyclerOptions.Builder<MessageFirebase>()
                    .setQuery(query, MessageFirebase.class)
                    .build();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_message_list, container, false);

        ButterKnife.bind(this, view);

        adapter = new MessageFirebaseAdapter(options);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(appCompatActivity));

        return view;
    }

    @OnClick(R.id.button_send_message)
    public void sendMessage(View v) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_MESSAGES_REF).child(uidChat).push();

        // Génération du message à envoyer
        MessageFirebase messageFirebase = new MessageFirebase();
        messageFirebase.setMessage(textToSend.getText().toString());
        messageFirebase.setUidAuthor(FirebaseAuth.getInstance().getUid());

        // On désactive le bouton envoyer
        buttonMessage.setEnabled(false);

        reference.setValue(messageFirebase)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Un message a été envoyé");
                    textToSend.setText("");
                    buttonMessage.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Un message n'a pas réussi à être envoyé." + e.getLocalizedMessage());
                    buttonMessage.setEnabled(true);
                });
    }
}
