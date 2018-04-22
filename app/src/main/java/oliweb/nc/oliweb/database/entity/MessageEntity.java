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

@Entity(tableName = "message", foreignKeys = @ForeignKey(entity = ChatEntity.class, parentColumns = "uidChat", childColumns = "uidChat", onDelete = CASCADE), indices = @Index("uidChat"))
public class MessageEntity implements Parcelable {
    @NonNull
    @PrimaryKey
    private String uidMessage;
    private String message;
    private String read;
    private String uidAuthor;
    private Long timestamp;
    private String uidChat;
    @TypeConverters(StatusConverter.class)
    private StatusRemote statusRemote;

    public MessageEntity() {
    }

    @Ignore
    public MessageEntity(@NonNull String uidMessage, String message, String read, String uidAuthor, Long timestamp, String uidChat, StatusRemote statusRemote) {
        this.uidMessage = uidMessage;
        this.message = message;
        this.read = read;
        this.uidAuthor = uidAuthor;
        this.timestamp = timestamp;
        this.uidChat = uidChat;
        this.statusRemote = statusRemote;
    }

    @NonNull
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

    public String getUidChat() {
        return uidChat;
    }

    public void setUidChat(String uidChat) {
        this.uidChat = uidChat;
    }

    public StatusRemote getStatusRemote() {
        return statusRemote;
    }

    public void setStatusRemote(StatusRemote statusRemote) {
        this.statusRemote = statusRemote;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.uidMessage);
        dest.writeString(this.message);
        dest.writeString(this.read);
        dest.writeString(this.uidAuthor);
        dest.writeValue(this.timestamp);
        dest.writeString(this.uidChat);
        dest.writeInt(this.statusRemote == null ? -1 : this.statusRemote.ordinal());
    }

    protected MessageEntity(Parcel in) {
        this.uidMessage = in.readString();
        this.message = in.readString();
        this.read = in.readString();
        this.uidAuthor = in.readString();
        this.timestamp = (Long) in.readValue(Long.class.getClassLoader());
        this.uidChat = in.readString();
        int tmpStatut = in.readInt();
        this.statusRemote = tmpStatut == -1 ? null : StatusRemote.values()[tmpStatut];
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
}
