package oliweb.nc.oliweb.database.converter;

import java.util.HashMap;

import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.dto.firebase.ChatFirebase;

public class ChatConverter {

    private ChatConverter() {
    }

    public static ChatEntity convertDtoToEntity(ChatFirebase chatFirebase) {
        ChatEntity chatEntity = new ChatEntity();
        chatEntity.setUidChat(chatFirebase.getUid());
        chatEntity.setCreationTimestamp(chatFirebase.getCreationTimestamp());
        chatEntity.setUpdateTimestamp(chatFirebase.getUpdateTimestamp());
        chatEntity.setUidSeller(chatFirebase.getUidSeller());
        chatEntity.setUidBuyer(chatFirebase.getUidBuyer());
        chatEntity.setLastMessage(chatFirebase.getLastMessage());
        chatEntity.setUidAnnonce(chatFirebase.getUidAnnonce());
        chatEntity.setTitreAnnonce(chatFirebase.getTitreAnnonce());
        chatEntity.setStatusRemote(StatusRemote.SEND);
        return chatEntity;
    }

    public static ChatFirebase convertEntityToDto(ChatEntity chatEntity) {
        HashMap<String, Boolean> hash = new HashMap<>();
        hash.put(chatEntity.getUidBuyer(), true);
        hash.put(chatEntity.getUidSeller(), true);

        ChatFirebase chatFirebase = new ChatFirebase();
        chatFirebase.setUid(chatEntity.getUidChat());
        chatFirebase.setUidAnnonce(chatEntity.getUidAnnonce());
        chatFirebase.setMembers(hash);
        chatFirebase.setUidBuyer(chatEntity.getUidBuyer());
        chatFirebase.setUidSeller(chatEntity.getUidSeller());
        chatFirebase.setTitreAnnonce(chatEntity.getTitreAnnonce());
        chatFirebase.setLastMessage(chatEntity.getLastMessage());
        chatFirebase.setCreationTimestamp((chatEntity.getCreationTimestamp() != null) ? chatEntity.getCreationTimestamp() : 0L);
        chatFirebase.setUpdateTimestamp((chatEntity.getUpdateTimestamp() != null) ? chatEntity.getUpdateTimestamp() : 0L);
        return chatFirebase;
    }
}
