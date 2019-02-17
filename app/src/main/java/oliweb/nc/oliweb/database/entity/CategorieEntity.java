package oliweb.nc.oliweb.database.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Entity(tableName = "categorie")
public class CategorieEntity extends AbstractEntity<Long> implements Parcelable {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    private Long idCategorie;
    private String name;
    private String couleur;

    public CategorieEntity() {
    }

    @Ignore
    public CategorieEntity(String name, String couleur) {
        this.name = name;
        this.couleur = couleur;
    }

    @Ignore
    public CategorieEntity(Long idCategorie, String name, String couleur) {
        this.idCategorie = idCategorie;
        this.name = name;
        this.couleur = couleur;
    }

    @NonNull
    public Long getId() {
        return idCategorie;
    }

    @NonNull
    public Long getIdCategorie() {
        return idCategorie;
    }

    public void setIdCategorie(@NonNull Long idCategorie) {
        this.idCategorie = idCategorie;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.idCategorie);
        dest.writeString(this.name);
        dest.writeString(this.couleur);
    }

    protected CategorieEntity(Parcel in) {
        this.idCategorie = (Long) in.readValue(Long.class.getClassLoader());
        this.name = in.readString();
        this.couleur = in.readString();
    }

    public static final Creator<CategorieEntity> CREATOR = new Creator<CategorieEntity>() {
        @Override
        public CategorieEntity createFromParcel(Parcel source) {
            return new CategorieEntity(source);
        }

        @Override
        public CategorieEntity[] newArray(int size) {
            return new CategorieEntity[size];
        }
    };
}
