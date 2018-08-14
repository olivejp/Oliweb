package oliweb.nc.oliweb.firebase.repository;

import android.content.Context;
import android.util.Log;

import javax.inject.Inject;

import io.reactivex.schedulers.Schedulers;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.database.repository.local.PhotoRepository;

public class FirebasePhotoRepository {

    private static final String TAG = FirebasePhotoRepository.class.getName();

    private static FirebasePhotoRepository instance;

    private PhotoRepository photoRepository;

    @Inject
    public FirebasePhotoRepository() {
    }

    public static synchronized  FirebasePhotoRepository getInstance(Context context) {
        if (instance == null) {
            instance = new FirebasePhotoRepository();
        }
        instance.photoRepository = PhotoRepository.getInstance(context);
        return instance;
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
