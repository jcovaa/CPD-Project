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

    public String getName() {
        return name;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public List<String> getActiveUsers() {
        return activeUsers;
    }

    public void addUser(String username) {
        if (!activeUsers.contains(username)) {
            activeUsers.add(username);
        }
    }

    public void removeUser(String username) {
        activeUsers.remove(username);
    }

    public boolean hasUser(String username) {
        return activeUsers.contains(username);
    }

    public void addMessage(Message message) {
        messages.add(message);
    }
}
