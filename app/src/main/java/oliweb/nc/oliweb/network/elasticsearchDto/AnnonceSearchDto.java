package oliweb.nc.oliweb.network.elasticsearchDto;

import com.google.firebase.database.PropertyName;

import java.util.List;

/**
 * Created by 2761oli on 20/02/2018.
 */

public class AnnonceSearchDto {
    private UtilisateurSearchDto utilisateur;
    private List<String> photos;
    private CategorieSearchDto categorie;
    private String uuid;
    private String titre;
    private String description;
    private int prix;

    @PropertyName("date_publication")
    private long datePublication;

    public UtilisateurSearchDto getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(UtilisateurSearchDto utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public CategorieSearchDto getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieSearchDto categorie) {
        this.categorie = categorie;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrix() {
        return prix;
    }

    public void setPrix(int prix) {
        this.prix = prix;
    }

    public List<String> getPhotos() {
        return photos;
    }

    public void setPhotos(List<String> photos) {
        this.photos = photos;
    }

    public long getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(long datePublication) {
        this.datePublication = datePublication;
    }
}
