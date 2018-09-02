package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.firebase.database.Exclude;

/**
 * Created by orlanth23 on 28/01/2018.
 */
@Entity(tableName = "utilisateur", indices = {@Index(value = "uid")})
public class UserEntity extends AbstractEntity<Long> implements Parcelable {
    @Exclude
    @NonNull
    @PrimaryKey
    private Long idUser;
    private String uid;
    private String telephone;
    private String email;
    private String profile;
    private Long dateCreation;
    private Long dateLastConnexion;
    private String photoUrl;
    private String tokenDevice;
    @TypeConverters(StatusConverter.class)
    private StatusRemote statut;
    private Integer favorite;

    @Exclude
    @NonNull
    public Long getId() {
        return idUser;
    }

    @Exclude
    @NonNull
    public Long getIdUser() {
        return idUser;
    }

    public void setIdUser(@NonNull Long idUser) {
        this.idUser = idUser;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(@NonNull String uid) {
        this.uid = uid;
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

    public Integer getFavorite() {
        return favorite;
    }

    public void setFavorite(Integer favorite) {
        this.favorite = favorite;
    }

    @Exclude
    public StatusRemote getStatut() {
        return statut;
    }

    public void setStatut(StatusRemote statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "idUser=" + idUser +
                ", uid='" + uid + '\'' +
                ", telephone='" + telephone + '\'' +
                ", email='" + email + '\'' +
                ", profile='" + profile + '\'' +
                ", dateCreation=" + dateCreation +
                ", dateLastConnexion=" + dateLastConnexion +
                ", photoUrl='" + photoUrl + '\'' +
                ", tokenDevice='" + tokenDevice + '\'' +
                ", statut=" + statut +
                ", favorite=" + favorite +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.idUser);
        dest.writeString(this.uid);
        dest.writeString(this.telephone);
        dest.writeString(this.email);
        dest.writeString(this.profile);
        dest.writeValue(this.dateCreation);
        dest.writeValue(this.dateLastConnexion);
        dest.writeString(this.photoUrl);
        dest.writeString(this.tokenDevice);
        dest.writeInt(this.statut == null ? -1 : this.statut.ordinal());
        dest.writeValue(this.favorite);
    }

    public UserEntity() {
    }

    protected UserEntity(Parcel in) {
        this.idUser = (Long) in.readValue(Long.class.getClassLoader());
        this.uid = in.readString();
        this.telephone = in.readString();
        this.email = in.readString();
        this.profile = in.readString();
        this.dateCreation = (Long) in.readValue(Long.class.getClassLoader());
        this.dateLastConnexion = (Long) in.readValue(Long.class.getClassLoader());
        this.photoUrl = in.readString();
        this.tokenDevice = in.readString();
        int tmpStatut = in.readInt();
        this.statut = tmpStatut == -1 ? null : StatusRemote.values()[tmpStatut];
        this.favorite = (Integer) in.readValue(Integer.class.getClassLoader());
    }

    public static final Creator<UserEntity> CREATOR = new Creator<UserEntity>() {
        @Override
        public UserEntity createFromParcel(Parcel source) {
            return new UserEntity(source);
        }

        @Override
        public UserEntity[] newArray(int size) {
            return new UserEntity[size];
        }
    };
}
