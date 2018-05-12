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

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.MessageConverter;
import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.utility.Constants;
import oliweb.nc.oliweb.utility.Utility;

public class ChatSyncService extends Service {

    private static final String TAG = ChatSyncService.class.getName();

    public static final String CHAT_SYNC_UID_USER = "CHAT_SYNC_UID_USER";

    private ChatRepository chatRepository;
    private MessageRepository messageRepository;
    private FirebaseChatRepository firebaseChatRepository;

    public ChatSyncService() {
        chatRepository = ChatRepository.getInstance(this);
        messageRepository = MessageRepository.getInstance(this);
        firebaseChatRepository = FirebaseChatRepository.getInstance(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String uidUser = intent.getStringExtra(CHAT_SYNC_UID_USER);

        // Récupération de tous les chats dans lesquels je suis identifié
        firebaseChatRepository.sync(uidUser);

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
                                if (message != null) {

                                    // Recherche si le message existe déjà en base
                                    messageRepository.findSingleByUid(message.getUidMessage())
                                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                            .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                                            .doOnComplete(() -> {

                                                // Conversion d'un nouveau message
                                                MessageEntity messageEntity = MessageConverter.convertDtoToEntity(chat.getIdChat(), message);

                                                // Enregistrement du nouveau message
                                                firebaseChatRepository.saveMessageIfNotExist(messageEntity);
                                            })
                                            .subscribe();
                                }
                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                            }

                            @Override
                            public void onChildRemoved(DataSnapshot dataSnapshot) {
                                // Suppression du message de la db locale
                                MessageFirebase message = dataSnapshot.getValue(MessageFirebase.class);
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
        return START_NOT_STICKY;
    }


}
