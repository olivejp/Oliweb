package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Entity(tableName = "annonce", foreignKeys = {
        @ForeignKey(entity = CategorieEntity.class, parentColumns = "idCategorie", childColumns = "idCategorie"),
        @ForeignKey(entity = UtilisateurEntity.class, parentColumns = "idUtilisateur", childColumns = "idUtilisateur")})
public class AnnonceEntity {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    private Long idAnnonce;
    private String UUID;
    private String titre;
    private String description;
    private Long datePublication;
    private Integer prix;
    private String statut;
    private String contactByTel;
    private String contactByEmail;
    private String contactByMsg;
    private Integer idUtilisateur;
    private Integer idCategorie;

    @NonNull
    public Long getIdAnnonce() {
        return idAnnonce;
    }

    public void setIdAnnonce(@NonNull Long idAnnonce) {
        this.idAnnonce = idAnnonce;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
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

    public Long getDatePublication() {
        return datePublication;
    }

    public void setDatePublication(Long datePublication) {
        this.datePublication = datePublication;
    }

    public Integer getPrix() {
        return prix;
    }

    public void setPrix(Integer prix) {
        this.prix = prix;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getContactByTel() {
        return contactByTel;
    }

    public void setContactByTel(String contactByTel) {
        this.contactByTel = contactByTel;
    }

    public String getContactByEmail() {
        return contactByEmail;
    }

    public void setContactByEmail(String contactByEmail) {
        this.contactByEmail = contactByEmail;
    }

    public String getContactByMsg() {
        return contactByMsg;
    }

    public void setContactByMsg(String contactByMsg) {
        this.contactByMsg = contactByMsg;
    }

    public Integer getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(Integer idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public Integer getIdCategorie() {
        return idCategorie;
    }

    public void setIdCategorie(Integer idCategorie) {
        this.idCategorie = idCategorie;
    }
}
