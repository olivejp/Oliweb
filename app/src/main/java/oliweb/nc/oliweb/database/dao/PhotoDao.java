package oliweb.nc.oliweb.database.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
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
    public abstract Single<List<PhotoEntity>> findAllSingleByIdAnnonce(long idAnnonce);

    @Transaction
    @Query("SELECT * FROM photo WHERE statut IN (:statut)")
    public abstract Flowable<PhotoEntity> getAllPhotosByStatus(List<String> statut);

    @Transaction
    @Query("SELECT photo.* FROM photo, annonce WHERE photo.statut NOT IN ('TO_DELETE', 'DELETED', 'FAILED_TO_DELETE') AND annonce.statut NOT IN ('TO_DELETE', 'DELETED', 'FAILED_TO_DELETE') AND annonce.idAnnonce = photo.idAnnonce AND annonce.uidUser = :uidUser")
    public abstract LiveData<List<PhotoEntity>> findAllByUidUser(String uidUser);

}
