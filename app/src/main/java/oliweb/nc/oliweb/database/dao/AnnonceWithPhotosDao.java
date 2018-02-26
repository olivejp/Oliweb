package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.entity.AnnoncePhotos;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public interface AnnonceWithPhotosDao {
    @Transaction
    @Query("SELECT * FROM annonce WHERE idAnnonce = :idAnnonce")
    LiveData<AnnoncePhotos> findById(Long idAnnonce);

    @Transaction
    @Query("SELECT * FROM annonce WHERE UuidUtilisateur = :uuidUtilisateur AND statut NOT IN ('TO_DELETE', 'DELETED', 'FAILED_TO_DELETE')")
    LiveData<List<AnnoncePhotos>> findActiveAnnonceByUidUtilisateur(String uuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM annonce WHERE statut = :statut")
    Maybe<List<AnnoncePhotos>> getAllAnnonceByStatus(String statut);

}
