package oliweb.nc.oliweb.repository.local;

import androidx.lifecycle.LiveData;
import android.content.Context;
import android.util.Log;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.PhotoDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;

/**
 * Created by 2761oli on 29/01/2018.
 */
@Singleton
public class PhotoRepository extends AbstractRepository<PhotoEntity, Long> {
    private static final String TAG = PhotoRepository.class.getName();
    private PhotoDao photoDao;

    @Inject
    public PhotoRepository(Context context) {
        super(context);
        this.photoDao = this.db.getPhotoDao();
        this.dao = this.photoDao;
    }

    public LiveData<List<PhotoEntity>> findAllByIdAnnonce(long idAnnonce) {
        Log.d(TAG, "Starting findAllByIdAnnonce idAnnonce : " + idAnnonce);
        return this.photoDao.findByIdAnnonce(idAnnonce);
    }

    private Single<List<PhotoEntity>> findAllPhotosByIdAnnonce(long idAnnonce) {
        Log.d(TAG, "Starting findAllPhotosByIdAnnonce idAnnonce : " + idAnnonce);
        return this.photoDao.findAllSingleByIdAnnonce(idAnnonce);
    }

    public Flowable<PhotoEntity> getAllPhotosByStatus(List<String> status) {
        Log.d(TAG, "Starting getAllPhotosByStatus status : " + status);
        return this.photoDao.getAllPhotosByStatus(status);
    }

    Observable<List<PhotoEntity>> markToDeleteByAnnonce(AnnonceEntity annonceEntity) {
        Log.d(TAG, "Starting markToDeleteByAnnonce annonceEntity : " + annonceEntity);
        return findAllPhotosByIdAnnonce(annonceEntity.getIdAnnonce())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .flattenAsObservable(list -> list)
                .concatMap(this::markToDelete)
                .toList()
                .toObservable();
    }

    private Observable<PhotoEntity> markToDelete(PhotoEntity photoEntity) {
        Log.d(TAG, "markAsToDelete photoEntity : " + photoEntity);
        photoEntity.setStatut(StatusRemote.TO_DELETE);
        return this.singleSave(photoEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    public Observable<PhotoEntity> markAsFailedToDelete(PhotoEntity photoEntity) {
        Log.d(TAG, "markAsFailedToDelete photoEntity : " + photoEntity);
        photoEntity.setStatut(StatusRemote.FAILED_TO_DELETE);
        return this.singleSave(photoEntity)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
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
