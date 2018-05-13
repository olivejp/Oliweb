package oliweb.nc.oliweb.service.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.ChatConverter;
import oliweb.nc.oliweb.database.converter.MessageConverter;
import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;

public class ChatSyncListenerService extends Service {

    private static final String TAG = ChatSyncListenerService.class.getName();

    public static final String CHAT_SYNC_UID_USER = "CHAT_SYNC_UID_USER";

    private ChatRepository chatRepository;
    private MessageRepository messageRepository;

    public ChatSyncListenerService() {
        chatRepository = ChatRepository.getInstance(this);
        messageRepository = MessageRepository.getInstance(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String uidUser = intent.getStringExtra(CHAT_SYNC_UID_USER);

        // Récupération des chats de l'utilisateur connecté
        listenForChat(uidUser);

        // Création d'observers pour écouter les nouveaux chats
        listenForMessage(uidUser);

        return START_NOT_STICKY;
    }

    /**
     * Ecoute tous les chats avec l'uid user comme membre et pour tout cela, va lancer
     *
     * @param uidUser
     */
    private void listenForChat(String uidUser) {
        Log.d(TAG, "Starting listenForChat uidUser : " + uidUser);
        FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_CHATS_REF).orderByChild("members/" + uidUser).equalTo(true)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        try {
                            if (dataSnapshot != null) {
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
                            }
                        } catch (Exception e1) {
                            Log.e(TAG, e1.getLocalizedMessage(), e1);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e(TAG, databaseError.getMessage());
                    }
                });
    }

    /**
     * Va créer un observer pour tout les chats présents en base
     * Si on rajoute un chat dans la base, cette méthode créera automatiquement un nouvel observer pour ce chat.
     *
     * @param uidUser
     */
    private void listenForMessage(String uidUser) {
        Log.d(TAG, "Starting listenForMessage uidUser : " + uidUser);
        // Récupération de la liste des chats pour l'utilisateur connecté et dont le statut n'est pas cloturé
        chatRepository.findFlowableByUidUserAndStatusNotIn(uidUser, Utility.allStatusToAvoid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnNext(chat -> {
                    if (chat.getUidChat() != null) {
                        // Création de listener pour chacun de ces chats
                        Query query = FirebaseDatabase.getInstance().getReference(Constants.FIREBASE_DB_MESSAGES_REF).child(chat.getUidChat()).orderByChild("timestamp");
                        query.addChildEventListener(new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                                MessageFirebase message = dataSnapshot.getValue(MessageFirebase.class);
                                Log.d(TAG, "Nouveau message reçu messageFirebase : " + message);
                                if (message != null) {
                                    messageRepository.saveMessageIfNotExist(MessageConverter.convertDtoToEntity(chat.getIdChat(), message));
                                }
                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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
                                            .doOnSuccess(messageRepository::delete)
                                            .subscribe();
                                }
                            }

                            @Override
                            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                })
                .subscribe();
    }

}
