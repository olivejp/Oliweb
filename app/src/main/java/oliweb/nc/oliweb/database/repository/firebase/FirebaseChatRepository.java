package oliweb.nc.oliweb.database.repository.firebase;

import android.content.Context;

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
import oliweb.nc.oliweb.database.converter.ChatConverter;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;

import static oliweb.nc.oliweb.utility.Constants.FIREBASE_DB_CHATS_REF;

public class FirebaseChatRepository {

    private static final String TAG = FirebaseChatRepository.class.getName();
    private DatabaseReference CHAT_REF = FirebaseDatabase.getInstance().getReference(FIREBASE_DB_CHATS_REF);

    private static FirebaseChatRepository instance;
    private ChatRepository chatRepository;
    private int countSuccesses;

    private FirebaseChatRepository() {
    }

    public static FirebaseChatRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FirebaseChatRepository();
        }
        instance.chatRepository = ChatRepository.getInstance(context);
        return instance;
    }

    public Single<List<ChatEntity>> getAllChatByUidUser(String uidUser) {
        return Single.create(e -> CHAT_REF.orderByChild("members/" + uidUser).equalTo(true)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    ArrayList<ChatEntity> listChat = new ArrayList<>();

                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            if (dataSnapshot != null) {
                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    ChatFirebase chatFirebase = data.getValue(ChatFirebase.class);
                                    if (chatFirebase != null) {
                                        ChatEntity chatEntity = ChatConverter.convertDtoToEntity(chatFirebase);
                                        listChat.add(chatEntity);
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

    public Single<AtomicBoolean> retreiveFromFirebaseByUidUser(String uidUser) {
        return Single.create(e -> {
            getAllChatByUidUser(uidUser)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnSuccess(chatEntities -> {
                        countSuccesses = 0;
                        for (ChatEntity chat : chatEntities) {
                            chatRepository.saveWithSingle(chat)
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .doOnSuccess(chatEntity -> {
                                        countSuccesses++;
                                        if (countSuccesses == chatEntities.size()) {
                                            e.onSuccess(new AtomicBoolean(true));
                                        }
                                    })
                                    .doOnError(e::onError)
                                    .subscribe();
                        }
                    })
                    .doOnError(e::onError)
                    .subscribe();
        });
    }
}
