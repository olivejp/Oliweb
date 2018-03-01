package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

/**
 * Created by orlanth23 on 08/02/2018.
 */

public class AnnonceFull {
    @Embedded
    public AnnonceEntity annonce;

    @Relation(parentColumn = "idAnnonce", entityColumn = "idAnnonce")
    public List<PhotoEntity> photos;

    @Relation(parentColumn = "idCategorie", entityColumn = "idCategorie")
    public List<CategorieEntity> categorie;

    @Relation(parentColumn = "UuidUtilisateur", entityColumn = "UuidUtilisateur")
    public List<UtilisateurEntity> utilisateur;

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

    public List<UtilisateurEntity> getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(List<UtilisateurEntity> utilisateur) {
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
}
