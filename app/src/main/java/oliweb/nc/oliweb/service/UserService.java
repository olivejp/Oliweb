package oliweb.nc.oliweb.service;

import android.arch.lifecycle.Observer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import oliweb.nc.oliweb.database.converter.UserConverter;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;
import oliweb.nc.oliweb.utility.CustomLiveData;

/**
 * Created by orlanth23 on 18/08/2018.
 */
@Singleton
public class UserService {

    private UserRepository userRepository;
    private FirebaseUserRepository firebaseUserRepository;

    private Scheduler processScheduler;
    private Scheduler androidScheduler;

    @Inject
    public UserService(UserRepository userRepository,
                       FirebaseUserRepository firebaseUserRepository,
                       @Named("processScheduler") Scheduler processScheduler,
                       @Named("androidScheduler") Scheduler androidScheduler) {
        this.userRepository = userRepository;
        this.firebaseUserRepository = firebaseUserRepository;
        this.processScheduler = processScheduler;
        this.androidScheduler = androidScheduler;
    }

    /**
     * @param firebaseUser user data that we want to save.
     * @return false if is an update, true for a creation.
     */
    public CustomLiveData<AtomicBoolean> saveUserFromFirebase(FirebaseUser firebaseUser) {
        return new CustomLiveData<AtomicBoolean>() {
            @Override
            public void observeOnce(Observer<AtomicBoolean> observer) {
                super.observeOnce(observer);
                userRepository.findMaybeByUid(firebaseUser.getUid())
                        .doOnError(e -> observer.onChanged(new AtomicBoolean(false)))
                        .map(userEntity -> saveUser(observer, userEntity, firebaseUser, false))
                        .doOnComplete(() -> saveUser(observer, null, firebaseUser, true))
                        .subscribeOn(processScheduler).observeOn(androidScheduler)
                        .subscribe();
            }
        };
    }

    @NonNull
    private Disposable saveUser(Observer<AtomicBoolean> observer, @Nullable UserEntity utilisateurEntity, FirebaseUser firebaseUser, boolean isAnCreation) {
        return firebaseUserRepository.getToken()
                .map(token -> UserConverter.convertFbToEntity(firebaseUser, token, isAnCreation))
                .map(userEntity -> {
                    if (utilisateurEntity != null) {
                        userEntity.setIdUser(utilisateurEntity.getIdUser());
                    }
                    return userEntity;
                })
                .flatMap(userRepository::singleSave)
                .doOnError(e -> observer.onChanged(new AtomicBoolean(false)))
                .subscribeOn(processScheduler).observeOn(androidScheduler)
                .doOnSuccess(userSaved -> observer.onChanged(new AtomicBoolean(isAnCreation)))
                .subscribe();
    }
}
