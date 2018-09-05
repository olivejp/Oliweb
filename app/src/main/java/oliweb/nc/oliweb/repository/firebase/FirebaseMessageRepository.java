package oliweb.nc.oliweb.repository.firebase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.dto.firebase.MessageFirebase;
import oliweb.nc.oliweb.utility.FirebaseUtilityService;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_MESSAGES_REF;

@Singleton
public class FirebaseMessageRepository {

    private static final String TAG = FirebaseMessageRepository.class.getName();
    private DatabaseReference msgRef;
    private FirebaseUtilityService firebaseUtilityService;

    @Inject
    public FirebaseMessageRepository(FirebaseUtilityService firebaseUtilityService) {
        msgRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_MESSAGES_REF);
        this.firebaseUtilityService = firebaseUtilityService;
    }

    public Single<Long> getCountMessageByUidUserAndUidChat(String uidUser, String uidChat) {
        return Single.create(emitter ->
                msgRef.child(uidChat).orderByChild("uidAuthor").equalTo(uidUser).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        emitter.onSuccess(dataSnapshot.getChildrenCount());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        emitter.onSuccess(0L);
                    }
                })
        );
    }

    public Single<List<MessageFirebase>> getAllMessagesByUidChat(String uidChat) {
        Log.d(TAG, "Starting getAllMessagesByUidChat uidChat : " + uidChat);
        return Single.create(e -> msgRef.child(uidChat)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    ArrayList<MessageFirebase> listMessages = new ArrayList<>();

                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        try {
                            for (DataSnapshot data : dataSnapshot.getChildren()) {
                                MessageFirebase messageFirebase = data.getValue(MessageFirebase.class);
                                if (messageFirebase != null) {
                                    listMessages.add(messageFirebase);
                                }
                            }
                            e.onSuccess(listMessages);
                        } catch (Exception e1) {
                            e.onError(e1);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
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
        return firebaseUtilityService.getServerTimestamp()
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .map(timestamp -> {
                            DatabaseReference newMessageRef = msgRef.child(uidChat).push();
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
                msgRef.child(messageFirebase.getUidChat()).child(messageFirebase.getUidMessage()).setValue(messageFirebase)
                        .addOnSuccessListener(aVoid -> emitter.onSuccess(messageFirebase))
                        .addOnFailureListener(emitter::onError)
        );
    }
}
