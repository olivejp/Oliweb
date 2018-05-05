package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.PhotoDao;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.task.AbstractRepositoryCudTask;

/**
 * Created by 2761oli on 29/01/2018.
 */

public class PhotoRepository extends AbstractRepository<PhotoEntity, Long> {
    private static final String TAG = PhotoRepository.class.getName();
    private static PhotoRepository instance;
    private PhotoDao photoDao;

    private PhotoRepository(Context context) {
        super(context);
        this.photoDao = this.db.getPhotoDao();
        this.dao = this.photoDao;
    }

    public static synchronized PhotoRepository getInstance(Context context) {
        if (instance == null) {
            instance = new PhotoRepository(context);
        }
        return instance;
    }

    public void save(PhotoEntity photoEntity) {
        save(photoEntity, null);
    }

    public void save(PhotoEntity photoEntity, @Nullable AbstractRepositoryCudTask.OnRespositoryPostExecute onRespositoryPostExecute) {
        Log.d(TAG, "Starting save photoEntity : " + photoEntity);
        if (photoEntity != null) {
            this.photoDao.findSingleById(photoEntity.getId())
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

    public Single<PhotoEntity> findSingleById(long idPhoto) {
        Log.d(TAG, "Starting findSingleByUid idPhoto : " + idPhoto);
        return this.photoDao.findSingleById(idPhoto);
    }

    public LiveData<List<PhotoEntity>> findAllByIdAnnonce(long idAnnonce) {
        Log.d(TAG, "Starting findAllByIdAnnonce idAnnonce : " + idAnnonce);
        return this.photoDao.findByIdAnnonce(idAnnonce);
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

    public Observable<PhotoEntity> observeAllPhotosByStatus(List<String> status) {
        return Observable.create(e ->
                this.photoDao.getAllPhotosByStatus(status)
                        .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                        .doOnError(e::onError)
                        .doOnSuccess(photoEntities -> {
                            for (PhotoEntity photoEntity : photoEntities) {
                                e.onNext(photoEntity);
                            }
                            e.onComplete();
                        })
                        .subscribe()
        );
    }

    public Maybe<List<PhotoEntity>> getAllPhotosByStatus(List<String> status) {
        Log.d(TAG, "Starting getAllPhotosByStatus status : " + status);
        return this.photoDao.getAllPhotosByStatus(status);
    }

    public Single<Integer> countAllPhotosByIdAnnonce(long idAnnonce) {
        Log.d(TAG, "Starting countAllPhotosByIdAnnonce idAnnonce : " + idAnnonce);
        return this.photoDao.countAllPhotosByIdAnnonce(idAnnonce);
    }

    public Flowable<Integer> countFlowableAllPhotosByStatus(String status) {
        Log.d(TAG, "Starting countFlowableAllPhotosByStatus status : " + status);
        return this.photoDao.countFlowableAllPhotosByStatus(status);
    }

    public Single<AtomicBoolean> markToDelete(Long idAnnonce) {
        Log.d(TAG, "Starting markToDelete idAnnonce : " + idAnnonce);
        return Single.create(emitter ->
                findAllPhotosByIdAnnonce(idAnnonce)
                        .doOnError(emitter::onError)
                        .flattenAsObservable(list -> list)
                        .doOnComplete(() -> emitter.onSuccess(new AtomicBoolean(true)))
                        .doOnNext(photoEntity -> {
                            photoEntity.setStatut(StatusRemote.TO_DELETE);
                            saveWithSingle(photoEntity)
                                    .doOnError(emitter::onError)
                                    .subscribe();
                        })
        );
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
