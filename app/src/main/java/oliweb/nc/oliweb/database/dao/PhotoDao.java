package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.PhotoEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public interface PhotoDao extends AbstractDao<PhotoEntity> {
    @Transaction
    @Query("SELECT * FROM photo")
    Maybe<List<PhotoEntity>> getListPhoto();

    @Transaction
    @Query("SELECT * FROM photo WHERE idPhoto = :idPhoto")
    LiveData<PhotoEntity> findById(long idPhoto);

    @Transaction
    @Query("SELECT * FROM photo WHERE idAnnonce = :idAnnonce")
    LiveData<List<PhotoEntity>> findByIdAnnonce(long idAnnonce);

    @Transaction
    @Query("SELECT * FROM photo WHERE idPhoto = :idPhoto")
    Single<PhotoEntity> singleById(long idPhoto);
}
