package oliweb.nc.oliweb.service.sync.listener;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.ChatConverter;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;

/**
 * This class will create Listeners to some points in Firebase database
 * And sync those items into the local database.
 * Items synced :
 * - Chats
 * - Messages
 */
public class FirebaseSyncListenerService extends Service {

    private static final String TAG = FirebaseSyncListenerService.class.getName();
    public static final String CHAT_SYNC_UID_USER = "CHAT_SYNC_UID_USER";

    private ChatRepository chatRepository;
    private MessageRepository messageRepository;
    private Query queryChat;
    private Map<String, Query> listChatQueryListener;

    private ValueEventListener chatListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            try {
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    ChatFirebase chatFirebase = data.getValue(ChatFirebase.class);
                    if (chatFirebase != null) {
                        ChatEntity chatEntity = ChatConverter.convertDtoToEntity(chatFirebase);
                        chatRepository.saveIfNotExist(chatEntity)
                                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                .doOnComplete(() -> Log.d(TAG, "Chat already exist chatEntity : " + chatEntity))
                                .doOnSuccess(chatEntity1 -> Log.d(TAG, "Chat was not present, creation successful chatEntity : " + chatEntity1))
                                .subscribe();
                    }
                }
            } catch (Exception e1) {
                Log.e(TAG, e1.getLocalizedMessage(), e1);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e(TAG, databaseError.getMessage());
        }
    };

    private ChildEventListener chatChildListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
            // do nothing
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
            // do nothing
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            ChatFirebase chatFirebase = dataSnapshot.getValue(ChatFirebase.class);
            if (chatFirebase != null) {
                chatRepository.deleteByUid(chatFirebase.getUid())
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                        .doOnSuccess(atomicBoolean -> {
                            if (atomicBoolean.get()) {
                                Log.d(TAG, "Successfuly delete chat in the local DB");
                            } else {
                                Log.d(TAG, "Fail to delete chat in the local DB");
                            }
                        })
                        .subscribe();
            }
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {
// do nothing
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
// do nothing
        }
    };

    private ChildEventListener messageChildListener = new ChildEventListener() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            MessageFirebase message = dataSnapshot.getValue(MessageFirebase.class);
            Log.d(TAG, "Nouveau message reçu messageFirebase : " + message);
            if (message != null) {
                messageRepository.saveMessageIfNotExist(message);
            }
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            // Suppression du message de la db locale
            MessageFirebase message = dataSnapshot.getValue(MessageFirebase.class);
            Log.d(TAG, "Suppression du message messageFirebase : " + message);
            if (message != null) {
                messageRepository.findSingleByUid(message.getUidMessage())
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .doOnSuccess(messageEntity -> messageRepository.delete(dataReturn -> {
                            if (dataReturn.isSuccessful()) {
                                Log.d(TAG, "Suppression du message réussie");
                            } else {
                                Log.d(TAG, "Suppression du message échouée");
                            }
                        }, messageEntity))
                        .subscribe();
            }
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            // Modification d'un message de la db locale
            MessageFirebase message = dataSnapshot.getValue(MessageFirebase.class);
            Log.d(TAG, "Mise à jour du message messageFirebase : " + message);
            if (message != null) {
                messageRepository.findSingleByUid(message.getUidMessage())
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .doOnSuccess(messageEntity -> {
                            messageEntity.setMessage(message.getMessage());
                            messageEntity.setTimestamp(message.getTimestamp());
                            messageEntity.setUidAuthor(message.getUidAuthor());
                            messageRepository.singleSave(messageEntity)
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                    .subscribe();
                        })
                        .subscribe();
            }
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {
            // do nothing
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            // do nothing
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Si aucun UID user donné en paramètre, on arrête directement le service.
     *
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Démarrage du service FirebaseSyncListenerService");
        if (intent.getStringExtra(CHAT_SYNC_UID_USER) == null || intent.getStringExtra(CHAT_SYNC_UID_USER).isEmpty()) {
            stopSelf();
        } else {
            chatRepository = ChatRepository.getInstance(this);
            messageRepository = MessageRepository.getInstance(this);
            listChatQueryListener = new HashMap<>();

            String uidUser = intent.getStringExtra(CHAT_SYNC_UID_USER);

            // Récupération des chats de l'utilisateur connecté
            listenForChatByUidUser(uidUser);

            // Création d'observers pour écouter les nouveaux messages
            listenForMessageByUidUser(uidUser);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Stop FirebaseSyncListenerService Bye bye");

        // Suppression des listeners
        queryChat.removeEventListener(chatListener);
        clearMessageListener();
        super.onDestroy();
    }

    /**
     * Recherche tous les chats pour lesquels l'utilisateur connecté est membre
     * et va attacher un chatListener pour chacun d'entre eux pour écouter les nouveaux
     * messages qui y arriveront.
     *
     * @param uidUser
     */
    private void listenForChatByUidUser(String uidUser) {
        Log.d(TAG, "Starting listenForChatByUidUser uidUser : " + uidUser);
        queryChat = FirebaseDatabase.getInstance()
                .getReference(Constants.FIREBASE_DB_CHATS_REF)
                .orderByChild("members/" + uidUser)
                .equalTo(true);

        queryChat.addValueEventListener(chatListener);
        queryChat.addChildEventListener(chatChildListener);
    }

    /**
     * Va créer un observer pour tout les chats présents en base
     * Si on rajoute un chat dans la base, cette méthode créera automatiquement un nouvel observer pour ce chat.
     *
     * @param uidUser
     */
    private void listenForMessageByUidUser(String uidUser) {
        Log.d(TAG, "Starting listenForMessageByUidUser uidUser : " + uidUser);

        // Récupération de la liste des chats pour l'utilisateur connecté et dont le statut n'est pas cloturé
        chatRepository.findFlowableByUidUserAndStatusNotIn(uidUser, Utility.allStatusToAvoid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .onBackpressureBuffer()
                .doOnNext(listChat -> {
                    for (ChatEntity chat : listChat) {
                        if (chat.getUidChat() != null && !isChatAlreadyObserved(chat.getUidChat())) {
                            Log.d(TAG, "Nouveau chat a écouté " + chat.getUidChat());

                            // Création de listener pour chacun de ces chats
                            Query query = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_MESSAGES_REF).child(chat.getUidChat()).orderByChild("timestamp");
                            query.addChildEventListener(messageChildListener);

                            // Ajout de la query et du listener à notre liste
                            listChatQueryListener.put(chat.getUidChat(), query);
                        }
                    }
                })
                .subscribe();
    }

    private boolean isChatAlreadyObserved(String chatUid) {
        for (Map.Entry<String, Query> entry : listChatQueryListener.entrySet()) {
            String uidChat = entry.getKey();
            if (uidChat.equals(chatUid)) {
                return true;
            }
        }
        return false;
    }

    private void clearMessageListener() {
        for (Map.Entry<String, Query> entry : listChatQueryListener.entrySet()) {
            Query query = entry.getValue();
            if (query != null) {
                query.removeEventListener(messageChildListener);
            }
        }
    }
}
