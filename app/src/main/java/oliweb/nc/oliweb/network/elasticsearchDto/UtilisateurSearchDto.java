package oliweb.nc.oliweb.network.elasticsearchDto;

/**
 * Created by orlanth23 on 20/02/2018.
 */

public class UtilisateurSearchDto {
    private String profile;
    private String uuid;
    private String telephone;
    private String email;

    public UtilisateurSearchDto() {
    }

    public UtilisateurSearchDto(String profile, String uuid, String telephone, String email) {
        this.profile = profile;
        this.uuid = uuid;
        this.telephone = telephone;
        this.email = email;
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
}
