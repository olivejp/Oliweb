package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.UtilisateurDao;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class UtilisateurRepository extends AbstractRepository<UtilisateurEntity, Long> {
    private static final String TAG = UtilisateurRepository.class.getName();
    private static UtilisateurRepository instance;
    private UtilisateurDao utilisateurDao;

    private UtilisateurRepository(Context context) {
        super(context);
        this.utilisateurDao = this.db.getUtilisateurDao();
        this.dao = utilisateurDao;
    }

    public static synchronized UtilisateurRepository getInstance(Context context) {
        if (instance == null) {
            instance = new UtilisateurRepository(context);
        }
        return instance;
    }

    public LiveData<UtilisateurEntity> findByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findByUuid(uuidUtilisateur);
    }

    public Maybe<UtilisateurEntity> findSingleByUid(String uuidUtilisateur) {
        return this.utilisateurDao.findSingleByUuid(uuidUtilisateur);
    }

    public Single<AtomicBoolean> existByUid(String uidUser) {
        return Single.create(e -> utilisateurDao.countByUid(uidUser)
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }
}
