package pt.up.fe.t06g10.shared;

public class Protocol {
    public static final String OK = "200";
    public static final String BAD_REQUEST = "400";
    public static final String UNAUTHORIZED = "401";
    public static final String NOT_FOUND = "404";
    public static final String INTERNAL_ERROR = "500";

    public enum Command {
        // Authentication
        AUTH, RECONNECT,

        // Room operations
        LIST_ROOMS, JOIN_ROOM, CREATE_ROOM, LEAVE_ROOM,

        // Messaging
        SEND, MESSAGE,

        // System
        ERROR, OK
    }

    public static String[] parse(String line) {
        if (line == null || line.isBlank()) return new String[0];
        return line.trim().split("\\s+", 2);
    }
}