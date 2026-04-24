package pt.up.fe.t06g10.shared;

public class Protocol {
    public static final String OK = "200";
    public static final String BAD_REQUEST = "400";
    public static final String UNAUTHORIZED = "401";
    public static final String NOT_FOUND = "404";
    public static final String INTERNAL_ERROR = "500";

    public enum ClientCommand {
        AUTH("username", "password"),
        TOKEN("token"),
        RECONNECT("token"),
        LIST_ROOMS(""),
        JOIN_ROOM("roomName"),
        CREATE_ROOM("roomName", "[prompt]"),
        LEAVE_ROOM(""),
        SEND("message"),
        BOT("room", "prompt", "context"),
        QUIT("");

        public final String[] argNames;

        ClientCommand(String... argNames) {
            this.argNames = argNames;
        }
    }

    public enum ServerResponse {
        AUTH_RESPONSE("code", "message|token"),
        LIST_ROOMS_RESPONSE("code", "rooms..."),
        JOIN_ROOM_RESPONSE("code", "message"),
        CREATE_ROOM_RESPONSE("code", "message"),
        MESSAGE("sender", "content"),
        ROOM_HISTORY("code", "messages..."),
        ERROR("code", "message"),
        OK("code", "message");

        public final String[] argNames;

        ServerResponse(String... argNames) {
            this.argNames = argNames;
        }
    }

    public static String[] parse(String line) {
        if (line == null || line.isBlank()) return new String[0];
        return line.trim().split("\\s+", -1);
    }

    public static boolean isValidClientCommand(String line) {
        String[] parts = parse(line);
        if (parts.length == 0) return false;
        try {
            ClientCommand.valueOf(parts[0].toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
