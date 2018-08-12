package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Relation;

import java.util.List;

/**
 * Created by orlanth23 on 08/02/2018.
 */

public class AnnonceWithChats {
    @Embedded
    public AnnonceEntity annonce;

    @Relation(parentColumn = "uid", entityColumn = "uidAnnonce")
    public List<ChatEntity> chats;

    public AnnonceEntity getAnnonce() {
        return annonce;
    }

    public void setAnnonce(AnnonceEntity annonce) {
        this.annonce = annonce;
    }

    public List<ChatEntity> getChats() {
        return chats;
    }

    public void setChats(List<ChatEntity> chats) {
        this.chats = chats;
    }

    @Override
    public String toString() {
        return "AnnonceWithChats{" +
                "annonce=" + annonce +
                ", chats=" + chats +
                '}';
    }
}
