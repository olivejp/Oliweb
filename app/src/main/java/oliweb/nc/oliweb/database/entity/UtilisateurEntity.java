package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Entity(tableName = "utilisateur")
public class UtilisateurEntity {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    private Long idUtilisateur;
    private String UUID;
    private Integer telephone;
    private String email;
    private Long dateCreation;
    private Long dateLastConnexion;
    private String statut;

    @NonNull
    public Long getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(@NonNull Long idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public Integer getTelephone() {
        return telephone;
    }

    public void setTelephone(Integer telephone) {
        this.telephone = telephone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Long dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Long getDateLastConnexion() {
        return dateLastConnexion;
    }

    public void setDateLastConnexion(Long dateLastConnexion) {
        this.dateLastConnexion = dateLastConnexion;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
