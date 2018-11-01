package oliweb.nc.oliweb.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

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
    public abstract Flowable<PhotoEntity> getAllPhotosByStatus(List<String> statut);

    @Transaction
    @Query("SELECT * FROM photo WHERE statut IN (:listStatut) AND idAnnonce = :idAnnonce")
    public abstract Maybe<List<PhotoEntity>> getAllPhotosByStatusAndIdAnnonce(List<String> listStatut, Long idAnnonce);

    @Transaction
    @Query("SELECT COUNT(*) FROM photo WHERE idAnnonce = :idAnnonce")
    public abstract Single<Integer> countAllPhotosByIdAnnonce(long idAnnonce);

    @Transaction
    @Query("SELECT COUNT(*) FROM photo WHERE statut = :status")
    public abstract Flowable<Integer> countFlowableAllPhotosByStatus(String status);

    @Transaction
    @Query("SELECT photo.* FROM photo INNER JOIN annonce ON photo.idAnnonce = annonce.idAnnonce WHERE photo.statut IN (:status) AND annonce.uidUser = :uidUser")
    public abstract Flowable<PhotoEntity> getAllPhotosByUidUserAndStatus(String uidUser, List<String> status);
}
