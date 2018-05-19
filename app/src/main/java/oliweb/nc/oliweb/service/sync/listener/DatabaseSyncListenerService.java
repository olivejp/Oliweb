package oliweb.nc.oliweb.service.sync.listener;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.service.sync.sender.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.ChatFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.MessageFirebaseSender;
import oliweb.nc.oliweb.service.sync.sender.PhotoFirebaseSender;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 14/05/2018.
 * This class will listen to the local database and send items to Firebase Database
 */
public class DatabaseSyncListenerService extends Service {

    private static final String TAG = DatabaseSyncListenerService.class.getName();

    public static final String CHAT_SYNC_UID_USER = "CHAT_SYNC_UID_USER";

    private Disposable disposableChatByStatus;
    private Disposable disposableMessageByStatus;
    private Disposable disposableAnnonceToSendByStatus;
    private Disposable disposablePhotoToSendByStatus;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Récupération de l'UID de l'utilisateur
        if (intent.getStringExtra(CHAT_SYNC_UID_USER) != null && !intent.getStringExtra(CHAT_SYNC_UID_USER).isEmpty()) {
            String uidUser = intent.getStringExtra(CHAT_SYNC_UID_USER);

            ChatRepository chatRepository = ChatRepository.getInstance(this);
            MessageRepository messageRepository = MessageRepository.getInstance(this);
            AnnonceRepository annonceRepository = AnnonceRepository.getInstance(this);
            PhotoRepository photoRepository = PhotoRepository.getInstance(this);
            AnnonceFirebaseSender annonceFirebaseSender = AnnonceFirebaseSender.getInstance(this);
            PhotoFirebaseSender photoFirebaseSender = PhotoFirebaseSender.getInstance(this);
            MessageFirebaseSender messageFirebaseSender = MessageFirebaseSender.getInstance(this);
            ChatFirebaseSender chatFirebaseSender = ChatFirebaseSender.getInstance(this);

            disposableAnnonceToSendByStatus = annonceRepository.findFlowableByUidUserAndStatusIn(uidUser, Utility.allStatusToSend())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .doOnNext(annonceFirebaseSender::sendAnnonceToRemoteDatabase)
                    .subscribe();

            disposablePhotoToSendByStatus = photoRepository.getAllPhotosByStatus(Utility.allStatusToSend())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .doOnNext(photoEntity -> photoFirebaseSender.sendPhotoToRemoteAndUpdateAnnonce(photoEntity).subscribe())
                    .subscribe();

            disposableChatByStatus = chatRepository.findFlowableByUidUserAndStatusIn(uidUser, Utility.allStatusToSend())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .doOnNext(chatFirebaseSender::sendNewChat)
                    .subscribe();

            // Cette souscription va écouter en permanence les messages pour l'UID user et dont le statut est A Envoyer
            disposableMessageByStatus = messageRepository.findFlowableByStatusAndUidChatNotNull(Utility.allStatusToSend())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .toObservable()
                    .flatMapIterable(list -> list)
                    .switchMap(messageFirebaseSender::sendMessage)
                    .subscribe();


            // TODO finir cette méthode
            //            disposableAnnonceToDeleteByStatus = annonceFullRepository.findFlowableByUidUserAndStatusIn(uidUser, Utility.allStatusToDelete())
            //                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
            //                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
            //                    .doOnNext(coreSync::sendAnnonceToRemoteDatabase)
            //                    .subscribe();

        } else {
            // Si pas d UID user on sort tout de suite du service
            stopSelf();
        }


        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Stop DatabaseSyncListenerService Bye bye");
        if (disposableAnnonceToSendByStatus != null) {
            disposableAnnonceToSendByStatus.dispose();
        }
        if (disposableChatByStatus != null) {
            disposableChatByStatus.dispose();
        }
        if (disposableMessageByStatus != null) {
            disposableMessageByStatus.dispose();
        }
        if (disposablePhotoToSendByStatus != null) {
            disposablePhotoToSendByStatus.dispose();
        }
        super.onDestroy();
    }
}
