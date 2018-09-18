package oliweb.nc.oliweb.dto.firebase;

import com.google.firebase.database.PropertyName;

import java.util.List;

/**
 * Created by 2761oli on 20/02/2018.
 *
 * Firebase Annonce Model
 */
public class AnnonceFirebase {
    private UserFirebase utilisateur;
    private List<String> photos;
    private CategorieFirebase categorie;
    private String uuid;
    private String titre;
    private String description;

    private boolean contactEmail;
    private boolean contactTel;
    private boolean contactMsg;

    private int prix;

    @PropertyName("date_publication")
    private long datePublication;

    public AnnonceFirebase() {
    }

    public UserFirebase getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(UserFirebase utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public CategorieFirebase getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieFirebase categorie) {
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

    public boolean isContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(boolean contactEmail) {
        this.contactEmail = contactEmail;
    }

    public boolean isContactTel() {
        return contactTel;
    }

    public void setContactTel(boolean contactTel) {
        this.contactTel = contactTel;
    }

    public boolean isContactMsg() {
        return contactMsg;
    }

    public void setContactMsg(boolean contactMsg) {
        this.contactMsg = contactMsg;
    }

    @Override
    public String toString() {
        return "AnnonceFirebase{" +
                "utilisateur=" + utilisateur +
                ", photos=" + photos +
                ", categorie=" + categorie +
                ", uuid='" + uuid + '\'' +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", contactEmail=" + contactEmail +
                ", contactTel=" + contactTel +
                ", contactMsg=" + contactMsg +
                ", prix=" + prix +
                ", datePublication=" + datePublication +
                '}';
    }
}
