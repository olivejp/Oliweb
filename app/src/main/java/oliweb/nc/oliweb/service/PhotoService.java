package oliweb.nc.oliweb.service;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Single;
import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.utility.MediaUtility;

/**
 * Created by orlanth23 on 18/08/2018.
 */
@Singleton
public class PhotoService {

    private PhotoRepository photoRepository;
    private Context context;
    private MediaUtility mediaUtility;

    @Inject
    public PhotoService(Context context,
                        PhotoRepository photoRepository,
                        MediaUtility mediaUtility) {
        this.context = context;
        this.photoRepository = photoRepository;
        this.mediaUtility = mediaUtility;
    }

    public void deleteListPhoto(List<PhotoEntity> listToDelete) {
        if (listToDelete != null && !listToDelete.isEmpty()) {
            for (PhotoEntity photo : listToDelete) {
                if (StringUtils.isNotBlank(photo.getUriLocal())) {
                    mediaUtility.deletePhotoFromDevice(context.getContentResolver(), photo.getUriLocal());
                }
                photoRepository.delete(photo);
            }
        }
    }

    private Single<AtomicBoolean> deleteFromDevice(PhotoEntity photo) {
        if (photo == null) {
            return Single.error(new RuntimeException("Photo to delete is null"));
        }
        if (StringUtils.isEmpty(photo.getUriLocal())) {
            return Single.error(new RuntimeException("Uri local to delete is null or empty"));
        }
        return Single.create(emitter -> {
            boolean deleted = mediaUtility.deletePhotoFromDevice(context.getContentResolver(), photo.getUriLocal());
            emitter.onSuccess(new AtomicBoolean(deleted));
        });
    }

    public Single<AtomicBoolean> deleteListFromDevice(List<PhotoEntity> photoList) {
        if (photoList == null || photoList.isEmpty()) {
            return Single.just(new AtomicBoolean(true));
        }
        return Single.create(emitter ->
                Observable.fromIterable(photoList)
                        .switchMapSingle(this::deleteFromDevice)
                        .doOnComplete(() -> emitter.onSuccess(new AtomicBoolean(true)))
                        .doOnError(emitter::onError)
                        .subscribe()
        );
    }
}
