package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;
import java.util.Set;

/**
 * Created by orlanth23 on 08/02/2018.
 */

public class AnnonceFull {
    @Embedded
    public AnnonceEntity annonceEntity;

    @Relation(parentColumn = "idAnnonce", entityColumn = "idAnnonce")
    public List<PhotoEntity> photos;

    @Relation(parentColumn = "idCategorie", entityColumn = "idCategorie")
    public Set<CategorieEntity> categorie;

    @Relation(parentColumn = "UuidUtilisateur", entityColumn = "UuidUtilisateur")
    public Set<UtilisateurEntity> utilisateurEntity;

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

    public Set<CategorieEntity> getCategorie() {
        return categorie;
    }

    public void setCategorie(Set<CategorieEntity> categorie) {
        this.categorie = categorie;
    }

    public Set<UtilisateurEntity> getUtilisateurEntity() {
        return utilisateurEntity;
    }

    public void setUtilisateurEntity(Set<UtilisateurEntity> utilisateurEntity) {
        this.utilisateurEntity = utilisateurEntity;
    }
}
