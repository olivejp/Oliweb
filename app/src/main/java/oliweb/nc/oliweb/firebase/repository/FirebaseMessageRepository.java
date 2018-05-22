package oliweb.nc.oliweb.firebase.repository;

import android.support.annotation.NonNull;
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
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_MESSAGES_REF;

public class FirebaseMessageRepository {

    private static final String TAG = FirebaseMessageRepository.class.getName();
    private DatabaseReference MSG_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_MESSAGES_REF);

    private static FirebaseMessageRepository instance;

    private FirebaseMessageRepository() {
    }

    public static FirebaseMessageRepository getInstance() {
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

    /**
     * Va récupérer un uid d'un message et le timestamp du serveur
     *
     * @param messageEntity
     * @return
     */
    public Single<MessageEntity> getUidAndTimestampFromFirebase(@NonNull String uidChat, MessageEntity messageEntity) {
        Log.d(TAG, "Starting getUidAndTimestampFromFirebase uidChat : " + uidChat + " messageEntity : " + messageEntity);
        return FirebaseUtility.getServerTimestamp()
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .map(timestamp -> {
                            DatabaseReference newMessageRef = MSG_REF.child(uidChat).push();
                            messageEntity.setTimestamp(timestamp);
                            messageEntity.setUidMessage(newMessageRef.getKey());
                            messageEntity.setUidChat(uidChat);
                            return messageEntity;
                        }
                );
    }


    /**
     * Envoi d'un message sur la Firebase Database
     *
     * @param messageFirebase
     * @return
     */
    public Single<MessageFirebase> saveMessage(MessageFirebase messageFirebase) {
        Log.d(TAG, "Starting saveMessage messageFirebase : " + messageFirebase);
        return Single.create(emitter ->
                MSG_REF.child(messageFirebase.getUidChat()).child(messageFirebase.getUidMessage()).setValue(messageFirebase)
                        .addOnSuccessListener(aVoid -> emitter.onSuccess(messageFirebase))
                        .addOnFailureListener(emitter::onError)
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
