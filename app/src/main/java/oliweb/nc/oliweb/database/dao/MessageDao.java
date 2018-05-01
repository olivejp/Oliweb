package oliweb.nc.oliweb.database.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.MessageEntity;

/**
 * Created by orlanth23 on 19/04/2018.
 */
@Dao
public interface MessageDao extends AbstractDao<MessageEntity> {
    @Transaction
    @Query("SELECT * FROM message WHERE uidMessage = :uidMessage")
    Maybe<MessageEntity> findSingleById(String uidMessage);

    @Transaction
    @Query("SELECT COUNT(*) FROM message WHERE uidMessage = :uidMessage")
    Single<Integer> countById(String uidMessage);

}
