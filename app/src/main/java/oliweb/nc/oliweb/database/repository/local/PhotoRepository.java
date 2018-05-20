package oliweb.nc.oliweb.database.repository.local;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.dao.PhotoDao;
import oliweb.nc.oliweb.database.entity.AnnonceEntity;
import oliweb.nc.oliweb.database.entity.AnnonceFull;
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
    private AnnonceFullRepository annonceFullRepository;

    private PhotoRepository(Context context) {
        super(context);
        this.photoDao = this.db.getPhotoDao();
        this.annonceFullRepository = AnnonceFullRepository.getInstance(context);
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

    public Flowable<PhotoEntity> getAllPhotosByUidUserAndStatus(String uidUser, List<String> status) {
        Log.d(TAG, "Starting getAllPhotosByUidUserAndStatus uidUser : " + uidUser + " status : " + status);
        return this.annonceFullRepository.getAllAnnoncesByUidUser(uidUser)                                   // Récupération de toutes les annonces pour l'uid user passé
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())                                     // On souscrit et on observe sur des threads de backend
                .filter(annonceFull -> annonceFull.getPhotos() != null && !annonceFull.getPhotos().isEmpty()) // Filtre pour ne prendre que les annonces avec des photos
                .flatMapIterable(AnnonceFull::getPhotos)
                .filter(photoEntity -> status.contains(photoEntity.getStatut().toString()));                 // Filtre sur la liste pour ne prendre que les photos avec les statuts recherchés
    }

    public Flowable<PhotoEntity> getAllPhotosByStatus(List<String> status) {
        Log.d(TAG, "Starting getAllPhotosByStatus status : " + status);
        return this.photoDao.getAllPhotosByStatus(status);
    }

    public Observable<List<PhotoEntity>> markToDeleteByAnnonce(AnnonceEntity annonceEntity) {
        Log.d(TAG, "Starting markToDeleteByAnnonce annonceEntity : " + annonceEntity);
        return findAllPhotosByIdAnnonce(annonceEntity.getIdAnnonce())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .flattenAsObservable(list -> list)
                .concatMap(this::markToDelete)
                .toList()
                .toObservable();
    }

    private Observable<PhotoEntity> markToDelete(PhotoEntity photoEntity) {
        Log.d(TAG, "markToDelete photoEntity : " + photoEntity);
        photoEntity.setStatut(StatusRemote.TO_DELETE);
        return this.saveWithSingle(photoEntity)
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .toObservable();
    }

    public Observable<PhotoEntity> markAsFailedToDelete(PhotoEntity photoEntity) {
        Log.d(TAG, "markAsFailedToDelete photoEntity : " + photoEntity);
        photoEntity.setStatut(StatusRemote.FAILED_TO_DELETE);
        return this.saveWithSingle(photoEntity)
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

    public Single<Integer> countByIdAnnonce(Long idAnnonce) {
        return photoDao.countAllPhotosByIdAnnonce(idAnnonce);
    }
}
