package pt.up.fe.t06g10.server.room;

import pt.up.fe.t06g10.shared.model.Room;
import pt.up.fe.t06g10.shared.model.Message;
import pt.up.fe.t06g10.shared.util.ThreadSafeMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoomManager {
    private final ThreadSafeMap<String, Room> activeRooms;

    public RoomManager() {
        this.activeRooms = new ThreadSafeMap<>();
    }

    public boolean roomExists(String roomName) {
        return activeRooms.containsKey(roomName);
    }

    public void createRoom(String roomName) {
        Room existing = activeRooms.get(roomName);
        if (existing == null) {
            activeRooms.put(roomName, new Room(roomName));
        }
    }

    public Room getRoom(String roomName) {
        return activeRooms.get(roomName);
    }

    public List<String> listRoomNames() {
        return new ArrayList<>(activeRooms.keySet());
    }

    public void addUserToRoom(String roomName, String username) {
        Room room = activeRooms.get(roomName);
        if (room != null) {
            room.addUser(username);
        }
    }

    public void removeUserFromRoom(String roomName, String username) {
        Room room = activeRooms.get(roomName);
        if (room != null) {
            room.removeUser(username);
        }
    }

    public boolean isUserInRoom(String roomName, String username) {
        Room room = activeRooms.get(roomName);
        if (room == null) return false;
        return room.hasUser(username);
    }

    public Message addMessage(String roomName, String sender, String content) {
        Room room = activeRooms.get(roomName);
        if (room == null) return null;
        Message message = new Message(sender, content, roomName);
        room.addMessage(message);
        return message;
    }

    public List<Message> getHistory(String roomName, int count) {
        Room room = activeRooms.get(roomName);
        if (room == null) return Collections.emptyList();
        List<Message> messages = room.getMessages();
        int size = messages.size();
        if (count <= 0 || count >= size) {
            return new ArrayList<>(messages);
        }
        return new ArrayList<>(messages.subList(size - count, size));
    }
}
