package oliweb.nc.oliweb.service.sync;

import android.content.Context;
import android.util.Log;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.service.sync.sender.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.ChatFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.MessageFirebaseSender;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 18/12/2017.
 * <p>
 * This class contains the series of network calls to make to sync local db with firebase
 */
public class ScheduleSync {
    private static final String TAG = ScheduleSync.class.getName();

    private static ScheduleSync instance;

    private FirebaseUserRepository firebaseUserRepository;
    private UtilisateurRepository utilisateurRepository;
    private AnnonceRepository annonceRepository;
    private AnnonceFirebaseSender annonceFirebaseSender;
    private ChatRepository chatRepository;
    private MessageRepository messageRepository;
    private MessageFirebaseSender messageFirebaseSender;
    private ChatFirebaseSender chatFirebaseSender;

    private ScheduleSync() {
    }

    public static synchronized ScheduleSync getInstance(Context context) {
        if (instance == null) {
            instance = new ScheduleSync();
            instance.utilisateurRepository = UtilisateurRepository.getInstance(context);
            instance.firebaseUserRepository = FirebaseUserRepository.getInstance();
            instance.annonceRepository = AnnonceRepository.getInstance(context);
            instance.annonceFirebaseSender = AnnonceFirebaseSender.getInstance(context);
            instance.messageRepository = MessageRepository.getInstance(context);
            instance.chatRepository = ChatRepository.getInstance(context);
            instance.messageFirebaseSender = MessageFirebaseSender.getInstance(context);
            instance.chatFirebaseSender = ChatFirebaseSender.getInstance(context);
        }
        return instance;
    }

    void synchronize() {
        sendAnnonces();
        sendMessages();
        sendChats();
        sendUtilisateurs();
    }

    private void sendAnnonces() {
        annonceRepository.getAllAnnonceByStatus(Utility.allStatusToSend())
                .distinct()
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnNext(annonceFirebaseSender::processToSendAnnonceToFirebase)
                .subscribe();
    }


    private void sendChats() {
        chatRepository.findByStatusIn(Utility.allStatusToSend())
                .distinct()
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnNext(chatFirebaseSender::sendNewChat)
                .subscribe();
    }

    private void sendMessages() {
        messageRepository.findFlowableByStatusAndUidChatNotNull(Utility.allStatusToSend())
                .distinct()
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable()
                .flatMapIterable(list -> list)
                .switchMap(messageFirebaseSender::sendMessage)
                .subscribe();
    }

    private void sendUtilisateurs() {
        utilisateurRepository.getAllUtilisateursByStatus(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .flatMapSingle(utilisateur -> firebaseUserRepository.insertUserIntoFirebase(utilisateur)
                        .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                        .doOnSuccess(success -> {
                            if (success.get()) {
                                utilisateur.setStatut(StatusRemote.SEND);
                                utilisateurRepository.singleSave(utilisateur)
                                        .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                                        .subscribe();
                            }
                        })
                )
                .subscribe();
    }
}
