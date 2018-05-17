package oliweb.nc.oliweb.database.dao;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.PhotoEntity;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Dao
public abstract class PhotoDao implements AbstractDao<PhotoEntity, Long> {
    @Override
    @Transaction
    @Query("SELECT * FROM photo WHERE idPhoto = :idPhoto")
    public abstract Maybe<PhotoEntity> findById(Long idPhoto);

    @Override
    @Transaction
    @Query("SELECT * FROM photo")
    public abstract Single<List<PhotoEntity>> getAll();

    @Override
    @Transaction
    @Query("SELECT COUNT(*) FROM photo")
    public abstract Single<Integer> count();

    @Transaction
    @Query("SELECT * FROM photo WHERE idAnnonce = :idAnnonce")
    public abstract LiveData<List<PhotoEntity>> findByIdAnnonce(Long idAnnonce);

    @Transaction
    @Query("SELECT * FROM photo WHERE idPhoto = :idPhoto")
    public abstract Single<PhotoEntity> findSingleById(long idPhoto);

    @Transaction
    @Query("SELECT * FROM photo WHERE idAnnonce = :idAnnonce")
    public abstract Single<List<PhotoEntity>> findAllSingleByIdAnnonce(long idAnnonce);

    @Transaction
    @Query("SELECT * FROM photo WHERE statut IN (:statut)")
    public abstract Maybe<List<PhotoEntity>> getAllPhotosByStatus(List<String> statut);

    @Transaction
    @Query("SELECT * FROM photo WHERE statut IN (:listStatut) AND idAnnonce = :idAnnonce")
    public abstract Maybe<List<PhotoEntity>> getAllPhotosByStatusAndIdAnnonce(List<String> listStatut, Long idAnnonce);

    @Transaction
    @Query("SELECT COUNT(*) FROM photo WHERE idAnnonce = :idAnnonce")
    public abstract Single<Integer> countAllPhotosByIdAnnonce(long idAnnonce);

    @Transaction
    @Query("SELECT COUNT(*) FROM photo WHERE statut = :status")
    public abstract Flowable<Integer> countFlowableAllPhotosByStatus(String status);
}
