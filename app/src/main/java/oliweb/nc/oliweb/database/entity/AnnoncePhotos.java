package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by orlanth23 on 08/02/2018.
 */

public class AnnoncePhotos implements Parcelable {
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


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.annonceEntity, flags);
        dest.writeList(this.photos);
    }

    public AnnoncePhotos() {
    }

    protected AnnoncePhotos(Parcel in) {
        this.annonceEntity = in.readParcelable(AnnonceEntity.class.getClassLoader());
        this.photos = new ArrayList<>();
        in.readList(this.photos, PhotoEntity.class.getClassLoader());
    }

    public static final Parcelable.Creator<AnnoncePhotos> CREATOR = new Parcelable.Creator<AnnoncePhotos>() {
        @Override
        public AnnoncePhotos createFromParcel(Parcel source) {
            return new AnnoncePhotos(source);
        }

        @Override
        public AnnoncePhotos[] newArray(int size) {
            return new AnnoncePhotos[size];
        }
    };
}
