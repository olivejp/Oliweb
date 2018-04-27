package oliweb.nc.oliweb.database.converter;

import oliweb.nc.oliweb.database.entity.ChatEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.firebase.dto.ChatFirebase;

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
        chatEntity.setStatusRemote(StatusRemote.SEND);
        return chatEntity;
    }
}
