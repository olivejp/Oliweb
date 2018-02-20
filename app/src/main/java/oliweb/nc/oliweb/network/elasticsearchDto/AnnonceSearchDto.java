package oliweb.nc.oliweb.network.elasticsearchDto;

import java.util.List;

/**
 * Created by 2761oli on 20/02/2018.
 */

public class AnnonceSearchDto {
    private String uuid;
    private long idCategorie;
    private String titre;
    private String description;
    private int prix;
    private List<PhotoSearchDto> photos;
    private long datePublication;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getIdCategorie() {
        return idCategorie;
    }

    public void setIdCategorie(long idCategorie) {
        this.idCategorie = idCategorie;
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

    public List<PhotoSearchDto> getPhotos() {
        return photos;
    }

    public void setPhotos(List<PhotoSearchDto> photos) {
        this.photos = photos;
    }

    public long getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(long datePublication) {
        this.datePublication = datePublication;
    }
}
