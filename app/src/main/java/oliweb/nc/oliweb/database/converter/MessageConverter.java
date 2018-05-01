package oliweb.nc.oliweb.database.converter;

import java.util.ArrayList;
import java.util.List;

import oliweb.nc.oliweb.database.entity.MessageEntity;
import oliweb.nc.oliweb.database.entity.StatusRemote;
import oliweb.nc.oliweb.firebase.dto.MessageFirebase;

public class MessageConverter {

    private MessageConverter() {
    }

    public static MessageEntity convertDtoToEntity(String uidChat, MessageFirebase messageFirebase) {
        MessageEntity messageEntity = new MessageEntity();
        messageEntity.setUidChat(messageFirebase.getUidMessage());
        messageEntity.setMessage(messageFirebase.getMessage());
        messageEntity.setTimestamp(messageFirebase.getTimestamp());
        messageEntity.setUidAuthor(messageFirebase.getUidAuthor());
        messageEntity.setUidChat(uidChat);
        messageEntity.setStatusRemote(StatusRemote.SEND);
        return messageEntity;
    }

    public static List<MessageEntity> convertDtoToEntity(String uidChat, List<MessageFirebase> messagesFirebase) {
        ArrayList<MessageEntity> listResult = new ArrayList<>();
        for (MessageFirebase message : messagesFirebase) {
            listResult.add(convertDtoToEntity(uidChat, message));
        }
        return listResult;
    }
}
