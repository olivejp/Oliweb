package oliweb.nc.oliweb.firebase.dto;

/**
 * Created by 2761oli on 22/03/2018.
 */

public class UtilisateurFirebase {
    private String profileName;
    private String telephone;
    private String photoPath;
    private String email;

    public UtilisateurFirebase() {
    }

    public UtilisateurFirebase(String profileName, String telephone, String photoPath, String email) {
        this.profileName = profileName;
        this.telephone = telephone;
        this.photoPath = photoPath;
        this.email = email;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}