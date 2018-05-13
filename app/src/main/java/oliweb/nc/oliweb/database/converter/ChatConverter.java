package oliweb.nc.oliweb.database.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        chatEntity.setTitreAnnonce(chatFirebase.getTitreAnnonce());
        chatEntity.setStatusRemote(StatusRemote.SEND);
        return chatEntity;
    }


    public static List<ChatEntity> convertDtoToEntity(List<ChatFirebase> listChatFirebase) {
        ArrayList<ChatEntity> listResult = new ArrayList<>();
        for (ChatFirebase chatFirebase : listChatFirebase) {
            listResult.add(convertDtoToEntity(chatFirebase));
        }
        return listResult;
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
        chatFirebase.setCreationTimestamp(chatEntity.getCreationTimestamp());
        chatFirebase.setUpdateTimestamp(chatEntity.getUpdateTimestamp());
        return chatFirebase;
    }
}
