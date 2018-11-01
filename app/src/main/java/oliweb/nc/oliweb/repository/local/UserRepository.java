package oliweb.nc.oliweb.repository.local;

import androidx.lifecycle.LiveData;
import android.content.Context;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.UtilisateurDao;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.entity.UserEntity;

/**
 * Created by 2761oli on 29/01/2018.
 */
@Singleton
public class UserRepository extends AbstractRepository<UserEntity, Long> {
    private UtilisateurDao utilisateurDao;

    @Inject
    public UserRepository(Context context) {
        super(context);
        this.utilisateurDao = this.db.getUtilisateurDao();
        this.dao = utilisateurDao;
    }

    public LiveData<UserEntity> findByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findByUid(uuidUtilisateur);
    }

    public Maybe<UserEntity> findMaybeByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findMaybeByUid(uuidUtilisateur);
    }

    public Flowable<UserEntity> findFlowableByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findFlowableByUid(uuidUtilisateur);
    }

    public Maybe<UserEntity> findMaybeFavoriteByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findMaybeFavoriteByUid(uuidUtilisateur);
    }

    public Single<AtomicBoolean> existByUid(String uidUser) {
        return Single.create(e -> utilisateurDao.countByUid(uidUser)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }

    public Maybe<UserEntity> findMaybeByUidAndStatus(String uid, List<String> status) {
        return utilisateurDao.findMaybeByUidAndStatus(uid, status);
    }

    public Single<UserEntity> markAsToSend(UserEntity userEntity) {
        userEntity.setStatut(StatusRemote.TO_SEND);
        return this.singleSave(userEntity);
    }

    public Single<UserEntity> markAsSend(UserEntity userEntity) {
        userEntity.setStatut(StatusRemote.SEND);
        return this.singleSave(userEntity);
    }
}
