package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
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

@Entity(tableName = "photo",
        foreignKeys = @ForeignKey(entity = AnnonceEntity.class, parentColumns = "idAnnonce", childColumns = "idAnnonce"),
        indices = @Index(value = "idAnnonce"))
public class PhotoEntity implements Parcelable {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    private Long idPhoto;
    private String uriLocal;
    private String firebasePath;
    @TypeConverters(StatusConverter.class)
    private StatusRemote statut;
    private Long idAnnonce;

    @Exclude
    @NonNull
    public Long getIdPhoto() {
        return idPhoto;
    }

    public void setIdPhoto(@NonNull Long idPhoto) {
        this.idPhoto = idPhoto;
    }

    @Exclude
    public String getUriLocal() {
        return uriLocal;
    }

    public void setUriLocal(String uriLocal) {
        this.uriLocal = uriLocal;
    }

    public String getFirebasePath() {
        return firebasePath;
    }

    public void setFirebasePath(String firebasePath) {
        this.firebasePath = firebasePath;
    }

    @Exclude
    public StatusRemote getStatut() {
        return statut;
    }

    public void setStatut(StatusRemote statut) {
        this.statut = statut;
    }

    @Exclude
    public Long getIdAnnonce() {
        return idAnnonce;
    }

    public void setIdAnnonce(Long idAnnonce) {
        this.idAnnonce = idAnnonce;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.idPhoto);
        dest.writeString(this.uriLocal);
        dest.writeString(this.firebasePath);
        dest.writeInt(this.statut == null ? -1 : this.statut.ordinal());
        dest.writeValue(this.idAnnonce);
    }

    public PhotoEntity() {
    }

    protected PhotoEntity(Parcel in) {
        this.idPhoto = (Long) in.readValue(Long.class.getClassLoader());
        this.uriLocal = in.readString();
        this.firebasePath = in.readString();
        int tmpStatut = in.readInt();
        this.statut = tmpStatut == -1 ? null : StatusRemote.values()[tmpStatut];
        this.idAnnonce = (Long) in.readValue(Long.class.getClassLoader());
    }

    public static final Parcelable.Creator<PhotoEntity> CREATOR = new Parcelable.Creator<PhotoEntity>() {
        @Override
        public PhotoEntity createFromParcel(Parcel source) {
            return new PhotoEntity(source);
        }

        @Override
        public PhotoEntity[] newArray(int size) {
            return new PhotoEntity[size];
        }
    };
}
