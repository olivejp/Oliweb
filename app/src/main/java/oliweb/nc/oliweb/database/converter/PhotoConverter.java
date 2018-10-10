package oliweb.nc.oliweb.database.converter;

import android.net.Uri;

import oliweb.nc.oliweb.database.entity.PhotoEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;

public class PhotoConverter {

    private PhotoConverter() {
    }

    /**
     * Create a new PhotoEntity
     *
     * @param idAnnonce which we want to attach the photo to
     * @param urlPhoto  remote url (firebase storage) of the photo
     * @param uriLocal  local uri of the photo
     * @return the photoEntity created
     */
    public static PhotoEntity createPhotoEntityFromUrl(long idAnnonce, String urlPhoto, Uri uriLocal) {
        PhotoEntity photoEntity = new PhotoEntity();
        photoEntity.setStatut(StatusRemote.SEND);
        photoEntity.setFirebasePath(urlPhoto);
        photoEntity.setUriLocal(uriLocal.toString());
        photoEntity.setIdAnnonce(idAnnonce);
        return photoEntity;
    }
}
