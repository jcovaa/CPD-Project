package pt.up.fe.t06g10.shared.model;

import java.util.ArrayList;
import java.util.List;

public class Room {
    private final String name;
    private final List<Message> messages;
    private final List<String> activeUsers;

    public Room(String name) {
        this.name = name;
        this.messages = new ArrayList<>();
        this.activeUsers = new ArrayList<>();
    }
}
