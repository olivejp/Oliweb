package oliweb.nc.oliweb.dto.firebase;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.PropertyName;

/**
 * Created by orlanth23 on 20/02/2018.
 */

public class CategorieFirebase implements Parcelable {
    @PropertyName("id_categorie")
    private long id;

    @PropertyName("libelle_categorie")
    private String libelle;

    public CategorieFirebase() {
    }

    public CategorieFirebase(long id, String libelle) {
        this.id = id;
        this.libelle = libelle;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    @Override
    public String toString() {
        return "CategorieFirebase{" +
                "id=" + id +
                ", libelle='" + libelle + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.libelle);
    }

    protected CategorieFirebase(Parcel in) {
        this.id = in.readLong();
        this.libelle = in.readString();
    }

    public static final Parcelable.Creator<CategorieFirebase> CREATOR = new Parcelable.Creator<CategorieFirebase>() {
        @Override
        public CategorieFirebase createFromParcel(Parcel source) {
            return new CategorieFirebase(source);
        }

        @Override
        public CategorieFirebase[] newArray(int size) {
            return new CategorieFirebase[size];
        }
    };
}
