package oliweb.nc.oliweb.database.repository.local;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.OliwebDatabase;
import oliweb.nc.oliweb.database.dao.AnnonceFullDao;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
import oliweb.nc.oliweb.database.entity.StatusRemote;

/**
 * Created by 2761oli on 20/02/2018.
 */

public class AnnonceFullRepository {
    private static AnnonceFullRepository INSTANCE;
    private AnnonceFullDao annonceFullDao;

    private AnnonceFullRepository(Context context) {
        OliwebDatabase db = OliwebDatabase.getInstance(context);
        this.annonceFullDao = db.getAnnonceFullDao();
    }

    public static synchronized AnnonceFullRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new AnnonceFullRepository(context);
        }
        return INSTANCE;
    }

    public Single<AnnonceFull> findAnnoncesByIdAnnonce(long idAnnonce) {
        return this.annonceFullDao.findSingleByIdAnnonce(idAnnonce);
    }

    public Maybe<List<AnnonceFull>> getAllAnnoncesByStatus(StatusRemote... status) {
        AtomicInteger countSuccess = new AtomicInteger();
        return Maybe.create(e -> {
            ArrayList<AnnonceFull> listAnnonceFull = new ArrayList<>();
            countSuccess.set(0);
            for (StatusRemote statut : status) {
                this.annonceFullDao.getAllAnnonceByStatus(statut.getValue())
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(e::onError)
                        .doOnSuccess(annonceFulls -> {
                            listAnnonceFull.addAll(annonceFulls);
                            countSuccess.getAndIncrement();
                            if (countSuccess.get() == status.length) {
                                e.onSuccess(listAnnonceFull);
                                e.onComplete();
                            }
                        })
                        .subscribe();
            }
        });
    }
}
