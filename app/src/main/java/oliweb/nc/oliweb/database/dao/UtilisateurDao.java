package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.UserEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public abstract class UtilisateurDao implements AbstractDao<UserEntity, Long> {

    @Override
    @Transaction
    @Query("SELECT * FROM utilisateur WHERE idUser = :idUtilisateur")
    public abstract Maybe<UserEntity> findById(Long idUtilisateur);

    @Override
    @Transaction
    @Query("SELECT * FROM utilisateur")
    public abstract Single<List<UserEntity>> getAll();

    @Override
    @Transaction
    @Query("SELECT COUNT(*) FROM utilisateur")
    public abstract Single<Integer> count();

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE uid = :uuidUtilisateur")
    public abstract Maybe<UserEntity> findById(String uuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE uid = :uuidUtilisateur")
    public abstract LiveData<UserEntity> findByUid(String uuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE uid = :uuidUtilisateur AND favorite = 1")
    public abstract Maybe<UserEntity> findMaybeFavoriteByUid(String uuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE uid = :uuidUtilisateur AND favorite = 0")
    public abstract Maybe<UserEntity> findMaybeByUid(String uuidUtilisateur);

    @Transaction
    @Query("SELECT COUNT(*) FROM utilisateur WHERE uid = :uuidUtilisateur")
    public abstract Single<Integer> countByUid(String uuidUtilisateur);

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE statut IN (:statutList)")
    public abstract Flowable<UserEntity> getAllUtilisateursByStatus(List<String> statutList);

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE uid = :uid and statut IN (:statutList)")
    public abstract Single<UserEntity> findSingleByUidAndStatus(String uid, List<String> statutList);

    @Transaction
    @Query("SELECT * FROM utilisateur WHERE uid = :uid")
    public abstract Flowable<UserEntity> findFlowableByUid(String uid);
}
