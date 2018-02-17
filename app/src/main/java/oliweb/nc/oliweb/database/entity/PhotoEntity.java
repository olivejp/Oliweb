package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.google.firebase.database.Exclude;

import static android.arch.persistence.room.ForeignKey.CASCADE;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Entity(tableName = "photo",
        foreignKeys = @ForeignKey(entity = AnnonceEntity.class, parentColumns = "idAnnonce", childColumns = "idAnnonce", onDelete = CASCADE),
        indices = @Index(value = "idAnnonce"))
public class PhotoEntity {
    @Exclude
    @NonNull
    @PrimaryKey(autoGenerate = true)
    private Long idPhoto;
    private String UUID;
    @Exclude
    private String uriLocal;
    private String firebasePath;
    @Exclude
    @TypeConverters(StatusConverter.class)
    private StatusRemote statut;
    @Exclude
    private Long idAnnonce;

    @NonNull
    public Long getIdPhoto() {
        return idPhoto;
    }

    public void setIdPhoto(@NonNull Long idPhoto) {
        this.idPhoto = idPhoto;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

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

    public StatusRemote getStatut() {
        return statut;
    }

    public void setStatut(StatusRemote statut) {
        this.statut = statut;
    }

    public Long getIdAnnonce() {
        return idAnnonce;
    }

    public void setIdAnnonce(Long idAnnonce) {
        this.idAnnonce = idAnnonce;
    }
}
