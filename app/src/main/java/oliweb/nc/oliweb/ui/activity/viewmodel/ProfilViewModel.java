package oliweb.nc.oliweb.ui.activity.viewmodel;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.annotation.NonNull;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.App;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseAnnonceRepository;
import oliweb.nc.oliweb.repository.firebase.FirebaseChatRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.service.firebase.FirebaseMessageService;

public class ProfilViewModel extends AndroidViewModel {

    @Inject
    UserRepository userRepository;

    @Inject
    FirebaseChatRepository firebaseChatRepository;

    @Inject
    FirebaseAnnonceRepository firebaseAnnonceRepository;

    @Inject
    FirebaseMessageService firebaseMessageService;

    public ProfilViewModel(@NonNull Application application) {
        super(application);
        ((App) application).getDatabaseRepositoriesComponent().inject(this);
        ((App) application).getFirebaseServicesComponent().inject(this);
        ((App) application).getFirebaseRepositoriesComponent().inject(this);
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

    public Single<UserEntity> markAsToSend(UserEntity userEntity) {
        return this.userRepository.markAsToSend(userEntity).subscribeOn(Schedulers.io());
    }
}
