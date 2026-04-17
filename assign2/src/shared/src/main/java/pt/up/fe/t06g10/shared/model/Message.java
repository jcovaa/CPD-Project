package pt.up.fe.t06g10.shared.model;

import java.time.Instant;

public class Message {
    private final String sender;
    private final String content;
    private final String room;
    private final Instant timestamp;

    public Message(String sender, String content, String room) {
        this.sender = sender;
        this.content = content;
        this.room = room;
        this.timestamp = Instant.now();
    }
}
