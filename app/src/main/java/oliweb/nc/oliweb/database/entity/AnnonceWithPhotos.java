package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

/**
 * Created by orlanth23 on 08/02/2018.
 */

public class AnnonceWithPhotos {
    @Embedded
    public AnnonceEntity annonceEntity;

    @Relation(parentColumn = "idAnnonce", entityColumn = "idAnnonce")
    public List<PhotoEntity> photos;

    public AnnonceEntity getAnnonceEntity() {
        return annonceEntity;
    }

    public void setAnnonceEntity(AnnonceEntity annonceEntity) {
        this.annonceEntity = annonceEntity;
    }

    public List<PhotoEntity> getPhotos() {
        return photos;
    }

    public void setPhotos(List<PhotoEntity> photos) {
        this.photos = photos;
    }
}
