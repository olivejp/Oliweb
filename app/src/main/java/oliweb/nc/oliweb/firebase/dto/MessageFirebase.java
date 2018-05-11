package oliweb.nc.oliweb.firebase.dto;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class MessageFirebase {
    private String uidMessage;
    private String uidAuthor;
    private String uidChat;
    private String message;
    private long timestamp;
    private boolean read;

    public MessageFirebase() {
    }

    public MessageFirebase(String uidMessage, String uidAuthor, String uidChat, String message, long timestamp, boolean read) {
        this.uidMessage = uidMessage;
        this.uidAuthor = uidAuthor;
        this.uidChat = uidChat;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getUidMessage() {
        return uidMessage;
    }

    public void setUidMessage(String uidMessage) {
        this.uidMessage = uidMessage;
    }

    public String getUidAuthor() {
        return uidAuthor;
    }

    public void setUidAuthor(String uidAuthor) {
        this.uidAuthor = uidAuthor;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getUidChat() {
        return uidChat;
    }

    public void setUidChat(String uidChat) {
        this.uidChat = uidChat;
    }

    @Override
    public String toString() {
        return "MessageFirebase{" +
                "uidMessage='" + uidMessage + '\'' +
                ", uidAuthor='" + uidAuthor + '\'' +
                ", uidChat='" + uidChat + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", read=" + read +
                '}';
    }
}
