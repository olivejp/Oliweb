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
public interface AnnonceDao extends AbstractDao<AnnonceEntity> {
    @Transaction
    @Query("SELECT * FROM annonce WHERE idAnnonce = :idAnnonce")
    LiveData<AnnonceEntity> findById(Long idAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE idAnnonce = :idAnnonce")
    Single<AnnonceEntity> findSingleById(Long idAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE statut = :status")
    Maybe<List<AnnonceEntity>> getAllAnnonceByStatus(String status);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE statut = :status")
    Flowable<Integer> countFlowableAllAnnoncesByStatus(String status);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE UuidUtilisateur = :uidUser")
    LiveData<Integer> countAllAnnoncesByUser(String uidUser);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE UuidUtilisateur = :uidUser AND favorite = 1")
    LiveData<Integer> countAllFavoritesByUser(String uidUser);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE UuidUtilisateur = :uidUtilisateur AND UUID = :uidAnnonce")
    Single<Integer> existByUidUtilisateurAndUidAnnonce(String uidUtilisateur, String uidAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce")
    Single<List<AnnonceEntity>> getAll();

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE UUID = :uidAnnonce AND favorite = 1")
    Single<Integer> isAnnonceFavorite(String uidAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE UUID = :uidAnnonce AND favorite = 0")
    LiveData<AnnonceEntity> findByUid(String uidAnnonce);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce")
    Single<Integer> count();

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE idAnnonce = :idAnnonce")
    Single<Integer> countById(Long idAnnonce);
}
