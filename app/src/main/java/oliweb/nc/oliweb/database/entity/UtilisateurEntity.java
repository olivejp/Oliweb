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
    @PrimaryKey
    private String UuidUtilisateur;
    private String telephone;
    private String email;
    private Long dateCreation;
    private Long dateLastConnexion;

    @NonNull
    public String getUuidUtilisateur() {
        return UuidUtilisateur;
    }

    public void setUuidUtilisateur(@NonNull String uuidUtilisateur) {
        UuidUtilisateur = uuidUtilisateur;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
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
}
