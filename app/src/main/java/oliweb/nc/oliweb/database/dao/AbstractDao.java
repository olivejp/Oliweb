package oliweb.nc.oliweb.database.dao;

import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Transaction;
import android.arch.persistence.room.Update;

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
