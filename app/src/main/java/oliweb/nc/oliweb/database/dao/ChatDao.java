package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.ChatEntity;

/**
 * Created by orlanth23 on 18/04/2018.
 */
@Dao
public interface ChatDao extends AbstractDao<ChatEntity> {
    @Transaction
    @Query("SELECT * FROM chat WHERE uidChat = :uidChat")
    LiveData<ChatEntity> findById(String uidChat);

    @Transaction
    @Query("SELECT * FROM chat WHERE uidChat = :uidChat")
    Single<ChatEntity> findSingleById(String uidChat);

    @Transaction
    @Query("SELECT * FROM chat WHERE uidSeller = :uidSeller")
    LiveData<List<ChatEntity>> findByUidSeller(String uidSeller);
}
