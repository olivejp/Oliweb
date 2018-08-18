package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.ChatEntity;

/**
 * Created by orlanth23 on 18/04/2018.
 */
@Dao
public abstract class ChatDao implements AbstractDao<ChatEntity, Long> {

    @Override
    @Transaction
    @Query("SELECT * FROM chat WHERE idChat = :idChat")
    public abstract Maybe<ChatEntity> findById(Long idChat);

    @Override
    @Transaction
    @Query("SELECT * FROM chat")
    public abstract Single<List<ChatEntity>> getAll();

    @Override
    @Transaction
    @Query("SELECT COUNT(*) FROM chat")
    public abstract Single<Integer> count();

    @Transaction
    @Query("SELECT * FROM chat WHERE uidAnnonce = :uidAnnonce AND statusRemote NOT IN (:status) ORDER BY titreAnnonce ASC, updateTimestamp DESC")
    public abstract LiveData<List<ChatEntity>> findByUidAnnonceAndStatusNotInWithOrderByTitreAnnonce(String uidAnnonce, List<String> status);

    @Transaction
    @Query("SELECT * FROM chat WHERE (uidSeller = :uidUser OR uidBuyer = :uidUser) AND statusRemote NOT IN (:status) ORDER BY titreAnnonce ASC, updateTimestamp DESC")
    public abstract LiveData<List<ChatEntity>> findByUidUserAndStatusNotInWithOrderByTitreAnnonce(String uidUser, List<String> status);

    @Transaction
    @Query("SELECT * FROM chat WHERE (uidSeller = :uidUser OR uidBuyer = :uidUser) AND statusRemote NOT IN (:status)")
    public abstract Flowable<List<ChatEntity>> findFlowableByUidUserAndStatusNotIn(String uidUser, List<String> status);

    @Transaction
    @Query("SELECT * FROM chat WHERE (uidSeller = :uidUser OR uidBuyer = :uidUser) AND statusRemote IN (:status)")
    public abstract Flowable<ChatEntity> findFlowableByUidUserAndStatusIn(String uidUser, List<String> status);

    @Transaction
    @Query("SELECT * FROM chat WHERE (uidSeller = :uidUser OR uidBuyer = :uidUser) AND uidAnnonce = :uidAnnonce LIMIT 1")
    public abstract Maybe<ChatEntity> findByUidUserAndUidAnnonce(String uidUser, String uidAnnonce);

    @Transaction
    @Query("SELECT * FROM chat WHERE statusRemote IN (:status)")
    public abstract Single<List<ChatEntity>> getAllChatByStatus(List<String> status);

    @Transaction
    @Query("SELECT COUNT(*) FROM chat WHERE (uidBuyer = :uidUser OR uidSeller = :uidUser) AND statusRemote NOT IN (:status)")
    public abstract LiveData<Integer> countAllFavoritesByUser(String uidUser, List<String> status);

    @Transaction
    @Query("SELECT * FROM chat WHERE uidChat = :uidChat")
    public abstract Maybe<ChatEntity> findByUid(String uidChat);

}
