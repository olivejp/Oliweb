package oliweb.nc.oliweb.database.dao;

import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

import java.util.List;

/**
 * Created by orlanth23 on 28/01/2018.
 */

public interface AbstractDao<T> {
    @Transaction
    @Insert
    Long[] insert(T... entities);

    @Transaction
    @Insert
    void insert(List<T> entities);

    @Transaction
    @Update
    int update(T... entities);

    @Transaction
    @Delete
    int delete(T... entities);

    @Transaction
    @Delete
    int delete(List<T> entities);
}
