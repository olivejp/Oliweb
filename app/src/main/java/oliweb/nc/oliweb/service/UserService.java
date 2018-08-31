package oliweb.nc.oliweb.service;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.Disposable;
import oliweb.nc.oliweb.database.converter.UserConverter;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;
import oliweb.nc.oliweb.repository.firebase.FirebaseUserRepository;
import oliweb.nc.oliweb.repository.local.UserRepository;

/**
 * Created by orlanth23 on 18/08/2018.
 */
@Singleton
public class UserService {

    private static final String TAG = UserService.class.getCanonicalName();

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
    public Single<UserEntity> saveSingleUserFromFirebase(FirebaseUser firebaseUser) {
        return Single.create(emitter ->
                userRepository.findMaybeByUid(firebaseUser.getUid())
                        .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                        .map(userEntity -> saveUserFromFirebase(emitter, userEntity, firebaseUser, false))
                        .doOnComplete(() -> saveUserFromFirebase(emitter, null, firebaseUser, true))
                        .subscribeOn(processScheduler).observeOn(androidScheduler)
                        .subscribe()
        );
    }

    public void saveUserToFavorite(UserEntity user) {
        userRepository.findMaybeFavoriteByUid(user.getUid())
                .subscribeOn(processScheduler).observeOn(processScheduler)
                .switchIfEmpty(saveUserFavorite(user))
                .doOnError(throwable -> Log.e(TAG, throwable.getLocalizedMessage(), throwable))
                .subscribe();
    }

    private Single<UserEntity> saveUserFavorite(UserEntity userEntity) {
        userEntity.setFavorite(1);
        userEntity.setStatut(StatusRemote.NOT_TO_SEND);
        return userRepository.singleSave(userEntity)
                .doOnSuccess(userSaved -> Log.d(TAG, String.format("Utilisateur créé dans les favoris %s", userSaved)));
    }

    @NonNull
    private Disposable saveUserFromFirebase(SingleEmitter<UserEntity> emitter,
                                            @Nullable UserEntity utilisateurEntity,
                                            FirebaseUser firebaseUser,
                                            boolean isAnCreation) {
        return firebaseUserRepository.getToken()
                .map(token -> UserConverter.convertFbToEntity(firebaseUser, token, isAnCreation))
                .map(userEntity -> {
                    if (utilisateurEntity != null) {
                        userEntity.setIdUser(utilisateurEntity.getIdUser());
                    }
                    return userEntity;
                })
                .flatMap(userRepository::singleSave)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .subscribeOn(processScheduler).observeOn(androidScheduler)
                .doOnSuccess(emitter::onSuccess)
                .subscribe();
    }
}
