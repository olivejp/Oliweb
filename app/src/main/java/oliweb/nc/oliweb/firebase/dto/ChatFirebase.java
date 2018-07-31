package oliweb.nc.oliweb.firebase.dto;

import java.util.Map;

/**
 * Created by 2761oli on 23/03/2018.
 */

public class ChatFirebase {

    private String uid;
    private String uidBuyer;
    private String uidSeller;
    private String uidAnnonce;
    private String lastMessage;
    private String titreAnnonce;
    private long creationTimestamp;
    private long updateTimestamp;
    private Map<String, Boolean> members;

    public ChatFirebase() {
    }

    public ChatFirebase(String uid, String uidBuyer, String uidSeller, String uidAnnonce, String lastMessage, long creationTimestamp, long updateTimestamp, Map<String, Boolean> members, String titreAnnonce) {
        this.uid = uid;
        this.uidBuyer = uidBuyer;
        this.uidSeller = uidSeller;
        this.uidAnnonce = uidAnnonce;
        this.lastMessage = lastMessage;
        this.creationTimestamp = creationTimestamp;
        this.updateTimestamp = updateTimestamp;
        this.titreAnnonce = titreAnnonce;
        this.members = members;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
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

    public ChatFirebase setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
        return this;
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

    public ChatFirebase setUpdateTimestamp(long updateTimestamp) {
        this.updateTimestamp = updateTimestamp;
        return this;
    }

    public Map<String, Boolean> getMembers() {
        return members;
    }

    public void setMembers(Map<String, Boolean> members) {
        this.members = members;
    }

    public String getTitreAnnonce() {
        return titreAnnonce;
    }

    public void setTitreAnnonce(String titreAnnonce) {
        this.titreAnnonce = titreAnnonce;
    }

    @Override
    public String toString() {
        return "ChatFirebase{" +
                "uid='" + uid + '\'' +
                ", uidBuyer='" + uidBuyer + '\'' +
                ", uidSeller='" + uidSeller + '\'' +
                ", uidAnnonce='" + uidAnnonce + '\'' +
                ", lastMessage='" + lastMessage + '\'' +
                ", titreAnnonce='" + titreAnnonce + '\'' +
                ", creationTimestamp=" + creationTimestamp +
                ", updateTimestamp=" + updateTimestamp +
                ", members=" + members +
                '}';
    }
}
