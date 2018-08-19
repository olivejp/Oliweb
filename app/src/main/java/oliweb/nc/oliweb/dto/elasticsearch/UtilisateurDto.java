package oliweb.nc.oliweb.dto.elasticsearch;

/**
 * Created by orlanth23 on 20/02/2018.
 */

public class UtilisateurDto {
    private String profile;
    private String uuid;
    private String telephone;
    private String email;
    private String photoUrl;

    public UtilisateurDto() {
    }

    public UtilisateurDto(String profile, String uuid, String telephone, String email, String photoUrl) {
        this.profile = profile;
        this.uuid = uuid;
        this.telephone = telephone;
        this.email = email;
        this.photoUrl = photoUrl;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    @Override
    public String toString() {
        return "UtilisateurDto{" +
                "profile='" + profile + '\'' +
                ", uuid='" + uuid + '\'' +
                ", telephone='" + telephone + '\'' +
                ", email='" + email + '\'' +
                ", photoUrl='" + photoUrl + '\'' +
                '}';
    }
}
