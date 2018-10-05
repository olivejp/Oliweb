package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
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
    @Query("SELECT * FROM annonce WHERE uidUserFavorite = :uuidUtilisateur AND favorite = 1")
    public abstract LiveData<List<AnnonceFull>> findFavoritesByUidUser(String uuidUtilisateur);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE uidUserFavorite = :uuidUtilisateur AND uid = :uidAnnonce AND favorite = 1")
    public abstract LiveData<Integer> findFavoritesByUidUserAndByUidAnnonce(String uuidUtilisateur, String uidAnnonce);
}
