package oliweb.nc.oliweb.database.entity;

import androidx.room.Embedded;
import androidx.room.Relation;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by orlanth23 on 08/02/2018.
 */

public class AnnonceFull implements Parcelable {
    @Embedded
    public AnnonceEntity annonce;

    @Relation(parentColumn = "idAnnonce", entityColumn = "idAnnonce")
    public List<PhotoEntity> photos;

    @Relation(parentColumn = "idCategorie", entityColumn = "idCategorie")
    public List<CategorieEntity> categorie;

    @Relation(parentColumn = "uidUser", entityColumn = "uid")
    public List<UserEntity> utilisateur;

    public AnnonceEntity getAnnonce() {
        return annonce;
    }

    public void setAnnonce(AnnonceEntity annonce) {
        this.annonce = annonce;
    }

    public List<PhotoEntity> getPhotos() {
        return photos;
    }

    public void setPhotos(List<PhotoEntity> photos) {
        this.photos = photos;
    }

    public List<CategorieEntity> getCategorie() {
        return categorie;
    }

    public void setCategorie(List<CategorieEntity> categorie) {
        this.categorie = categorie;
    }

    public List<UserEntity> getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(List<UserEntity> utilisateur) {
        this.utilisateur = utilisateur;
    }

    @Override
    public String toString() {
        return "AnnonceFull{" +
                "annonce=" + annonce +
                ", photos=" + photos +
                ", categorie=" + categorie +
                ", utilisateur=" + utilisateur +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.annonce, flags);
        dest.writeTypedList(this.photos);
        dest.writeTypedList(this.categorie);
        dest.writeTypedList(this.utilisateur);
    }

    public AnnonceFull() {
    }

    protected AnnonceFull(Parcel in) {
        this.annonce = in.readParcelable(AnnonceEntity.class.getClassLoader());
        this.photos = in.createTypedArrayList(PhotoEntity.CREATOR);
        this.categorie = in.createTypedArrayList(CategorieEntity.CREATOR);
        this.utilisateur = in.createTypedArrayList(UserEntity.CREATOR);
    }

    public static final Creator<AnnonceFull> CREATOR = new Creator<AnnonceFull>() {
        @Override
        public AnnonceFull createFromParcel(Parcel source) {
            return new AnnonceFull(source);
        }

        @Override
        public AnnonceFull[] newArray(int size) {
            return new AnnonceFull[size];
        }
    };
}
