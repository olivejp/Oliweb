package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.AnnonceDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class AnnonceRepository extends AbstractRepository<AnnonceEntity, Long> {

    private static final String TAG = AnnonceRepository.class.getName();

    private static AnnonceRepository instance;
    private AnnonceDao annonceDao;

    private AnnonceRepository(Context context) {
        super(context);
        this.dao = this.db.getAnnonceDao();
        this.annonceDao = (AnnonceDao) this.dao;
    }

    public static synchronized AnnonceRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AnnonceRepository(context);
        }
        return instance;
    }

    public LiveData<AnnonceEntity> findLiveById(long idAnnonce) {
        Log.d(TAG, "Starting findLiveById " + idAnnonce);
        return this.annonceDao.findLiveById(idAnnonce);
    }

    public LiveData<AnnonceEntity> findByUid(String uidAnnonce) {
        Log.d(TAG, "Starting findByUid " + uidAnnonce);
        return this.annonceDao.findByUid(uidAnnonce);
    }

    public Observable<AnnonceEntity> getAllAnnonceByStatus(List<String> status) {
        Log.d(TAG, "Starting getAllAnnonceByStatus " + status);
        return Observable.create(emitter -> {
            this.annonceDao.getAllAnnonceByStatus(status)
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnError(emitter::onError)
                    .doOnSuccess(listAnnonceStatus -> {
                        for (AnnonceEntity annonce : listAnnonceStatus) {
                            emitter.onNext(annonce);
                        }
                        emitter.onComplete();
                    })
                    .subscribe();
        });
    }

    public Flowable<Integer> countFlowableAllAnnoncesByStatus(String status) {
        return this.annonceDao.countFlowableAllAnnoncesByStatus(status);
    }

    public LiveData<Integer> countAllAnnoncesByUser(String uidUser, List<String> statusToAvoid) {
        return this.annonceDao.countAllAnnoncesByUser(uidUser, statusToAvoid);
    }

    public LiveData<Integer> countAllFavoritesByUser(String uidUser) {
        return this.annonceDao.countAllFavoritesByUser(uidUser);
    }

    public Single<Integer> countByUidUtilisateurAndUidAnnonce(String uidUtilisateur, String uidAnnonce) {
        Log.d(TAG, "Starting countByUidUtilisateurAndUidAnnonce uidUtilisateur : " + uidUtilisateur + " uidAnnonce : " + uidAnnonce);
        return this.annonceDao.existByUidUtilisateurAndUidAnnonce(uidUtilisateur, uidAnnonce);
    }

    public Single<Integer> isAnnonceFavorite(String uidAnnonce) {
        return this.annonceDao.isAnnonceFavorite(uidAnnonce);
    }
}
