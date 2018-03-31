package oliweb.nc.oliweb.ui.fragment;

import android.annotation.TargetApi;
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
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import oliweb.nc.oliweb.Constants;
import oliweb.nc.oliweb.R;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.ui.activity.viewmodel.MyChatsActivityViewModel;
import oliweb.nc.oliweb.ui.adapter.MessageFirebaseAdapter;

import static oliweb.nc.oliweb.Constants.FIREBASE_DB_MESSAGES_REF;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ListMessageFragment extends Fragment {
    private static final String TAG = ListMessageFragment.class.getName();

    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_MESSAGES_REF);
    private FirebaseRecyclerOptions<MessageFirebase> options;
    private AppCompatActivity appCompatActivity;
    private String uidChat;
    private MessageFirebaseAdapter adapter;
    private Vibrator vibrator;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyChatsActivityViewModel viewModel = ViewModelProviders.of(appCompatActivity).get(MyChatsActivityViewModel.class);

        vibrator = (Vibrator) appCompatActivity.getSystemService(Context.VIBRATOR_SERVICE);

        uidChat = viewModel.getSelectedUidChat();
        Query query = reference.child(uidChat).orderByChild("timestamp");
        options = new FirebaseRecyclerOptions.Builder<MessageFirebase>()
                .setQuery(query, MessageFirebase.class)
                .build();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list_message, container, false);

        ButterKnife.bind(this, view);

        adapter = new MessageFirebaseAdapter(options);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(appCompatActivity));

        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_MESSAGES_REF).child(uidChat).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot datasnapshot1 : dataSnapshot.getChildren()) {
                    MessageFirebase messageFirebase = datasnapshot1.getValue(MessageFirebase.class);
                    Log.d(TAG, messageFirebase.toString());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.stopListening();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void sendVibrationO() {

    }

    @OnClick(R.id.button_send_message)
    public void sendMessage(View v) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_MESSAGES_REF).child(uidChat).push();

        // Génération du message à envoyer
        MessageFirebase messageFirebase = new MessageFirebase();
        messageFirebase.setMessage(textToSend.getText().toString());
        messageFirebase.setUidAuthor(FirebaseAuth.getInstance().getUid());

        // On désactive le bouton envoyer
        imageSend.setEnabled(false);

        reference.setValue(messageFirebase)
                .addOnSuccessListener(aVoid -> {
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
}
