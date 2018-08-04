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

import static android.arch.persistence.room.ForeignKey.CASCADE;

/**
 * Created by orlanth23 on 19/04/2018.
 */

@Entity(tableName = "message", foreignKeys = @ForeignKey(entity = ChatEntity.class, parentColumns = "idChat", childColumns = "idChat", onDelete = CASCADE), indices = {@Index("idChat"), @Index(value = "uidMessage", unique = true)})
public class MessageEntity extends AbstractEntity<Long> implements Parcelable {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    private Long idMessage;
    private String uidMessage;
    private String message;
    private String read;
    @NonNull
    private String uidAuthor;
    private String uidChat;
    private Long timestamp;
    @NonNull
    private Long idChat;
    @TypeConverters(StatusConverter.class)
    private StatusRemote statusRemote;

    public MessageEntity() {
    }

    @Ignore
    public MessageEntity(@NonNull Long idMessage, String uidMessage, String message, String read, String uidAuthor, String uidChat, Long timestamp, Long idChat, StatusRemote statusRemote) {
        this.idMessage = idMessage;
        this.uidMessage = uidMessage;
        this.message = message;
        this.read = read;
        this.uidAuthor = uidAuthor;
        this.uidChat = uidChat;
        this.timestamp = timestamp;
        this.idChat = idChat;
        this.statusRemote = statusRemote;
    }

    @NonNull
    public Long getId() {
        return idMessage;
    }

    @NonNull
    public Long getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(@NonNull Long idMessage) {
        this.idMessage = idMessage;
    }

    public Long getIdChat() {
        return idChat;
    }

    public void setIdChat(Long idChat) {
        this.idChat = idChat;
    }

    public String getUidMessage() {
        return uidMessage;
    }

    public void setUidMessage(@NonNull String uidMessage) {
        this.uidMessage = uidMessage;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRead() {
        return read;
    }

    public void setRead(String read) {
        this.read = read;
    }

    @NonNull
    public String getUidAuthor() {
        return uidAuthor;
    }

    public void setUidAuthor(String uidAuthor) {
        this.uidAuthor = uidAuthor;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public StatusRemote getStatusRemote() {
        return statusRemote;
    }

    public void setStatusRemote(StatusRemote statusRemote) {
        this.statusRemote = statusRemote;
    }

    public String getUidChat() {
        return uidChat;
    }

    public void setUidChat(String uidChat) {
        this.uidChat = uidChat;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(this.idMessage);
        dest.writeString(this.uidMessage);
        dest.writeString(this.message);
        dest.writeString(this.read);
        dest.writeString(this.uidAuthor);
        dest.writeString(this.uidChat);
        dest.writeValue(this.timestamp);
        dest.writeValue(this.idChat);
        dest.writeInt(this.statusRemote == null ? -1 : this.statusRemote.ordinal());
    }

    protected MessageEntity(Parcel in) {
        this.idMessage = (Long) in.readValue(Long.class.getClassLoader());
        this.uidMessage = in.readString();
        this.message = in.readString();
        this.read = in.readString();
        this.uidAuthor = in.readString();
        this.uidChat = in.readString();
        this.timestamp = (Long) in.readValue(Long.class.getClassLoader());
        this.idChat = (Long) in.readValue(Long.class.getClassLoader());
        int tmpStatusRemote = in.readInt();
        this.statusRemote = tmpStatusRemote == -1 ? null : StatusRemote.values()[tmpStatusRemote];
    }

    public static final Creator<MessageEntity> CREATOR = new Creator<MessageEntity>() {
        @Override
        public MessageEntity createFromParcel(Parcel source) {
            return new MessageEntity(source);
        }

        @Override
        public MessageEntity[] newArray(int size) {
            return new MessageEntity[size];
        }
    };

    @Override
    public String toString() {
        return "MessageEntity{" +
                "idMessage=" + idMessage +
                ", uidMessage='" + uidMessage + '\'' +
                ", message='" + message + '\'' +
                ", read='" + read + '\'' +
                ", uidAuthor='" + uidAuthor + '\'' +
                ", uidChat='" + uidChat + '\'' +
                ", timestamp=" + timestamp +
                ", idChat=" + idChat +
                ", statusRemote=" + statusRemote +
                '}';
    }
}
