package oliweb.nc.oliweb.dto.firebase;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by orlanth23 on 20/02/2018.
 */

public class UserFirebase implements Parcelable {
    private String profile;
    private String uuid;
    private String telephone;
    private String email;
    private String photoUrl;

    public UserFirebase() {
    }

    public UserFirebase(String profile, String uuid, String telephone, String email, String photoUrl) {
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
        return "UserFirebase{" +
                "profile='" + profile + '\'' +
                ", uuid='" + uuid + '\'' +
                ", telephone='" + telephone + '\'' +
                ", email='" + email + '\'' +
                ", photoUrl='" + photoUrl + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.profile);
        dest.writeString(this.uuid);
        dest.writeString(this.telephone);
        dest.writeString(this.email);
        dest.writeString(this.photoUrl);
    }

    protected UserFirebase(Parcel in) {
        this.profile = in.readString();
        this.uuid = in.readString();
        this.telephone = in.readString();
        this.email = in.readString();
        this.photoUrl = in.readString();
    }

    public static final Parcelable.Creator<UserFirebase> CREATOR = new Parcelable.Creator<UserFirebase>() {
        @Override
        public UserFirebase createFromParcel(Parcel source) {
            return new UserFirebase(source);
        }

        @Override
        public UserFirebase[] newArray(int size) {
            return new UserFirebase[size];
        }
    };
}
