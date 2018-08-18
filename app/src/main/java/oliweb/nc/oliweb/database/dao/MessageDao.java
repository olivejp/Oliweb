package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.MessageEntity;

/**
 * Created by orlanth23 on 19/04/2018.
 */
@Dao
public abstract class MessageDao implements AbstractDao<MessageEntity, Long> {

    @Override
    @Transaction
    @Query("SELECT * FROM message WHERE idMessage = :idMessage")
    public abstract Maybe<MessageEntity> findById(Long idMessage);

    @Override
    @Transaction
    @Query("SELECT * FROM message")
    public abstract Single<List<MessageEntity>> getAll();

    @Override
    @Transaction
    @Query("SELECT COUNT(*) FROM message")
    public abstract Single<Integer> count();

    @Transaction
    @Query("SELECT * FROM message WHERE uidMessage = :uidMessage")
    public abstract Maybe<MessageEntity> findSingleByUid(String uidMessage);

    @Transaction
    @Query("SELECT * FROM message WHERE idChat = :idChat ORDER BY timestamp DESC")
    public abstract LiveData<List<MessageEntity>> findAllByIdChat(Long idChat);

    @Transaction
    @Query("SELECT * FROM message WHERE statusRemote IN (:status) AND uidChat <> ''")
    public abstract Flowable<List<MessageEntity>> findFlowableByStatusAndUidChatNotNull(List<String> status);

    @Transaction
    @Query("SELECT * FROM message WHERE statusRemote IN (:status) AND uidChat <> ''")
    public abstract Single<List<MessageEntity>> findSingleByStatus(List<String> status);

    @Transaction
    @Query("SELECT * FROM message WHERE idChat =:idChat")
    public abstract Single<List<MessageEntity>> getSingleByIdChat(Long idChat);
}
