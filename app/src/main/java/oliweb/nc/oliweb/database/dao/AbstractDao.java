package oliweb.nc.oliweb.database.dao;

import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * Created by orlanth23 on 28/01/2018.
 */

public interface AbstractDao<T, U> {

    @Transaction
    @Insert
    U insert(T entity);

    @Transaction
    @Insert
    U[] insert(T... entities);

    @Transaction
    @Insert
    void insert(List<T> entities);

    @Transaction
    @Update
    int update(T entities);

    @Transaction
    @Update
    int update(T... entities);

    @Transaction
    @Delete
    int delete(T... entities);

    @Transaction
    @Delete
    int delete(List<T> entities);

    Maybe<T> findById(U id);

    Single<List<T>> getAll();

    Single<Integer> count();
}
