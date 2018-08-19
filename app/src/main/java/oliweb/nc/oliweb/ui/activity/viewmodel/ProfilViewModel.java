package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;
import oliweb.nc.oliweb.service.sync.SyncService;
import oliweb.nc.oliweb.system.broadcast.NetworkReceiver;
import oliweb.nc.oliweb.system.dagger.component.DaggerDatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.DaggerFirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.component.DatabaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseRepositoriesComponent;
import oliweb.nc.oliweb.system.dagger.component.FirebaseServicesComponent;
import oliweb.nc.oliweb.system.dagger.module.ContextModule;

public class ProfilViewModel extends AndroidViewModel {

    private UserRepository userRepository;
    private FirebaseChatRepository firebaseChatRepository;
    private FirebaseAnnonceRepository firebaseAnnonceRepository;
    private FirebaseMessageService firebaseMessageService;

    public ProfilViewModel(@NonNull Application application) {
        super(application);

        ContextModule contextModule = new ContextModule(application);
        DatabaseRepositoriesComponent component = DaggerDatabaseRepositoriesComponent.builder().contextModule(contextModule).build();
        FirebaseServicesComponent componentFbServices = DaggerFirebaseServicesComponent.builder().contextModule(contextModule).build();
        FirebaseRepositoriesComponent componentFb = DaggerFirebaseRepositoriesComponent.builder().build();

        firebaseMessageService = componentFbServices.getFirebaseMessageService();

        userRepository = component.getUserRepository();
        firebaseChatRepository = componentFb.getFirebaseChatRepository();
        firebaseAnnonceRepository = componentFb.getFirebaseAnnonceRepository();
    }

    public LiveData<Long> getFirebaseUserNbMessagesCount(String uidUser) {
        return this.firebaseMessageService.getCountMessageByUidUser(uidUser);
    }

    public LiveData<Long> getFirebaseUserNbChatsCount(String uidUser) {
        return this.firebaseChatRepository.getCountChatByUidUser(uidUser);
    }

    public LiveData<Long> getFirebaseUserNbAnnoncesCount(String uidUser) {
        return this.firebaseAnnonceRepository.getCountAnnonceByUidUser(uidUser);
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
