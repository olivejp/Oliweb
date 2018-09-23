package oliweb.nc.oliweb.dto.firebase;

import com.google.firebase.database.PropertyName;

/**
 * Created by orlanth23 on 20/02/2018.
 */

public class CategorieFirebase {
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
}
