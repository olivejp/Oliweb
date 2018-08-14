package oliweb.nc.oliweb.firebase.repository;

import android.util.Log;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;

@Singleton
public class FirebasePhotoRepository {

    private static final String TAG = FirebasePhotoRepository.class.getName();

    @Inject
    public PhotoRepository photoRepository;

    @Inject
    public FirebasePhotoRepository() {
    }

    public void updatePhotosStatusByIdAnnonce(Long idAnnonce, StatusRemote statusRemote) {
        photoRepository.findAllPhotosByIdAnnonce(idAnnonce)
                .subscribeOn(Schedulers.io()).observeOn(Schedulers.io())
                .doOnError(e -> Log.e(TAG, e.getLocalizedMessage(), e))
                .doOnSuccess(listPhotos -> {
                    for (PhotoEntity photo : listPhotos) {
                        photo.setStatut(statusRemote);
                        photoRepository.save(photo);
                    }
                })
                .subscribe();
    }
}
