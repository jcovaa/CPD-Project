package pt.up.fe.t06g10.shared.model;

import java.util.ArrayList;
import java.util.Collections;
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
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
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
        addMessage(message, true);
    }

    public void addMessage(Message message, boolean notify) {
        List<Message> snapshot;
        synchronized (messages) {
            messages.add(message);
            snapshot = new ArrayList<>(messages);
        }
        if (notify) {
            onMessageAdded(Collections.unmodifiableList(snapshot), message);
        }
    }

    protected void onMessageAdded(List<Message> historySnapshot, Message addedMessage) {
        // no-op in base class
    }
}
