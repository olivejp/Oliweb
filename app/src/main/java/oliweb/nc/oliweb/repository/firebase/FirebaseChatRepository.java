package oliweb.nc.oliweb.repository.firebase;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.dto.firebase.ChatFirebase;
import oliweb.nc.oliweb.dto.firebase.MessageFirebase;
import oliweb.nc.oliweb.utility.FirebaseUtility;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_CHATS_REF;

public class FirebaseChatRepository {

    private static final String TAG = FirebaseChatRepository.class.getName();
    private DatabaseReference chatRef;
    private FirebaseMessageRepository fbMessageRepository;
    private FirebaseUserRepository fbUserRepository;

    @Inject
    public FirebaseChatRepository(FirebaseMessageRepository fbMessageRepository,
                                  FirebaseUserRepository fbUserRepository) {
        chatRef = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_CHATS_REF);
        this.fbMessageRepository = fbMessageRepository;
        this.fbUserRepository = fbUserRepository;
    }

    public LiveData<Long> getCountMessageByUidUser(String uidUser) {
        return new LiveData<Long>() {
            @Override
            public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<Long> observer) {
                super.observe(owner, observer);
                List<Long> countTotal = new ArrayList<>();
                getByUidUser(uidUser)
                        .flattenAsObservable(chatFirebases -> chatFirebases)
                        .flatMapSingle(chatFirebase -> fbMessageRepository.getCountMessageByUidUserAndUidChat(uidUser, chatFirebase.getUid()))
                        .doOnNext(countTotal::add)
                        .doOnComplete(() -> {
                            Long total = 0L;
                            for (Long count : countTotal) {
                                total = total + count;
                            }
                            observer.onChanged(total);
                        })
                        .subscribe();
            }
        };
    }

    public LiveData<Long> getCountChatByUidUser(String uidUser) {
        return new LiveData<Long>() {
            @Override
            public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<Long> observer) {
                super.observe(owner, observer);
                chatRef.orderByChild("members/" + uidUser).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        long count = dataSnapshot.getChildrenCount();
                        observer.onChanged(count);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        observer.onChanged(0L);
                    }
                });
            }
        };
    }

    /**
     * Va récupérer un uid d'un message et le timestamp du serveur
     *
     * @param chatEntity
     * @return
     */
    public Single<ChatEntity> getUidAndTimestampFromFirebase(ChatEntity chatEntity) {
        Log.d(TAG, "Starting getUidAndTimestampFromFirebase chatEntity : " + chatEntity);
        return Single.create(emitter ->
                FirebaseUtility.getServerTimestamp()
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(timestamp -> {
                            DatabaseReference newChetRef = chatRef.push();
                            if (newChetRef.getKey() != null) {
                                chatEntity.setCreationTimestamp(timestamp);
                                chatEntity.setUidChat(newChetRef.getKey());
                                emitter.onSuccess(chatEntity);
                            } else {
                                emitter.onError(new FirebaseRepositoryException("Firebase returned a null uid chat."));
                            }
                        })
                        .subscribe()
        );
    }

    /**
     * @param chatFirebase
     * @return
     */
    public Single<ChatFirebase> saveChat(ChatFirebase chatFirebase) {
        Log.d(TAG, "Starting saveChat chatFirebase : " + chatFirebase);
        return Single.create(emitter ->
                chatRef.child(chatFirebase.getUid()).setValue(chatFirebase)
                        .addOnSuccessListener(aVoid -> emitter.onSuccess(chatFirebase))
                        .addOnFailureListener(emitter::onError)
        );
    }

    /**
     * Update the update date and the last message of a chat
     *
     * @param messageFirebase
     * @return
     */
    public Observable<ChatFirebase> updateLastMessageChat(MessageFirebase messageFirebase) {
        Log.d(TAG, "Starting updateLastMessageChat messageFirebase : " + messageFirebase);
        return getByUidChat(messageFirebase.getUidChat())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .toObservable()
                .zipWith(FirebaseUtility.getServerTimestamp().toObservable(), ChatFirebase::setUpdateTimestamp)
                .map(chatFirebaseTimed -> chatFirebaseTimed.setLastMessage(messageFirebase.getMessage()))
                .switchMap(chatFirebaseToSave -> this.saveChat(chatFirebaseToSave).toObservable());
    }

    /**
     * Va lire tous les chats pour l'uid user, puis pour tout ces chats va
     * récupérer tous les membres et pour tous ces membres va récupérer leur photo URL
     */
    public Single<HashMap<String, UserEntity>> getPhotoUrlsByUidUser(String uidUser) {
        HashMap<String, UserEntity> map = new HashMap<>();
        return Single.create(emitter -> getByUidUser(uidUser)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .flattenAsObservable(chatFirebases -> chatFirebases)
                .map(chatFirebase -> chatFirebase.getMembers().keySet())
                .flatMapIterable(uidsUserFromChats -> uidsUserFromChats)
                .flatMap(foreignUidUserFromChat -> fbUserRepository.getUtilisateurByUid(foreignUidUserFromChat).toObservable())
                .distinct()
                .map(utilisateurEntity -> {
                    map.put(utilisateurEntity.getUid(), utilisateurEntity);
                    return map;
                })
                .doOnComplete(() -> emitter.onSuccess(map))
                .subscribe()
        );
    }

    private Single<List<ChatFirebase>> getByUidUser(String uidUser) {
        return Single.create(emitter -> chatRef.orderByChild("members/" + uidUser).equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        ArrayList<ChatFirebase> listResult = new ArrayList<>();
                        for (DataSnapshot data : dataSnapshot.getChildren()) {
                            ChatFirebase chat = data.getValue(ChatFirebase.class);
                            listResult.add(chat);
                        }
                        emitter.onSuccess(listResult);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        emitter.onError(new RuntimeException(databaseError.getMessage()));
                    }
                }));
    }

    /**
     * Récupération d'un chat sur FB avec son UID
     *
     * @param uidChat
     * @return
     */
    private Single<ChatFirebase> getByUidChat(String uidChat) {
        return Single.create(emitter ->
                chatRef.child(uidChat).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        ChatFirebase chatFirebase = dataSnapshot.getValue(ChatFirebase.class);
                        emitter.onSuccess(chatFirebase);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        emitter.onError(new RuntimeException(databaseError.getMessage()));
                    }
                })
        );
    }
}
