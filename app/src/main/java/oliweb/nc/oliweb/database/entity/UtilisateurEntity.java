package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Entity(tableName = "utilisateur", indices = {@Index(value = "uuidUtilisateur", unique = true)})
public class UtilisateurEntity extends AbstractEntity<Long> {
    @NonNull
    @PrimaryKey
    private Long idUtilisateur;
    private String uuidUtilisateur;
    private String telephone;
    private String email;
    private String profile;
    private Long dateCreation;
    private Long dateLastConnexion;
    private String photoUrl;
    private String tokenDevice;

    @NonNull
    public Long getId() {
        return idUtilisateur;
    }

    @NonNull
    public Long getIdUtilisateur() {
        return idUtilisateur;
    }

    public void setIdUtilisateur(@NonNull Long idUtilisateur) {
        this.idUtilisateur = idUtilisateur;
    }

    public String getUuidUtilisateur() {
        return uuidUtilisateur;
    }

    public void setUuidUtilisateur(@NonNull String uuidUtilisateur) {
        this.uuidUtilisateur = uuidUtilisateur;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getTokenDevice() {
        return tokenDevice;
    }

    public void setTokenDevice(String tokenDevice) {
        this.tokenDevice = tokenDevice;
    }

    @Override
    public String toString() {
        return "UtilisateurEntity{" +
                "idUtilisateur=" + idUtilisateur +
                ", uuidUtilisateur='" + uuidUtilisateur + '\'' +
                ", telephone='" + telephone + '\'' +
                ", email='" + email + '\'' +
                ", profile='" + profile + '\'' +
                ", dateCreation=" + dateCreation +
                ", dateLastConnexion=" + dateLastConnexion +
                ", photoUrl='" + photoUrl + '\'' +
                ", tokenDevice='" + tokenDevice + '\'' +
                '}';
    }
}
