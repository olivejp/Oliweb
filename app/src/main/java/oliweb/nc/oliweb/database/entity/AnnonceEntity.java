package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Ignore;
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

@Entity(tableName = "annonce", foreignKeys = @ForeignKey(entity = CategorieEntity.class, parentColumns = "idCategorie", childColumns = "idCategorie"),
        indices = {
                @Index("uidUser"),
                @Index("idCategorie")})
public class AnnonceEntity extends AbstractEntity<Long> implements Parcelable {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    private Long idAnnonce;
    private String uid;
    private String titre;
    private String description;
    private Long datePublication;
    private Integer prix;
    private String contactByTel;
    private String contactByEmail;
    private String contactByMsg;
    @TypeConverters(StatusConverter.class)
    private StatusRemote statut;
    private String uidUser;
    private long idCategorie;
    private String debattre;
    private Integer favorite;
    private String uidUserFavorite;

    public AnnonceEntity() {
    }

    @Ignore
    public AnnonceEntity(@NonNull Long idAnnonce, String uid, String titre, String description, StatusRemote statut, String uidUser, long idCategorie, String uidUserFavorite) {
        this.idAnnonce = idAnnonce;
        this.uid = uid;
        this.titre = titre;
        this.description = description;
        this.statut = statut;
        this.uidUser = uidUser;
        this.idCategorie = idCategorie;
        this.uidUserFavorite = uidUserFavorite;
    }

    @NonNull
    @Override
    public Long getId() {
        return idAnnonce;
    }

    @NonNull
    public Long getIdAnnonce() {
        return idAnnonce;
    }

    public void setIdAnnonce(@NonNull Long idAnnonce) {
        this.idAnnonce = idAnnonce;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
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

    @Exclude
    public StatusRemote getStatut() {
        return statut;
    }

    public void setStatut(StatusRemote statut) {
        this.statut = statut;
    }

    @Exclude
    public String getUidUser() {
        return uidUser;
    }

    public void setUidUser(String uidUser) {
        this.uidUser = uidUser;
    }

    @Exclude
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

    public Integer getFavorite() {
        return favorite;
    }

    public void setFavorite(Integer favorite) {
        this.favorite = favorite;
    }

    public boolean isFavorite() {
        return favorite != null && favorite.equals(1);
    }

    @Exclude
    public String getUidUserFavorite() {
        return uidUserFavorite;
    }

    public void setUidUserFavorite(String uidUserFavorite) {
        this.uidUserFavorite = uidUserFavorite;
    }



    @Override
    public String toString() {
        return "AnnonceEntity{" +
                "idAnnonce=" + idAnnonce +
                ", uid='" + uid + '\'' +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", datePublication=" + datePublication +
                ", prix=" + prix +
                ", contactByTel='" + contactByTel + '\'' +
                ", contactByEmail='" + contactByEmail + '\'' +
                ", contactByMsg='" + contactByMsg + '\'' +
                ", statut=" + statut +
                ", uidUser='" + uidUser + '\'' +
                ", idCategorie=" + idCategorie +
                ", debattre='" + debattre + '\'' +
                ", favorite=" + favorite +
                ", uidUserFavorite='" + uidUserFavorite + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.idAnnonce);
        dest.writeString(this.uid);
        dest.writeString(this.titre);
        dest.writeString(this.description);
        dest.writeValue(this.datePublication);
        dest.writeValue(this.prix);
        dest.writeString(this.contactByTel);
        dest.writeString(this.contactByEmail);
        dest.writeString(this.contactByMsg);
        dest.writeInt(this.statut == null ? -1 : this.statut.ordinal());
        dest.writeString(this.uidUser);
        dest.writeLong(this.idCategorie);
        dest.writeString(this.debattre);
        dest.writeValue(this.favorite);
        dest.writeString(this.uidUserFavorite);
    }

    protected AnnonceEntity(Parcel in) {
        this.idAnnonce = (Long) in.readValue(Long.class.getClassLoader());
        this.uid = in.readString();
        this.titre = in.readString();
        this.description = in.readString();
        this.datePublication = (Long) in.readValue(Long.class.getClassLoader());
        this.prix = (Integer) in.readValue(Integer.class.getClassLoader());
        this.contactByTel = in.readString();
        this.contactByEmail = in.readString();
        this.contactByMsg = in.readString();
        int tmpStatut = in.readInt();
        this.statut = tmpStatut == -1 ? null : StatusRemote.values()[tmpStatut];
        this.uidUser = in.readString();
        this.idCategorie = in.readLong();
        this.debattre = in.readString();
        this.favorite = (Integer) in.readValue(Integer.class.getClassLoader());
        this.uidUserFavorite = in.readString();
    }

    public static final Creator<AnnonceEntity> CREATOR = new Creator<AnnonceEntity>() {
        @Override
        public AnnonceEntity createFromParcel(Parcel source) {
            return new AnnonceEntity(source);
        }

        @Override
        public AnnonceEntity[] newArray(int size) {
            return new AnnonceEntity[size];
        }
    };
}
