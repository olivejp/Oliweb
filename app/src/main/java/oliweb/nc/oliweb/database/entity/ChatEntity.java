package oliweb.nc.oliweb.database.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Created by orlanth23 on 28/01/2018.
 */

@Entity(tableName = "chat", indices = {@Index(value = {"uidChat", "uidBuyer"}, unique = true)})
public class ChatEntity extends AbstractEntity<Long> implements Parcelable {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    private Long idChat;
    private String uidChat;
    private String uidBuyer;
    private String uidSeller;
    private String uidAnnonce;
    private String titreAnnonce;
    private String lastMessage;
    private Long creationTimestamp;
    private Long updateTimestamp;
    @TypeConverters(StatusConverter.class)
    private StatusRemote statusRemote;

    public ChatEntity() {
    }

    @Ignore
    public ChatEntity(@NonNull Long idChat, String uidChat, String uidBuyer, String uidSeller, String uidAnnonce, String lastMessage, Long creationTimestamp, Long updateTimestamp, StatusRemote statusRemote, String titreAnnonce) {
        this.idChat = idChat;
        this.uidChat = uidChat;
        this.uidBuyer = uidBuyer;
        this.uidSeller = uidSeller;
        this.uidAnnonce = uidAnnonce;
        this.titreAnnonce = titreAnnonce;
        this.lastMessage = lastMessage;
        this.creationTimestamp = creationTimestamp;
        this.updateTimestamp = updateTimestamp;
        this.statusRemote = statusRemote;
    }

    @NonNull
    @Override
    public Long getId() {
        return idChat;
    }

    @NonNull
    public Long getIdChat() {
        return idChat;
    }

    public void setIdChat(@NonNull Long idChat) {
        this.idChat = idChat;
    }

    public String getUidChat() {
        return uidChat;
    }

    public void setUidChat(@NonNull String uidChat) {
        this.uidChat = uidChat;
    }

    public String getUidBuyer() {
        return uidBuyer;
    }

    public void setUidBuyer(String uidBuyer) {
        this.uidBuyer = uidBuyer;
    }

    public String getUidSeller() {
        return uidSeller;
    }

    public void setUidSeller(String uidSeller) {
        this.uidSeller = uidSeller;
    }

    public String getUidAnnonce() {
        return uidAnnonce;
    }

    public void setUidAnnonce(String uidAnnonce) {
        this.uidAnnonce = uidAnnonce;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public Long getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(Long updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public StatusRemote getStatusRemote() {
        return statusRemote;
    }

    public void setStatusRemote(StatusRemote statusRemote) {
        this.statusRemote = statusRemote;
    }

    public String getTitreAnnonce() {
        return titreAnnonce;
    }

    public void setTitreAnnonce(String titreAnnonce) {
        this.titreAnnonce = titreAnnonce;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatEntity)) return false;
        ChatEntity that = (ChatEntity) o;
        return Objects.equals(idChat, that.idChat) &&
                Objects.equals(uidChat, that.uidChat) &&
                Objects.equals(uidBuyer, that.uidBuyer) &&
                Objects.equals(uidSeller, that.uidSeller) &&
                Objects.equals(uidAnnonce, that.uidAnnonce) &&
                Objects.equals(titreAnnonce, that.titreAnnonce) &&
                Objects.equals(lastMessage, that.lastMessage) &&
                Objects.equals(creationTimestamp, that.creationTimestamp) &&
                Objects.equals(updateTimestamp, that.updateTimestamp) &&
                statusRemote == that.statusRemote;
    }

    @Override
    public int hashCode() {

        return Objects.hash(idChat, uidChat, uidBuyer, uidSeller, uidAnnonce, titreAnnonce, lastMessage, creationTimestamp, updateTimestamp, statusRemote);
    }

    @Override
    public String toString() {
        return "ChatEntity{" +
                "idChat=" + idChat +
                ", uidChat='" + uidChat + '\'' +
                ", uidBuyer='" + uidBuyer + '\'' +
                ", uidSeller='" + uidSeller + '\'' +
                ", uidAnnonce='" + uidAnnonce + '\'' +
                ", titreAnnonce='" + titreAnnonce + '\'' +
                ", lastMessage='" + lastMessage + '\'' +
                ", creationTimestamp=" + creationTimestamp +
                ", updateTimestamp=" + updateTimestamp +
                ", statusRemote=" + statusRemote +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.idChat);
        dest.writeString(this.uidChat);
        dest.writeString(this.uidBuyer);
        dest.writeString(this.uidSeller);
        dest.writeString(this.uidAnnonce);
        dest.writeString(this.titreAnnonce);
        dest.writeString(this.lastMessage);
        dest.writeValue(this.creationTimestamp);
        dest.writeValue(this.updateTimestamp);
        dest.writeInt(this.statusRemote == null ? -1 : this.statusRemote.ordinal());
    }

    protected ChatEntity(Parcel in) {
        this.idChat = (Long) in.readValue(Long.class.getClassLoader());
        this.uidChat = in.readString();
        this.uidBuyer = in.readString();
        this.uidSeller = in.readString();
        this.uidAnnonce = in.readString();
        this.titreAnnonce = in.readString();
        this.lastMessage = in.readString();
        this.creationTimestamp = (Long) in.readValue(Long.class.getClassLoader());
        this.updateTimestamp = (Long) in.readValue(Long.class.getClassLoader());
        int tmpStatusRemote = in.readInt();
        this.statusRemote = tmpStatusRemote == -1 ? null : StatusRemote.values()[tmpStatusRemote];
    }

    public static final Creator<ChatEntity> CREATOR = new Creator<ChatEntity>() {
        @Override
        public ChatEntity createFromParcel(Parcel source) {
            return new ChatEntity(source);
        }

        @Override
        public ChatEntity[] newArray(int size) {
            return new ChatEntity[size];
        }
    };
}
