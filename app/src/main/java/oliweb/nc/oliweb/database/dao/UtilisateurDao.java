package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
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
    @Query("SELECT * FROM utilisateur WHERE UuidUtilisateur = :UuidUtilisateur")
    LiveData<UtilisateurEntity> findByUuid(String UuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE UuidUtilisateur = :UuidUtilisateur")
    Single<UtilisateurEntity> findSingleByUuid(String UuidUtilisateur);

}
