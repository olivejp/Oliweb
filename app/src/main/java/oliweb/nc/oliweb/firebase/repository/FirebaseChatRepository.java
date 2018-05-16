package oliweb.nc.oliweb.firebase.repository;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_CHATS_REF;

public class FirebaseChatRepository {

    private static final String TAG = FirebaseChatRepository.class.getName();
    private DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_CHATS_REF);

    private static FirebaseChatRepository instance;
    private FirebaseMessageRepository fbMessageRepository;

    private FirebaseChatRepository() {
    }

    public static FirebaseChatRepository getInstance() {
        if (instance == null) {
            instance = new FirebaseChatRepository();
            instance.fbMessageRepository = FirebaseMessageRepository.getInstance();
        }
        return instance;
    }

    private Single<AtomicBoolean> removeChatByUid(String uidChat) {
        Log.d(TAG, "Starting removeChatByUid uidChat : " + uidChat);
        return Single.create(emitter ->
                chatRef.child(uidChat)
                        .removeValue()
                        .addOnSuccessListener(aVoid -> emitter.onSuccess(new AtomicBoolean(true)))
                        .addOnFailureListener(emitter::onError)
        );
    }

    private Single<List<ChatFirebase>> getChatsByUidAnnonce(String uidAnnonce) {
        Log.d(TAG, "Starting getChatByUidAnnonce uidAnnonce : " + uidAnnonce);
        return Single.create(emitter ->
                chatRef.orderByChild("uidAnnonce")
                        .equalTo(uidAnnonce)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                ArrayList<ChatFirebase> listResult = new ArrayList<>();
                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    ChatFirebase chat = data.getValue(ChatFirebase.class);
                                    listResult.add(chat);
                                }
                                emitter.onSuccess(listResult);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                emitter.onError(new RuntimeException(databaseError.getMessage()));
                            }
                        })
        );
    }

    public Single<ChatFirebase> createChat(ChatEntity chatEntity) {
        Log.d(TAG, "Starting createChat chatEntity : " + chatEntity);
        return Single.create(emitter ->
                FirebaseUtility.getServerTimestamp()
                        .doOnError(emitter::onError)
                        .doOnSuccess(timestamp -> {
                            try {
                                DatabaseReference ref = chatRef.push();
                                HashMap<String, Boolean> hash = new HashMap<>();
                                hash.put(chatEntity.getUidBuyer(), true);
                                hash.put(chatEntity.getUidSeller(), true);

                                ChatFirebase chatFirebase = new ChatFirebase();
                                chatFirebase.setUid(ref.getKey());
                                chatFirebase.setUidAnnonce(chatEntity.getUidAnnonce());
                                chatFirebase.setMembers(hash);
                                chatFirebase.setUidBuyer(chatEntity.getUidBuyer());
                                chatFirebase.setUidSeller(chatEntity.getUidSeller());
                                chatFirebase.setTitreAnnonce(chatEntity.getTitreAnnonce());
                                chatFirebase.setLastMessage(chatEntity.getLastMessage());
                                chatFirebase.setCreationTimestamp(timestamp);
                                chatFirebase.setUpdateTimestamp(timestamp);
                                ref.setValue(chatFirebase)
                                        .addOnSuccessListener(aVoid -> emitter.onSuccess(chatFirebase))
                                        .addOnFailureListener(emitter::onError);
                            } catch (Exception e) {
                                emitter.onError(e);
                            }
                        })
                        .subscribe()
        );
    }

    /**
     * Delete the chats by uidAnnonce and the messages related.
     *
     * @param uidAnnonce
     * @return AtomicBoolean will return true if chat and message has been deleted, false otherwise.
     */
    public Single<AtomicBoolean> deleteChatByAnnonceUid(String uidAnnonce) {
        Log.d(TAG, "Starting deleteChatByAnnonceUid uidAnnonce : " + uidAnnonce);
        return Single.create(emitter ->
                getChatsByUidAnnonce(uidAnnonce)
                        .doOnError(emitter::onError)
                        .flattenAsObservable(list -> list)
                        .doOnNext(chat ->
                                fbMessageRepository.deleteMessageByUidChat(chat.getUid())
                                        .doOnError(emitter::onError)
                                        .doOnSuccess(atomicBoolean -> {
                                            if (atomicBoolean.get()) {
                                                removeChatByUid(chat.getUid())
                                                        .doOnError(emitter::onError)
                                                        .doOnSuccess(emitter::onSuccess)
                                                        .subscribe();
                                            } else {
                                                emitter.onError(new RuntimeException("deleteMessageByUidChat return false"));
                                            }
                                        })
                                        .subscribe()
                        )
                        .subscribe());
    }

}
