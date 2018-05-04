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
public abstract class UtilisateurDao implements AbstractDao<UtilisateurEntity, Long> {

    @Override
    @Transaction
    @Query("SELECT * FROM utilisateur WHERE idUser = :idUtilisateur")
    public abstract Maybe<UtilisateurEntity> findById(Long idUtilisateur);

    @Override
    @Transaction
    @Query("SELECT * FROM utilisateur")
    public abstract Single<List<UtilisateurEntity>> getAll();

    @Override
    @Transaction
    @Query("SELECT COUNT(*) FROM utilisateur")
    public abstract Single<Integer> count();

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE uid = :UuidUtilisateur")
    public abstract Maybe<UtilisateurEntity> findById(String UuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE uid = :UuidUtilisateur")
    public abstract LiveData<UtilisateurEntity> findByUuid(String UuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE uid = :UuidUtilisateur")
    public abstract Maybe<UtilisateurEntity> findSingleByUuid(String UuidUtilisateur);

    @Transaction
    @Query("SELECT COUNT(*) FROM utilisateur WHERE uid = :UuidUtilisateur")
    public abstract Single<Integer> countByUid(String UuidUtilisateur);
}
