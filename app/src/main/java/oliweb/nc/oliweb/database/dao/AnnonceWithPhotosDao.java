package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import oliweb.nc.oliweb.database.entity.AnnonceWithPhotos;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public interface AnnonceWithPhotosDao {
    @Transaction
    @Query("SELECT * FROM annonce WHERE idAnnonce = :idAnnonce")
    LiveData<AnnonceWithPhotos> findById(Long idAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE UuidUtilisateur = :uuidUtilisateur")
    LiveData<List<AnnonceWithPhotos>> findByUuidUtilisateur(String uuidUtilisateur);
}
