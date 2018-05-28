package oliweb.nc.oliweb.service.sync.listener;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.database.repository.local.ChatRepository;
import oliweb.nc.oliweb.database.repository.local.MessageRepository;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;
import oliweb.nc.oliweb.database.repository.local.UtilisateurRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.service.sync.deleter.AnnonceFirebaseDeleter;
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

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Démarrage du service DatabaseSyncListenerService");

        // Récupération de l'UID de l'utilisateur
        if (intent.getStringExtra(CHAT_SYNC_UID_USER) != null && !intent.getStringExtra(CHAT_SYNC_UID_USER).isEmpty()) {
            String uidUser = intent.getStringExtra(CHAT_SYNC_UID_USER);

            ChatRepository chatRepository = ChatRepository.getInstance(this);
            MessageRepository messageRepository = MessageRepository.getInstance(this);
            AnnonceRepository annonceRepository = AnnonceRepository.getInstance(this);
            UtilisateurRepository utilisateurRepository = UtilisateurRepository.getInstance(this);
            PhotoRepository photoRepository = PhotoRepository.getInstance(this);
            AnnonceFirebaseSender annonceFirebaseSender = AnnonceFirebaseSender.getInstance(this);
            AnnonceFirebaseDeleter annonceFirebaseDeleter = AnnonceFirebaseDeleter.getInstance(this);
            PhotoFirebaseSender photoFirebaseSender = PhotoFirebaseSender.getInstance(this);
            MessageFirebaseSender messageFirebaseSender = MessageFirebaseSender.getInstance(this);
            ChatFirebaseSender chatFirebaseSender = ChatFirebaseSender.getInstance(this);
            FirebaseUserRepository firebaseUserRepository = FirebaseUserRepository.getInstance();

            // SENDERS
            // Envoi toutes les annonces
            disposables.add(annonceRepository.findFlowableByUidUserAndStatusIn(uidUser, Utility.allStatusToSend())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .doOnNext(annonceFirebaseSender::processToSendAnnonceToFirebase)
                    .subscribe());

            // Envoi toutes les photos
            disposables.add(photoRepository.getAllPhotosByUidUserAndStatus(uidUser, Utility.allStatusToSend())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .doOnNext(photoFirebaseSender::sendPhotoToRemoteAndUpdateAnnonce)
                    .subscribe());

            // Envoi tous les chats
            disposables.add(chatRepository.findFlowableByUidUserAndStatusIn(uidUser, Utility.allStatusToSend())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .doOnNext(chatFirebaseSender::sendNewChat)
                    .subscribe());

            // Envoi tous les messages
            disposables.add(messageRepository.findFlowableByStatusAndUidChatNotNull(Utility.allStatusToSend())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .toObservable()
                    .flatMapIterable(list -> list)
                    .switchMap(messageFirebaseSender::sendMessage)
                    .subscribe());

            // Envoi tous les utilisateurs
            disposables.add(utilisateurRepository.getAllUtilisateursByStatus(Utility.allStatusToSend())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                    .flatMapSingle(utilisateur -> firebaseUserRepository.insertUserIntoFirebase(utilisateur)
                            .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                            .doOnSuccess(success -> {
                                if (success.get()) {
                                    Log.d(TAG, "insertUserIntoFirebase successfully send user : " + utilisateur);
                                    utilisateur.setStatut(StatusRemote.SEND);
                                    utilisateurRepository.singleSave(utilisateur)
                                            .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                                            .subscribe();
                                }
                            })
                    )
                    .subscribe());

            // DELETERS
            disposables.add(annonceRepository.findFlowableByUidUserAndStatusIn(uidUser, Utility.allStatusToDelete())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .doOnNext(annonceFirebaseDeleter::processToDeleteAnnonce)
                    .subscribe());

            disposables.add(photoRepository.getAllPhotosByStatus(Utility.allStatusToDelete())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                    .toObservable()
                    .map(photoEntity ->
                            annonceFirebaseDeleter.deleteOnePhoto(photoEntity).toObservable()
                                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                                    .switchMap(atomicBoolean -> annonceRepository.findById(photoEntity.getIdAnnonce()).toObservable())
                                    .filter(annonceEntity -> annonceEntity.getStatut() == StatusRemote.SEND)
                                    .switchMap(annonceFirebaseSender::convertToFullAndSendToFirebase)
                                    .subscribe()
                    )
                    .subscribe());
        } else {
            // Si pas d UID user on sort tout de suite du service
            stopSelf();
        }


        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Stop DatabaseSyncListenerService Bye bye");
        disposables.dispose();
        super.onDestroy();
    }
}
