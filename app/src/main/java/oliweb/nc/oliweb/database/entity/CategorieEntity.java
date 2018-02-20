package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import com.google.firebase.database.Exclude;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Entity(tableName = "categorie")
public class CategorieEntity {
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

    @Exclude
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
}
