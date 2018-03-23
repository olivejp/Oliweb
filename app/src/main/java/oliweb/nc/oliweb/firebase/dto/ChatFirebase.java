package oliweb.nc.oliweb.firebase.dto;

import java.util.HashMap;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ChatFirebase {
    String uidAnnonce;
    String lastMessage;
    long creationTimestamp;
    long updateTimestamp;
    HashMap<String, Boolean> members;

    public ChatFirebase() {
    }

    public ChatFirebase(String uidAnnonce, String lastMessage, long creationTimestamp, long updateTimestamp, HashMap<String, Boolean> members) {
        this.uidAnnonce = uidAnnonce;
        this.lastMessage = lastMessage;
        this.creationTimestamp = creationTimestamp;
        this.updateTimestamp = updateTimestamp;
        this.members = members;
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

    public long getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public long getUpdateTimestamp() {
        return updateTimestamp;
    }

    public void setUpdateTimestamp(long updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
    }

    public HashMap<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(HashMap<String, Boolean> members) {
        this.members = members;
    }
}
