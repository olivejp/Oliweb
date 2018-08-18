package oliweb.nc.oliweb.repository.local;

import android.arch.lifecycle.LiveData;
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
        return this.utilisateurDao.findByUuid(uuidUtilisateur);
    }

    public Maybe<UserEntity> findMaybeByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findMaybeByUuid(uuidUtilisateur);
    }

    public Single<AtomicBoolean> existByUid(String uidUser) {
        return Single.create(e -> utilisateurDao.countByUid(uidUser)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }

    public Flowable<UserEntity> getAllUtilisateursByStatus(List<String> status) {
        return utilisateurDao.getAllUtilisateursByStatus(status);
    }

    public Single<List<UserEntity>> findAllByStatus(List<String> status) {
        return utilisateurDao.findAllByStatus(status);
    }



}