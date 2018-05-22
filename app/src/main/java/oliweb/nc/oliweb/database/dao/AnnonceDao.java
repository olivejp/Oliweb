package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

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
    @Query("SELECT * FROM annonce WHERE uid = :uidAnnonce")
    public abstract Maybe<AnnonceEntity> findSingleByUid(String uidAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE statut IN (:status)")
    public abstract Maybe<List<AnnonceEntity>> getAllAnnonceByStatus(List<String> status);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE statut = :status")
    public abstract Flowable<Integer> countFlowableAllAnnoncesByStatus(String status);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE uidUser = :uidUser AND statut NOT IN (:statutToAvoid)")
    public abstract LiveData<Integer> countAllAnnoncesByUser(String uidUser, List<String> statutToAvoid);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE uidUser = :uidUser AND favorite = 1")
    public abstract LiveData<Integer> countAllFavoritesByUser(String uidUser);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE uidUser = :uidUtilisateur AND uid = :uidAnnonce")
    public abstract Single<Integer> existByUidUtilisateurAndUidAnnonce(String uidUtilisateur, String uidAnnonce);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE uid = :uidAnnonce AND favorite = 1")
    public abstract Single<Integer> isAnnonceFavorite(String uidAnnonce);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE uid = :uidAnnonce AND favorite = 1 AND uidUser <> :uidUser")
    public abstract Single<Integer> isAnnonceFavoriteNotTheAuthor(String uidUser, String uidAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uid = :uidAnnonce AND favorite = 0")
    public abstract LiveData<AnnonceEntity> findByUid(String uidAnnonce);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE idAnnonce = :idAnnonce")
    public abstract Single<Integer> countById(Long idAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uidUser = :uidUser AND statut IN (:statutList)")
    public abstract Maybe<List<AnnonceEntity>> getAllAnnoncesByUidUserAndStatus(String uidUser, List<String> statutList);

    @Transaction
    @Query("SELECT * FROM annonce WHERE uidUser = :uidUser AND statut IN (:status)")
    public abstract Flowable<AnnonceEntity> findFlowableByUidUserAndStatusIn(String uidUser, List<String> status);
}
