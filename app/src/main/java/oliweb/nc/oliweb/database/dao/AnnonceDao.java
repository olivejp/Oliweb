package oliweb.nc.oliweb.database.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public abstract class AnnonceDao implements AbstractDao<AnnonceEntity, Long> {

    @Override
    @Transaction
    @Query("SELECT * FROM annonce WHERE idAnnonce = :idAnnonce")
    public abstract Maybe<AnnonceEntity> findById(Long idAnnonce);

    @Override
    @Transaction
    @Query("SELECT * FROM annonce")
    public abstract Single<List<AnnonceEntity>> getAll();

    @Override
    @Transaction
    @Query("SELECT COUNT(*) FROM annonce")
    public abstract Single<Integer> count();

    @Transaction
    @Query("SELECT * FROM annonce WHERE idAnnonce = :idAnnonce")
    public abstract LiveData<AnnonceEntity> findLiveById(Long idAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uid = :uidAnnonce AND favorite = :favorite")
    public abstract Maybe<AnnonceEntity> findMaybeByUidAndFavorite(String uidAnnonce, int favorite);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uid = :uidAnnonce AND uidUser = :uidUser LIMIT 1")
    public abstract Maybe<AnnonceEntity> getMaybeByUidUserAndUidAnnonce(String uidAnnonce, String uidUser);

    @Transaction
    @Query("SELECT * FROM annonce WHERE statut IN (:status)")
    public abstract Single<List<AnnonceEntity>> getAllAnnonceByStatus(List<String> status);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE uidUser = :uidUser AND statut NOT IN (:statutToAvoid) AND favorite <> 1")
    public abstract LiveData<Integer> countAllAnnoncesByUser(String uidUser, List<String> statutToAvoid);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE uidUserFavorite = :uidUser AND favorite = 1")
    public abstract LiveData<Integer> countAllFavoritesByUser(String uidUser);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE uidUser = :uidUtilisateur AND uid = :uidAnnonce AND favorite <> 1")
    public abstract Single<Integer> existByUidUtilisateurAndUidAnnonce(String uidUtilisateur, String uidAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uid = :uidAnnonce AND favorite = 1 AND uidUserFavorite = :uidUser")
    public abstract Maybe<AnnonceEntity> getAnnonceFavoriteByUidUserAndUidAnnonce(String uidUser, String uidAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uid = :uidAnnonce AND favorite = 0")
    public abstract LiveData<AnnonceEntity> findByUid(String uidAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uidUser = :uidUser AND statut IN (:status)")
    public abstract Flowable<AnnonceEntity> findFlowableByUidUserAndStatusIn(String uidUser, List<String> status);

    @Transaction
    @Query("SELECT *  FROM annonce WHERE uidUser = :uidUser AND statut IN (:status)")
    public abstract Single<List<AnnonceEntity>> findSingleByUidUserAndStatusIn(String uidUser, List<String> status);

    @Transaction
    @Query("DELETE FROM annonce WHERE uidUserFavorite = :uidUser AND uid = :uidAnnonce AND favorite = 1")
    public abstract int deleteFromFavorite(String uidUser, String uidAnnonce);


}
