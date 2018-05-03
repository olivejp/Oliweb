package oliweb.nc.oliweb.database.repository.local;

import android.content.Context;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AnnonceFullDao;
import oliweb.nc.oliweb.database.entity.AnnonceFull;

/**
 * Created by 2761oli on 20/02/2018.
 */

public class AnnonceFullRepository {
    private static AnnonceFullRepository instance;
    private AnnonceFullDao annonceFullDao;

    private AnnonceFullRepository(Context context) {
        OliwebDatabase db = OliwebDatabase.getInstance(context);
        this.annonceFullDao = db.getAnnonceFullDao();
    }

    public static synchronized AnnonceFullRepository getInstance(Context context) {
        if (instance == null) {
            instance = new AnnonceFullRepository(context);
        }
        return instance;
    }

    public Single<AnnonceFull> findAnnoncesByIdAnnonce(long idAnnonce) {
        return this.annonceFullDao.findSingleByIdAnnonce(idAnnonce);
    }

    public Maybe<List<AnnonceFull>> getAllAnnoncesByStatus(List<String> status) {
        return this.annonceFullDao.getAllAnnonceByStatus(status);
    }

    public Observable<AnnonceFull> observeAllAnnoncesByStatus(List<String> status) {
        return Observable.create(e ->
                this.annonceFullDao.getAllAnnonceByStatus(status)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(e::onError)
                        .doOnSuccess(listAnnonceFull -> {
                            for (AnnonceFull annonceFull : listAnnonceFull) {
                                e.onNext(annonceFull);
                            }
                            e.onComplete();
                        })
                        .subscribe()
        );
    }
}
