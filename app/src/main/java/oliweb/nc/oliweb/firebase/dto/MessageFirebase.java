package oliweb.nc.oliweb.firebase.dto;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class MessageFirebase {
    String uidAnnonce;
    String uidAuthor;
    String uidReceiver;
    String message;
    long timestamp;
    boolean read;

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
}
