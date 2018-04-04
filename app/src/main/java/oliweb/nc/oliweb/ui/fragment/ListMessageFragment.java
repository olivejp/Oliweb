package oliweb.nc.oliweb.ui.fragment;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.ui.activity.viewmodel.ListeningForChat;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.MessageFirebaseAdapter;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_MESSAGES_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ListMessageFragment extends Fragment implements ListeningForChat {
    private static final String TAG = ListMessageFragment.class.getName();

    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_MESSAGES_REF);
    private FirebaseRecyclerOptions<MessageFirebase> options;
    private AppCompatActivity appCompatActivity;
    private String uidChat;
    private AnnonceEntity annonce;
    private MessageFirebaseAdapter adapter;
    private Vibrator vibrator;
    private MyChatsActivityViewModel viewModel;
    private Query query;

    @BindView(R.id.recycler_list_message)
    RecyclerView recyclerView;

    @BindView(R.id.text_to_send)
    EditText textToSend;

    @BindView(R.id.button_send_message)
    ImageView imageSend;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        appCompatActivity = (AppCompatActivity) context;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = ViewModelProviders.of(appCompatActivity).get(MyChatsActivityViewModel.class);
        vibrator = (Vibrator) appCompatActivity.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_message, container, false);

        ButterKnife.bind(this, view);

        LinearLayoutManager linearLayout = new LinearLayoutManager(appCompatActivity);
        linearLayout.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayout);

        switch (viewModel.getTypeRecherche()) {
            case PAR_ANNONCE:
                annonce = viewModel.getSelectedAnnonce();
                break;
            case PAR_UTILISATEUR:
                break;
            case PAR_CHAT:
                uidChat = viewModel.getSelectedUidChat();
                query = reference.child(uidChat).orderByChild("timestamp");
                attachFirebaseRefToAdapter();
                break;
        }

        return view;
    }

    private void attachFirebaseRefToAdapter() {
        options = new FirebaseRecyclerOptions.Builder<MessageFirebase>()
                .setQuery(query, MessageFirebase.class)
                .build();
        adapter = new MessageFirebaseAdapter(options);
        recyclerView.setAdapter(adapter);
    }

    @OnClick(R.id.button_send_message)
    public void clickOnSend(View v) {
        sendMessage();
    }

    private void sendMessage() {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_MESSAGES_REF).child(uidChat).push();

        // Génération du message à envoyer
        MessageFirebase messageFirebase = new MessageFirebase();
        messageFirebase.setMessage(textToSend.getText().toString());
        messageFirebase.setUidAuthor(FirebaseAuth.getInstance().getUid());

        // On désactive le bouton envoyer
        imageSend.setEnabled(false);

        reference.setValue(messageFirebase)
                .addOnSuccessListener(aVoid -> {
                    reference.child("timestamp").setValue(ServerValue.TIMESTAMP);
                    Log.d(TAG, "Un message a été envoyé");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(500, 50));
                    } else {
                        vibrator.vibrate(500);
                    }
                    textToSend.setText("");
                    imageSend.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.d(TAG, "Un message n'a pas réussi à être envoyé." + e.getLocalizedMessage());
                    imageSend.setEnabled(true);
                });
    }


    private void lookForExistingChat(ListeningForChat listener) {
        // Recherche dans Firebase si on a déjà une conversation pour cette annonce
        FirebaseDatabase.getInstance()
                .getReference(Constants.FIREBASE_DB_CHATS_REF)
                .orderByChild("members/" + FirebaseAuth.getInstance().getUid())
                .equalTo(true)
                .addListenerForSingleValueEvent(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                boolean found = false;
                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    ChatFirebase chat = data.getValue(ChatFirebase.class);
                                    if (chat.getUidAnnonce().equals(annonce.getUUID())) {
                                        listener.findChat(chat);
                                        uidChat = chat.getUid();
                                        sendMessage();
                                        found = true;
                                        break;
                                    }
                                }

                                // Create new chat
                                if (!found) {
                                    ChatFirebase chat = viewModel.createNewFirebaseChat(annonce);
                                    FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_CHATS_REF).child(chat.getUid())
                                            .setValue(chat)
                                            .addOnSuccessListener(aVoid -> listener.findChat(chat));
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // Do nothing
                            }
                        }
                );
    }

    @Override
    public void findChat(@NonNull ChatFirebase chat) {
        uidChat = chat.getUid();
        sendMessage();
    }

}
