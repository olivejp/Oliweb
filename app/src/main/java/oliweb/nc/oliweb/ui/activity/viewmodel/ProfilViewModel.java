package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.dagger.component.DaggerFirebaseRepositoriesComponent;
import oliweb.nc.oliweb.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.dagger.component.FirebaseRepositoriesComponent;
import oliweb.nc.oliweb.dagger.module.ContextModule;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.database.repository.local.UserRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseChatRepository;
import oliweb.nc.oliweb.firebase.repository.FirebaseUserRepository;
import oliweb.nc.oliweb.service.sync.SyncService;

public class ProfilViewModel extends AndroidViewModel {

    private UserRepository userRepository;
    private FirebaseChatRepository firebaseChatRepository;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private FirebaseUserRepository firebaseUserRepository;

    public ProfilViewModel(@NonNull Application application) {
        super(application);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder()
                .contextModule(new ContextModule(application))
                .build();

        FirebaseRepositoriesComponent componentFb = DaggerFirebaseRepositoriesComponent.builder()
                .contextModule(new ContextModule(application))
                .build();
        userRepository = component.getUserRepository();
        firebaseChatRepository = componentFb.getFirebaseChatRepository();
        firebaseAnnonceRepository = componentFb.getFirebaseAnnonceRepository();
        firebaseUserRepository = componentFb.getFirebaseUserRepository();

    }

    public LiveData<Long> getFirebaseUserNbMessagesCount(String uidUser) {
        return this.firebaseChatRepository.getCountMessageByUidUser(uidUser);
    }

    public LiveData<Long> getFirebaseUserNbChatsCount(String uidUser) {
        return this.firebaseChatRepository.getCountChatByUidUser(uidUser);
    }

    public LiveData<Long> getFirebaseUserNbAnnoncesCount(String uidUser) {
        return this.firebaseAnnonceRepository.getCountAnnonceByUidUser(uidUser);
    }

    public LiveData<UserEntity> getFirebaseUser(String uidUser) {
        return this.firebaseUserRepository.getLiveUtilisateurByUid(uidUser);
    }

    public LiveData<UserEntity> getUtilisateurByUid(String uidUser) {
        return this.userRepository.findByUid(uidUser);
    }

    public Single<AtomicBoolean> saveUtilisateur(UserEntity userEntity) {
        return Single.create(emitter -> {
            userEntity.setStatut(StatusRemote.TO_SEND);
            this.userRepository.singleSave(userEntity)
                    .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                    .doOnError(emitter::onError)
                    .doOnSuccess(utilisateurEntity1 -> {
                        if (NetworkReceiver.checkConnection(getApplication())) {
                            SyncService.launchSynchroForUser(getApplication());
                        }
                        emitter.onSuccess(new AtomicBoolean(true));
                    })
                    .subscribe();
        });
    }
}
