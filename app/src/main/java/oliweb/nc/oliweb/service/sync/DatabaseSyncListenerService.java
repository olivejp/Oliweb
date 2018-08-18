package oliweb.nc.oliweb.service.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.AnnonceRepository;
import oliweb.nc.oliweb.repository.local.ChatRepository;
import oliweb.nc.oliweb.repository.local.MessageRepository;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseDeleter;
import oliweb.nc.oliweb.service.firebase.AnnonceFirebaseSender;
import oliweb.nc.oliweb.service.firebase.FirebaseChatService;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;
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

        // Condition de garde : Récupération de l'UID de l'utilisateur
        if (intent.getStringExtra(CHAT_SYNC_UID_USER) == null || intent.getStringExtra(CHAT_SYNC_UID_USER).isEmpty()) {
            stopSelf();
        }

        String uidUser = intent.getStringExtra(CHAT_SYNC_UID_USER);

        ContextModule contextModule = new ContextModule(this);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();
        FirebaseServicesComponent componentFbService = DaggerFirebaseServicesComponent.builder().contextModule(contextModule).build();
        FirebaseRepositoriesComponent componentFb = DaggerFirebaseRepositoriesComponent.builder().build();

        ChatRepository chatRepository = component.getChatRepository();
        MessageRepository messageRepository = component.getMessageRepository();
        AnnonceRepository annonceRepository = component.getAnnonceRepository();
        UserRepository userRepository = component.getUserRepository();
        PhotoRepository photoRepository = component.getPhotoRepository();
        AnnonceFirebaseSender annonceFirebaseSender = componentFbService.getAnnonceFirebaseSender();
        AnnonceFirebaseDeleter annonceFirebaseDeleter = componentFbService.getAnnonceFirebaseDeleter();
        FirebaseMessageService firebaseMessageService = componentFbService.getFirebaseMessageService();
        FirebaseChatService firebaseChatService = componentFbService.getFirebaseChatService();
        FirebaseUserRepository firebaseUserRepository = componentFb.getFirebaseUserRepository();

        // Suppression des listeners
        disposables.clear();

        // SENDERS
        // Envoi toutes les annonces
        disposables.add(annonceRepository.findFlowableByUidUserAndStatusIn(uidUser, Utility.allStatusToSend())
                .distinct()
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnNext(annonceFirebaseSender::processToSendAnnonceToFirebase)
                .subscribe());

        // Envoi tous les chats
        disposables.add(chatRepository.findFlowableByUidUserAndStatusIn(uidUser, Utility.allStatusToSend())
                .distinct()
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnNext(firebaseChatService::sendNewChat)
                .subscribe());

        // Envoi tous les messages
        disposables.add(messageRepository.findFlowableByStatusAndUidChatNotNull(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable()
                .flatMapIterable(list -> list)
                .distinct()
                .switchMap(firebaseMessageService::sendMessage)
                .subscribe());

        // Envoi tous les utilisateurs
        disposables.add(userRepository.getAllUtilisateursByStatus(Utility.allStatusToSend())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                .flatMapSingle(utilisateur -> firebaseUserRepository.insertUserIntoFirebase(utilisateur)
                        .doOnError(exception -> Log.e(TAG, exception.getLocalizedMessage(), exception))
                        .doOnSuccess(success -> {
                            if (success.get()) {
                                Log.d(TAG, "insertUserIntoFirebase successfully send user : " + utilisateur);
                                utilisateur.setStatut(StatusRemote.SEND);
                                userRepository.singleSave(utilisateur)
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

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Stop DatabaseSyncListenerService Bye bye");
        disposables.dispose();
        super.onDestroy();
    }
}
