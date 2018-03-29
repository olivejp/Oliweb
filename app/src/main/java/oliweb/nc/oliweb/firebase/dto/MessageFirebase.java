package oliweb.nc.oliweb.firebase.dto;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class MessageFirebase {
    private String uidAnnonce;
    private String uidAuthor;
    private String uidReceiver;
    private String message;
    private long timestamp;
    private boolean read;

    public MessageFirebase() {
    }

    public MessageFirebase(String uidAnnonce, String uidAuthor, String uidReceiver, String message, long timestamp, boolean read) {
        this.uidAnnonce = uidAnnonce;
        this.uidAuthor = uidAuthor;
        this.uidReceiver = uidReceiver;
        this.message = message;
        this.timestamp = timestamp;
        this.read = read;
    }

    public String getUidAnnonce() {
        return uidAnnonce;
    }

    public void setUidAnnonce(String uidAnnonce) {
        this.uidAnnonce = uidAnnonce;
    }

    public String getUidAuthor() {
        return uidAuthor;
    }

    public void setUidAuthor(String uidAuthor) {
        this.uidAuthor = uidAuthor;
    }

    public String getUidReceiver() {
        return uidReceiver;
    }

    public void setUidReceiver(String uidReceiver) {
        this.uidReceiver = uidReceiver;
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

    @Override
    public String toString() {
        return "MessageFirebase{" +
                "uidAnnonce='" + uidAnnonce + '\'' +
                ", uidAuthor='" + uidAuthor + '\'' +
                ", uidReceiver='" + uidReceiver + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", read=" + read +
                '}';
    }
}
