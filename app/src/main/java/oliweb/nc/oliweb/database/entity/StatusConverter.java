package oliweb.nc.oliweb.database.entity;

import android.arch.persistence.room.TypeConverter;


/**
 * Created by orlanth23 on 10/01/2018.
 */

public class StatusConverter {
    @TypeConverter
    public static StatusRemote getValue(String value) {
        if (value.equals(StatusRemote.TO_SEND.getValue())) {
            return StatusRemote.TO_SEND;
        } else if (value.equals(StatusRemote.TO_DELETE.getValue())) {
            return StatusRemote.TO_DELETE;
        } else if (value.equals(StatusRemote.SEND.getValue())) {
            return StatusRemote.SEND;
        } else if (value.equals(StatusRemote.DELETED.getValue())) {
            return StatusRemote.DELETED;
        } else {
            throw new IllegalArgumentException("Could not recognize status");
        }
    }

    @TypeConverter
    public static String toValue(StatusRemote origine) {
        return origine.getValue();
    }
}
