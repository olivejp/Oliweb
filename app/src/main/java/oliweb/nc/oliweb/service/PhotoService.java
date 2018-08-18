package oliweb.nc.oliweb.service;

import android.content.Context;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.repository.local.PhotoRepository;
import oliweb.nc.oliweb.utility.MediaUtility;

/**
 * Created by orlanth23 on 18/08/2018.
 */
@Singleton
public class PhotoService {

    private static final String TAG = PhotoService.class.getName();

    private PhotoRepository photoRepository;
    private Context context;

    @Inject
    public PhotoService(Context context,
                        PhotoRepository photoRepository) {
        this.context = context;
        this.photoRepository = photoRepository;
    }

    public void deleteListPhoto(List<PhotoEntity> listToDelete) {
        if (listToDelete != null && !listToDelete.isEmpty()) {
            for (PhotoEntity photo : listToDelete) {
                MediaUtility.deletePhotoFromDevice(context.getContentResolver(), photo);
                photoRepository.delete(photo);
            }
        }
    }
}
