package oliweb.nc.oliweb.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public abstract class AnnonceWithPhotosDao {
    @Transaction
    @Query("SELECT * FROM annonce WHERE idAnnonce = :idAnnonce")
    public abstract LiveData<AnnoncePhotos> findById(Long idAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uidUser = :uuidUtilisateur AND statut NOT IN ('TO_DELETE', 'DELETED', 'FAILED_TO_DELETE') AND favorite <> 1")
    public abstract LiveData<List<AnnoncePhotos>> findActiveAnnonceByUidUser(String uuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uidUserFavorite = :uuidUtilisateur AND favorite = 1")
    public abstract LiveData<List<AnnoncePhotos>> findFavoritesByUidUser(String uuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uid = :uidAnnonce AND uidUserFavorite = :uidUser AND favorite = 1")
    public abstract Maybe<AnnoncePhotos> findFavoriteAnnonceByUidAnnonce(String uidUser, String uidAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE idAnnonce = :idAnnonce")
    public abstract LiveData<AnnoncePhotos> findLiveById(Long idAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uid = :uidAnnonce AND favorite = 0")
    public abstract LiveData<AnnoncePhotos> findByUid(String uidAnnonce);
}
