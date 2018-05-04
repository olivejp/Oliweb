package oliweb.nc.oliweb.firebase.repository;

import android.content.Context;
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

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.ChatConverter;
import oliweb.nc.oliweb.database.converter.MessageConverter;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_CHATS_REF;

public class FirebaseChatRepository {

    private static final String TAG = FirebaseChatRepository.class.getName();
    private DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_CHATS_REF);

    private static FirebaseChatRepository instance;
    private FirebaseMessageRepository fbMessageRepository;
    private ChatRepository chatRepository;
    private MessageRepository messageRepository;

    private FirebaseChatRepository() {
    }

    public static FirebaseChatRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseChatRepository();
            instance.fbMessageRepository = FirebaseMessageRepository.getInstance(context);
            instance.chatRepository = ChatRepository.getInstance(context);
            instance.messageRepository = MessageRepository.getInstance(context);
        }
        return instance;
    }

    /**
     * @param uidUser
     * @return a Single<List<ChatEntity>> containing all the ChatEntity from Firebase where User is in the members.
     */
    private Single<List<ChatFirebase>> getAllChatByUidUser(String uidUser) {
        Log.d(TAG, "Starting getAllChatByUidUser uidUser : " + uidUser);
        return Single.create(e -> chatRef.orderByChild("members/" + uidUser).equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    ArrayList<ChatFirebase> listChat = new ArrayList<>();

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            if (dataSnapshot != null) {
                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    ChatFirebase chatFirebase = data.getValue(ChatFirebase.class);
                                    if (chatFirebase != null) {
                                        listChat.add(chatFirebase);
                                    }
                                }
                            }
                            e.onSuccess(listChat);
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

    private Single<ChatFirebase> getChatByUid(String uidChat) {
        Log.d(TAG, "Starting getChatByUid uidChat : " + uidChat);
        return Single.create(emitter ->
                chatRef.child(uidChat)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                ChatFirebase chat = dataSnapshot.getValue(ChatFirebase.class);
                                emitter.onSuccess(chat);
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                emitter.onError(new RuntimeException(databaseError.getMessage()));
                            }
                        })
        );
    }

    /**
     * Essaye de trouver un chat existant pour cet utilisateur et pour cette annonce
     *
     * @param userUid
     * @param uidAnnonce
     */
    public Maybe<ChatFirebase> findChat(String userUid, String uidAnnonce) {
        Log.d(TAG, "Starting findChat userUid : " + userUid + " uidAnnonce : " + uidAnnonce);
        return Maybe.create(emitter ->
                chatRef.orderByChild("members/" + userUid)
                        .equalTo(true)
                        .addListenerForSingleValueEvent(
                                new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        boolean found = false;
                                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                                            ChatFirebase chat = data.getValue(ChatFirebase.class);
                                            if (chat != null && chat.getUidAnnonce().equals(uidAnnonce)) {
                                                emitter.onSuccess(chat);
                                                emitter.onComplete();
                                                found = true;
                                                break;
                                            }
                                        }
                                        if (!found) {
                                            emitter.onError(new RuntimeException("Nothing to return"));
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                        emitter.onError(new RuntimeException(databaseError.getMessage()));
                                    }
                                }
                        )
        );
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

    public Single<ChatFirebase> createChat(String uidUserBuyer, AnnonceEntity annonce) {
        Log.d(TAG, "Starting createChat uidUserBuyer : " + uidUserBuyer + " annonce : " + annonce);
        return Single.create(emitter ->
                FirebaseUtility.getServerTimestamp()
                        .doOnError(emitter::onError)
                        .doOnSuccess(timestamp -> {
                            try {
                                DatabaseReference ref = chatRef.push();
                                HashMap<String, Boolean> hash = new HashMap<>();
                                hash.put(uidUserBuyer, true);
                                hash.put(annonce.getUidUser(), true);

                                ChatFirebase chatFirebase = new ChatFirebase();
                                chatFirebase.setUid(ref.getKey());
                                chatFirebase.setUidAnnonce(annonce.getUid());
                                chatFirebase.setMembers(hash);
                                chatFirebase.setUidBuyer(uidUserBuyer);
                                chatFirebase.setUidSeller(annonce.getUidUser());
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
     * Update the updateTimestamp of the chat and the last message.
     *
     * @param uidChat
     * @param messageFirebase
     * @return True if everything works fine, otherwise an excpetion is thrown
     */
    public Single<AtomicBoolean> updateChat(String uidChat, MessageFirebase messageFirebase) {
        Log.d(TAG, "Starting updateChat uidChat : " + uidChat + " messageFirebase : " + messageFirebase);
        return Single.create(emitter ->
                getChatByUid(uidChat)
                        .doOnError(emitter::onError)
                        .doOnSuccess(chatFirebase ->
                                FirebaseUtility.getServerTimestamp()
                                        .doOnError(emitter::onError)
                                        .doOnSuccess(timestamp -> {
                                            try {
                                                chatFirebase.setLastMessage(messageFirebase.getMessage());
                                                chatFirebase.setUpdateTimestamp(timestamp);
                                                chatRef.child(uidChat)
                                                        .setValue(chatFirebase)
                                                        .addOnSuccessListener(aVoid1 -> emitter.onSuccess(new AtomicBoolean(true)))
                                                        .addOnFailureListener(emitter::onError);
                                            } catch (Exception e) {
                                                emitter.onError(e);
                                            }
                                        })
                                        .subscribe()
                        )
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

    /**
     * Get a list of ChatFirebase from Firebase then convert them to ChatEntity
     * and for each ChatEntity try to insert it to the local DB.
     *
     * @param uidUser
     * @return
     */
    public void sync(String uidUser) {
        Log.d(TAG, "Starting sync uidUser : " + uidUser);
        getAllChatByUidUser(uidUser)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .flattenAsObservable(chatFirebases -> chatFirebases)
                .map(ChatConverter::convertDtoToEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnNext(this::saveChat)
                .subscribe();
    }

    private void saveChat(ChatEntity chatEntity) {
        Log.d(TAG, "Starting saveChat chatEntity : " + chatEntity);
        chatRepository.saveWithSingle(chatEntity)
                .doOnSuccess(this::saveMessagesFromChat)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribe();
    }

    private void saveMessagesFromChat(ChatEntity chatEntity) {
        fbMessageRepository.getAllMessagesByUidChat(chatEntity.getUidChat())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .flattenAsObservable(messageFirebases -> messageFirebases)
                .map(messageFirebase -> MessageConverter.convertDtoToEntity(chatEntity.getId(), messageFirebase))
                .doOnNext(messageEntity ->
                        messageRepository.saveWithSingle(messageEntity)
                                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                .subscribe()
                )
                .subscribe();
    }
}
