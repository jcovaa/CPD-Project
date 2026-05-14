package pt.up.fe.t06g10.server.room;

import pt.up.fe.t06g10.server.entity.MessageEntity;
import pt.up.fe.t06g10.server.entity.RoomEntity;
import pt.up.fe.t06g10.server.entity.UserEntity;
import pt.up.fe.t06g10.server.repository.MessageRepository;
import pt.up.fe.t06g10.server.repository.RoomMemberRepository;
import pt.up.fe.t06g10.server.repository.RoomRepository;
import pt.up.fe.t06g10.server.repository.UserRepository;
import pt.up.fe.t06g10.shared.model.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomManager {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public RoomManager(RoomRepository roomRepository, RoomMemberRepository roomMemberRepository, MessageRepository messageRepository, UserRepository userRepository) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public boolean roomExists(String roomName) {
        return roomRepository.existsByName(roomName);
    }

    public void createRoom(String roomName) {
        if (!roomRepository.existsByName(roomName)) {
            roomRepository.save(new RoomEntity(roomName));
        }
    }

    public List<String> listRoomNames() {
        List<RoomEntity> rooms = roomRepository.findAll();
        List<String> names = new ArrayList<>(rooms.size());
        for (RoomEntity room : rooms) {
            names.add(room.getName());
        }
        return names;
    }

    public void addUserToRoom(String roomName, String username) {
        Optional<RoomEntity> room = roomRepository.findByName(roomName);
        Optional<UserEntity> user = userRepository.findByUsername(username);
        if (room.isEmpty() || user.isEmpty()) {
            return;
        }
        if (!roomMemberRepository.existsByRoomAndUser(room.get(), user.get())) {
            roomMemberRepository.addMember(room.get(), user.get());
        }
    }

    public void removeUserFromRoom(String roomName, String username) {
        Optional<RoomEntity> room = roomRepository.findByName(roomName);
        Optional<UserEntity> user = userRepository.findByUsername(username);
        if (room.isEmpty() || user.isEmpty()) {
            return;
        }
        roomMemberRepository.removeMember(room.get(), user.get());
    }

    public boolean isUserInRoom(String roomName, String username) {
        Optional<RoomEntity> room = roomRepository.findByName(roomName);
        Optional<UserEntity> user = userRepository.findByUsername(username);
        if (room.isEmpty() || user.isEmpty()) {
            return false;
        }
        return roomMemberRepository.existsByRoomAndUser(room.get(), user.get());
    }

    public Message addMessage(String roomName, String sender, String content) {
        Optional<RoomEntity> room = roomRepository.findByName(roomName);
        Optional<UserEntity> user = userRepository.findByUsername(sender);
        if (room.isEmpty() || user.isEmpty()) {
            return null;
        }

        MessageEntity entity = new MessageEntity(room.get(), user.get(), content);
        messageRepository.save(entity);
        return new Message(sender, content, roomName, entity.getCreatedAt());
    }

    public List<Message> getHistory(String roomName, int count) {
        Optional<RoomEntity> room = roomRepository.findByName(roomName);
        if (room.isEmpty()) {
            return List.of();
        }

        List<MessageEntity> entities = messageRepository.findRecentByRoom(room.get(), count);
        List<Message> messages = new ArrayList<>(entities.size());
        for (int i = entities.size() - 1; i >= 0; i--) {
            MessageEntity entity = entities.get(i);
            String sender = entity.getSender().getUsername();
            messages.add(new Message(sender, entity.getContent(), roomName, entity.getCreatedAt()));
        }
        return messages;
    }
}
