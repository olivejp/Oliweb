package oliweb.nc.oliweb.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.AnnonceFull;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public abstract class AnnonceFullDao {

    @Transaction
    @Query("SELECT * FROM annonce WHERE idAnnonce = :idAnnonce")
    public abstract Single<AnnonceFull> findSingleByIdAnnonce(long idAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uidUser = :uidUser AND statut IN (:statutList)")
    public abstract Maybe<List<AnnonceFull>> getAllAnnoncesByUidUserAndStatus(String uidUser, List<String> statutList);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uidUser = :uidUser AND statut IN (:status)")
    public abstract Flowable<AnnonceFull> findFlowableByUidUserAndStatusIn(String uidUser, List<String> status);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uidUser = :uidUser")
    public abstract Single<List<AnnonceFull>> getAllAnnoncesByUidUser(String uidUser);
}
