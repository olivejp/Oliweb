package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.google.firebase.database.Exclude;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Entity(tableName = "annonce", foreignKeys = {
        @ForeignKey(entity = CategorieEntity.class, parentColumns = "idCategorie", childColumns = "idCategorie"),
        @ForeignKey(entity = UtilisateurEntity.class, parentColumns = "UuidUtilisateur", childColumns = "UuidUtilisateur")},
        indices = {
                @Index("UuidUtilisateur"),
                @Index("idCategorie")})
public class AnnonceEntity {
    @Exclude
    @NonNull
    @PrimaryKey(autoGenerate = true)
    private Long idAnnonce;
    private String UUID;
    private String titre;
    private String description;
    private Long datePublication;
    private Integer prix;
    private String contactByTel;
    private String contactByEmail;
    private String contactByMsg;
    @Exclude
    @TypeConverters(StatusConverter.class)
    private StatusRemote statut;
    private String UuidUtilisateur;
    private Long idCategorie;
    private String debattre;

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

    public StatusRemote getStatut() {
        return statut;
    }

    public void setStatut(StatusRemote statut) {
        this.statut = statut;
    }

    public String getUuidUtilisateur() {
        return UuidUtilisateur;
    }

    public void setUuidUtilisateur(String uuidUtilisateur) {
        UuidUtilisateur = uuidUtilisateur;
    }

    public Long getIdCategorie() {
        return idCategorie;
    }

    public void setIdCategorie(Long idCategorie) {
        this.idCategorie = idCategorie;
    }

    public String getDebattre() {
        return debattre;
    }

    public void setDebattre(String debattre) {
        this.debattre = debattre;
    }
}
