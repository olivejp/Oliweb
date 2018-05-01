package oliweb.nc.oliweb.firebase.repository;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_MESSAGES_REF;

public class FirebaseMessageRepository {

    private static final String TAG = FirebaseMessageRepository.class.getName();
    private DatabaseReference MSG_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_MESSAGES_REF);

    private static FirebaseMessageRepository instance;

    private FirebaseMessageRepository() {
    }

    public static FirebaseMessageRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseMessageRepository();
        }
        return instance;
    }

    public Single<List<MessageFirebase>> getAllMessagesByUidChat(String uidChat) {
        Log.d(TAG, "Starting getAllMessagesByUidChat uidChat : " + uidChat);
        return Single.create(e -> MSG_REF.child(uidChat)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    ArrayList<MessageFirebase> listMessages = new ArrayList<>();

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            if (dataSnapshot != null) {
                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    MessageFirebase messageFirebase = data.getValue(MessageFirebase.class);
                                    if (messageFirebase != null) {
                                        listMessages.add(messageFirebase);
                                    }
                                }
                            }
                            e.onSuccess(listMessages);
                        } catch (Exception e1) {
                            e.onError(e1);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        e.onError(new RuntimeException(databaseError.getMessage()));
                    }
                }));
    }

    public Single<AtomicBoolean> saveMessage(String uidChat, MessageFirebase messageFirebase) {
        Log.d(TAG, "Starting saveMessage uidChat : " + uidChat + " messageFirebase : " + messageFirebase);
        return Single.create(emitter ->
                FirebaseUtility.getServerTimestamp()
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(timestamp -> {
                            messageFirebase.setTimestamp(timestamp);
                            DatabaseReference newMessageRef = MSG_REF.child(uidChat).push();
                            newMessageRef.setValue(messageFirebase)
                                    .addOnSuccessListener(aVoid -> emitter.onSuccess(new AtomicBoolean(true)))
                                    .addOnFailureListener(emitter::onError);
                        })
                        .subscribe()
        );
    }

    public Single<AtomicBoolean> deleteMessageByUidChat(String uidChat) {
        Log.d(TAG, "Starting deleteMessageByUidChat uidChat : " + uidChat);
        return Single.create(emitter ->
                MSG_REF.child(uidChat)
                        .removeValue()
                        .addOnSuccessListener(aVoid -> emitter.onSuccess(new AtomicBoolean(true)))
                        .addOnFailureListener(emitter::onError)
        );
    }
}
