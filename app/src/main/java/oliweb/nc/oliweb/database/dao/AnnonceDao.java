package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

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
    @Query("SELECT * FROM annonce WHERE idCategorie = :idCategorie")
    Maybe<List<AnnonceEntity>> findByIdCategorie(Long idCategorie);

    @Transaction
    @Query("SELECT * FROM annonce WHERE UuidUtilisateur = :UuidUtilisateur")
    Maybe<List<AnnonceEntity>> findByUuidUtilisateur(String UuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM annonce WHERE statut = :status")
    Maybe<List<AnnonceEntity>> getAllAnnonceByStatus(String status);


    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE statut = :status")
    Single<Integer> countAllAnnoncesByStatus(String status);


    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE UuidUtilisateur = :uidUtilisateur AND UUID = :uidAnnonce")
    Single<Integer> existByUidUtilisateurAndUidAnnonce(String uidUtilisateur, String uidAnnonce);

    @Transaction
    @Query("SELECT COUNT(*) FROM annonce WHERE statut = :status AND UuidUtilisateur = :uidUtilisateur")
    LiveData<Integer> getAllAnnonceByStatusAndByUidUser(String status, String uidUtilisateur);
}
