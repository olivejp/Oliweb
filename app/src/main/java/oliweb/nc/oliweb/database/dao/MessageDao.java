package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import oliweb.nc.oliweb.database.entity.MessageEntity;

/**
 * Created by orlanth23 on 19/04/2018.
 */
@Dao
public interface MessageDao extends AbstractDao<MessageEntity> {
    @Transaction
    @Query("SELECT * FROM message WHERE uidChat = :uidChat")
    LiveData<List<MessageEntity>> findByUidChat(String uidChat);
}
