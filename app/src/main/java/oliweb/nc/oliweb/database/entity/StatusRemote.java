package oliweb.nc.oliweb.database.entity;

/**
 * Created by 2761oli on 29/01/2018.
 */

public enum StatusRemote {
    TO_SEND("TO_SEND"),
    SEND("SEND"),
    SENDING("SENDING"),
    TO_DELETE("TO_DELETE"),
    TO_UPDATE("TO_UPDATE"),
    DELETED("DELETED"),
    FAILED_TO_SEND("FAILED_TO_SEND"),
    FAILED_TO_DELETE("FAILED_TO_DELETE"),
    NOT_TO_SEND("NOT_TO_SEND"),
    TO_INSERT("TO_INSERT");

    private final String value;

    StatusRemote(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        return value;
    }
}
