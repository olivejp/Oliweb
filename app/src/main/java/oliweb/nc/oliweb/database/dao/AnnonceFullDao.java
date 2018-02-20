package oliweb.nc.oliweb.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.AnnonceFull;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public interface AnnonceFullDao {

    @Transaction
    @Query("SELECT * FROM annonce WHERE idAnnonce = :idAnnonce")
    Single<AnnonceFull> findSingleByIdAnnonce(long idAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE statut = :statut")
    Maybe<List<AnnonceFull>> getAllAnnonceByStatus(String statut);

}
