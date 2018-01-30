package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Maybe;
import oliweb.nc.oliweb.database.entity.UtilisateurEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public interface UtilisateurDao extends AbstractDao<UtilisateurEntity> {
    @Transaction
    @Query("SELECT * FROM utilisateur")
    Maybe<List<UtilisateurEntity>> getListUtilisateur();

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE idUtilisateur = :idUtilisateur")
    LiveData<UtilisateurEntity> findById(Long idUtilisateur);
}
