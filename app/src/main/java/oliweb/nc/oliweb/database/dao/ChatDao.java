package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.ChatEntity;

/**
 * Created by orlanth23 on 18/04/2018.
 */
@Dao
public interface ChatDao extends AbstractDao<ChatEntity> {

    @Transaction
    @Query("SELECT * FROM chat")
    Single<List<ChatEntity>> getAll();

    @Transaction
    @Query("SELECT COUNT(*) FROM chat")
    Single<Integer> count();

    @Transaction
    @Query("SELECT * FROM chat WHERE idChat = :idChat")
    Maybe<ChatEntity> findById(Long idChat);

    @Transaction
    @Query("SELECT * FROM chat WHERE uidAnnonce = :uidAnnonce")
    LiveData<List<ChatEntity>> findByUidAnnonce(String uidAnnonce);

    @Transaction
    @Query("SELECT * FROM chat WHERE uidSeller = :uidUser OR uidBuyer = :uidUser")
    LiveData<List<ChatEntity>> findByUidUser(String uidUser);

    @Transaction
    @Query("SELECT * FROM chat WHERE (uidSeller = :uidUser OR uidBuyer = :uidUser) AND uidAnnonce = :uidAnnonce LIMIT 1")
    Maybe<ChatEntity> findByUidUserAndUidAnnonce(String uidUser, String uidAnnonce);
}
