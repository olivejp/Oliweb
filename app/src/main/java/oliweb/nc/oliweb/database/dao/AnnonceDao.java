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
}
