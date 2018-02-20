package oliweb.nc.oliweb.database.repository;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.PhotoDao;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class PhotoRepository extends AbstractRepository<PhotoEntity> {
    private static PhotoRepository INSTANCE;
    private PhotoDao photoDao;

    private PhotoRepository(Context context) {
        super(context);
        this.photoDao = this.db.PhotoDao();
        this.dao = this.photoDao;
    }

    public static synchronized PhotoRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new PhotoRepository(context);
        }
        return INSTANCE;
    }

    public void save(PhotoEntity photoEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        if (photoEntity.getIdPhoto() != null) {
            this.photoDao.findSingleById(photoEntity.getIdPhoto())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe((photoEntity1, throwable) -> {
                        if (throwable != null) {
                            // This user don't exists already, create it
                            insert(onRespositoryPostExecute, photoEntity);
                        } else {
                            if (photoEntity1 != null) {
                                // User exists, just update it
                                update(onRespositoryPostExecute, photoEntity);
                            }
                        }
                    });
        } else {
            insert(onRespositoryPostExecute, photoEntity);
        }
    }

    /**
     * Attention le postExecute de l'interface OnRespositoryPostExecute sera appelé autant de fois qu'il y aura de photos supprimées.
     *
     * @param listPhoto
     * @param onRespositoryPostExecute
     */
    public void save(List<PhotoEntity> listPhoto, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        for (PhotoEntity photoEntity : listPhoto) {
            save(photoEntity, onRespositoryPostExecute);
        }
    }

    public Single<PhotoEntity> singleById(long idPhoto) {
        return this.photoDao.findSingleById(idPhoto);
    }

    public LiveData<List<PhotoEntity>> findAllByIdAnnonce(long idAnnonce) {
        return this.photoDao.findByIdAnnonce(idAnnonce);
    }

    public void deleteByIdAnnonce(long idAnnonce) {
        this.photoDao.findAllSingleByIdAnnonce(idAnnonce)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(photoEntities -> {
                    if (photoEntities != null && !photoEntities.isEmpty()) {
                        this.photoDao.delete(photoEntities);
                    }
                });
    }

    public Maybe<List<PhotoEntity>> getAllPhotosByStatus(String status) {
        return this.photoDao.getAllPhotosByStatus(status);
    }
}
