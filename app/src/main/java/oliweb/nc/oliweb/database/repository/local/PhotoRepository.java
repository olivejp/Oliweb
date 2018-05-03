package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.PhotoDao;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class PhotoRepository extends AbstractRepository<PhotoEntity> {
    private static final String TAG = PhotoRepository.class.getName();
    private static PhotoRepository INSTANCE;
    private PhotoDao photoDao;

    private PhotoRepository(Context context) {
        super(context);
        this.photoDao = this.db.getPhotoDao();
        this.dao = this.photoDao;
    }

    public static synchronized PhotoRepository getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new PhotoRepository(context);
        }
        return INSTANCE;
    }

    public void save(PhotoEntity photoEntity) {
        save(photoEntity, null);
    }

    public void save(PhotoEntity photoEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        Log.d(TAG, "Starting save photoEntity : " + photoEntity);
        if (photoEntity != null) {
            this.photoDao.findSingleById(photoEntity.getIdPhoto())
                    .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                    .doOnSuccess(photoSaved -> {
                        if (photoSaved != null) {
                            insert(onRespositoryPostExecute, photoEntity);
                        } else {
                            update(onRespositoryPostExecute, photoEntity);
                        }
                    })
                    .doOnError(exception -> Log.e(TAG, "save " + exception.getLocalizedMessage(), exception))
                    .subscribe();
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
        Log.d(TAG, "Starting save listPhoto : " + listPhoto);
        for (PhotoEntity photoEntity : listPhoto) {
            save(photoEntity, onRespositoryPostExecute);
        }
    }


    private Single<AtomicBoolean> existById(Long idAnnonce) {
        Log.d(TAG, "Starting existById idAnnonce : " + idAnnonce);
        return Single.create(e -> photoDao.countById(idAnnonce)
                .doOnSuccess(count -> e.onSuccess(new AtomicBoolean(count != null && count == 1)))
                .doOnError(e::onError)
                .subscribe());
    }

    private Single<PhotoEntity> insertSingle(PhotoEntity entity) {
        Log.d(TAG, "Starting insertSingle photoEntity : " + entity);
        return Single.create(e -> {
            try {
                Long[] ids = dao.insert(entity);
                if (ids.length == 1) {
                    findSingleById(ids[0])
                            .doOnSuccess(e::onSuccess)
                            .subscribe();
                } else {
                    e.onError(new RuntimeException("Failed to insert into PhotoRepository"));
                }
            } catch (Exception exception) {
                Log.e(TAG, exception.getMessage());
                e.onError(exception);
            }
        });
    }

    /**
     * You have to subscribe to this Single on a background thread
     * because it queries the Database which only accept background queries.
     */
    private Single<PhotoEntity> updateSingle(PhotoEntity entity) {
        Log.d(TAG, "Starting updateSingle photoEntity : " + entity);
        return Single.create(e -> {
            try {
                int updatedCount = dao.update(entity);
                if (updatedCount == 1) {
                    findSingleById(entity.getIdAnnonce())
                            .doOnSuccess(e::onSuccess)
                            .subscribe();
                } else {
                    e.onError(new RuntimeException("Failed to update into PhotoRepository"));
                }
            } catch (Exception exception) {
                Log.e(TAG, "updateSingle catch exception : " + exception.getMessage());
                e.onError(exception);
            }
        });
    }

    public Single<PhotoEntity> saveWithSingle(PhotoEntity photoEntity) {
        Log.d(TAG, "Starting saveWithSingle photoEntity : " + photoEntity);
        return Single.create(emitter -> existById(photoEntity.getIdAnnonce())
                .observeOn(Schedulers.io()).subscribeOn(Schedulers.io())
                .doOnError(emitter::onError)
                .doOnSuccess(result -> {
                    Log.d(TAG, "saveWithSingle.doOnSuccess result : " + result.get());
                    if (result.get()) {
                        updateSingle(photoEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    } else {
                        insertSingle(photoEntity)
                                .doOnSuccess(emitter::onSuccess)
                                .doOnError(emitter::onError)
                                .subscribe();
                    }
                })
                .subscribe());
    }

    public Maybe<List<PhotoEntity>> saveWithSingle(List<PhotoEntity> photoEntities) {
        Log.d(TAG, "Starting saveWithSingle photoEntities : " + photoEntities);
        return Maybe.create(emitter -> {
            if (photoEntities == null || photoEntities.isEmpty()) {
                emitter.onSuccess(Collections.emptyList());
            } else {
                AtomicInteger countSuccess = new AtomicInteger();
                ArrayList<PhotoEntity> listResult = new ArrayList<>();

                for (PhotoEntity annonce : photoEntities) {
                    saveWithSingle(annonce)
                            .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                            .doOnSuccess(photoEntity -> {
                                Log.d(TAG, "saveWithSingle.doOnSuccess photoEntity : " + photoEntity);
                                listResult.add(photoEntity);
                                countSuccess.getAndIncrement();
                                if (countSuccess.get() == photoEntities.size()) {
                                    emitter.onSuccess(listResult);
                                }
                            })
                            .doOnError(e -> {
                                Log.d(TAG, "saveWithSingle.doOnError excpetion : " + e.getLocalizedMessage());
                                emitter.onError(e);
                            })
                            .subscribe();
                }
            }
        });
    }

    public Single<PhotoEntity> findSingleById(long idPhoto) {
        Log.d(TAG, "Starting findSingleByUid idPhoto : " + idPhoto);
        return this.photoDao.findSingleById(idPhoto);
    }

    public LiveData<List<PhotoEntity>> findAllByIdAnnonce(long idAnnonce) {
        Log.d(TAG, "Starting findAllByIdAnnonce idAnnonce : " + idAnnonce);
        return this.photoDao.findByIdAnnonce(idAnnonce);
    }

    public Maybe<List<PhotoEntity>> getAllPhotosByStatus(String status) {
        Log.d(TAG, "Starting getAllPhotosByStatus status : " + status);
        return this.photoDao.getAllPhotosByStatus(status);
    }

    public Maybe<List<PhotoEntity>> getAllPhotosByStatusAndIdAnnonce(long idAnnonce, List<String> status) {
        Log.d(TAG, "Starting getAllPhotosByStatusAndIdAnnonce idAnnonce : " + idAnnonce + " status : " + status);
        return this.photoDao.getAllPhotosByStatusAndIdAnnonce(status, idAnnonce);
    }

    public Single<List<PhotoEntity>> findAllPhotosByIdAnnonce(long idAnnonce) {
        Log.d(TAG, "Starting findAllPhotosByIdAnnonce idAnnonce : " + idAnnonce);
        return this.photoDao.findAllSingleByIdAnnonce(idAnnonce);
    }

    public Observable<PhotoEntity> observeAllPhotosByIdAnnonce(long idAnnonce) {
        Log.d(TAG, "Starting findAllPhotosByIdAnnonce idAnnonce : " + idAnnonce);
        return this.photoDao.findAllSingleByIdAnnonce(idAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .flattenAsObservable(list -> list);
    }

    public Observable<PhotoEntity> observeAllPhotosByStatus(String status) {
        Log.d(TAG, "Starting getAllPhotosByStatus status : " + status);
        return this.photoDao.getAllPhotosByStatus(status)
                .flattenAsObservable(list -> list);
    }

    public Observable<PhotoEntity> observeAllPhotosByStatus(List<String> status) {
        Log.d(TAG, "Starting getAllPhotosByStatus status : " + status);
        return this.photoDao.getAllPhotosByStatus(status)
                .flattenAsObservable(list -> list);
    }

    public Single<Integer> countAllPhotosByIdAnnonce(long idAnnonce) {
        Log.d(TAG, "Starting countAllPhotosByIdAnnonce idAnnonce : " + idAnnonce);
        return this.photoDao.countAllPhotosByIdAnnonce(idAnnonce);
    }

    public Flowable<Integer> countFlowableAllPhotosByStatus(String status) {
        Log.d(TAG, "Starting countFlowableAllPhotosByStatus status : " + status);
        return this.photoDao.countFlowableAllPhotosByStatus(status);
    }

    public Single<Integer> deleteAllByIdAnnonce(Long idAnnonce) {
        Log.d(TAG, "Starting deleteAllByIdAnnonce idAnnonce : " + idAnnonce);
        return Single.create(emitter ->
                findAllPhotosByIdAnnonce(idAnnonce)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(emitter::onError)
                        .doOnSuccess(listPhotos -> {
                            Log.d(TAG, "findAllPhotosByIdAnnonce.doOnSuccess listPhotos : " + listPhotos);
                            if (listPhotos == null || listPhotos.isEmpty()) {
                                emitter.onSuccess(0);
                            } else {
                                try {
                                    emitter.onSuccess(photoDao.delete(listPhotos));
                                } catch (Exception e) {
                                    emitter.onError(e);
                                }
                            }
                        })
                        .subscribe()
        );
    }
}
