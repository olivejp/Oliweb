package oliweb.nc.oliweb.service;

import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;

import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.converter.UtilisateurConverter;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.utility.LiveDataOnce;
import oliweb.nc.oliweb.utility.Utility;

/**
 * Created by orlanth23 on 18/08/2018.
 */
@Singleton
public class UserService {

    private UserRepository userRepository;
    private FirebaseUserRepository firebaseUserRepository;

    @Inject
    public UserService(UserRepository userRepository, FirebaseUserRepository firebaseUserRepository) {
        this.userRepository = userRepository;
        this.firebaseUserRepository = firebaseUserRepository;
    }

    /**
     * @param firebaseUser user data that we want to save.
     * @return false if is an update, true for a creation.
     */
    public LiveDataOnce<AtomicBoolean> saveUserFromFirebase(FirebaseUser firebaseUser) {
        return observer -> userRepository.findSingleByUid(firebaseUser.getUid())
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> observer.onChanged(new AtomicBoolean(false)))
                .doOnSuccess(utilisateurEntity -> saveUser(observer, utilisateurEntity, false))
                .doOnComplete(() -> {
                    UserEntity userEntity = UtilisateurConverter.convertFbToEntity(firebaseUser);
                    saveUser(observer, userEntity, true);
                })
                .subscribe();
    }

    @NonNull
    private Disposable saveUser(Observer<AtomicBoolean> observer, UserEntity utilisateurEntity, boolean isAnCreation) {
        return firebaseUserRepository.getToken()
                .doOnSuccess(token -> {
                    utilisateurEntity.setTokenDevice(token);
                    utilisateurEntity.setDateLastConnexion(Utility.getNowInEntityFormat());
                    utilisateurEntity.setStatut(StatusRemote.TO_SEND);
                    userRepository.singleSave(utilisateurEntity)
                            .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                            .doOnError(e -> observer.onChanged(new AtomicBoolean(false)))
                            .doOnSuccess(utilisateurEntity1 -> observer.onChanged(new AtomicBoolean(isAnCreation)))
                            .subscribe();
                })
                .doOnError(e -> observer.onChanged(new AtomicBoolean(false)))
                .subscribe();
    }
}
