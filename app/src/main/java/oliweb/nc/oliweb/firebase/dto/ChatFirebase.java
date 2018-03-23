package oliweb.nc.oliweb.firebase.dto;

import java.util.Map;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ChatFirebase {
    private String uidBuyer;
    private String uidSeller;
    private String uidAnnonce;
    private String lastMessage;
    private long creationTimestamp;
    private long updateTimestamp;
    private boolean amITheSeller;
    private Map<String, Boolean> members;

    public ChatFirebase() {
    }

    public ChatFirebase(String uidBuyer, String uidSeller, String uidAnnonce, String lastMessage, long creationTimestamp, long updateTimestamp, Map<String, Boolean> members, boolean amITheSeller) {
        this.uidBuyer = uidBuyer;
        this.uidSeller = uidSeller;
        this.uidAnnonce = uidAnnonce;
        this.lastMessage = lastMessage;
        this.creationTimestamp = creationTimestamp;
        this.updateTimestamp = updateTimestamp;
        this.members = members;
        this.amITheSeller = amITheSeller;
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

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }

    public boolean isAmITheSeller() {
        return amITheSeller;
    }

    public void setAmITheSeller(boolean amITheSeller) {
        this.amITheSeller = amITheSeller;
    }
}
