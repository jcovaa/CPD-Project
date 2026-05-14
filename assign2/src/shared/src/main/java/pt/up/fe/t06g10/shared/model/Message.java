package pt.up.fe.t06g10.shared.model;

import java.time.Instant;

public class Message {
    private final String sender;
    private final String content;
    private final String room;
    private final Instant timestamp;

    public Message(String sender, String content, String room, Instant timestamp) {
        this.sender = sender;
        this.content = content;
        this.room = room;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public String getRoom() {
        return room;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
