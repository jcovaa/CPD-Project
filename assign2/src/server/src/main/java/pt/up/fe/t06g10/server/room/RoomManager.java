package pt.up.fe.t06g10.server.room;

import pt.up.fe.t06g10.server.ai.AiService;
import pt.up.fe.t06g10.server.entity.MessageEntity;
import pt.up.fe.t06g10.server.entity.RoomEntity;
import pt.up.fe.t06g10.server.entity.UserEntity;
import pt.up.fe.t06g10.server.model.Message;
import pt.up.fe.t06g10.server.repository.MessageRepository;
import pt.up.fe.t06g10.server.repository.RoomMemberRepository;
import pt.up.fe.t06g10.server.repository.RoomRepository;
import pt.up.fe.t06g10.server.repository.UserRepository;
import pt.up.fe.t06g10.server.util.PasswordUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private static final String DEFAULT_BOT_NAME = "Bot";
    private static final int MAX_AI_HISTORY = 50;

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SessionManager sessionManager;
    private final AiService aiService;
    private final Map<String, String> aiRoomPrompts = new ConcurrentHashMap<>();
    private final String botName;

    public RoomManager(RoomRepository roomRepository, RoomMemberRepository roomMemberRepository, MessageRepository messageRepository, UserRepository userRepository, SessionManager sessionManager, AiService aiService) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
        this.aiService = aiService;
        this.botName = DEFAULT_BOT_NAME;
        loadAiPrompts();
        ensureBotUser();
    }

    public boolean roomExists(String roomName) {
        return roomRepository.existsByName(roomName);
    }

    public void createRoom(String roomName) {
        createRoom(roomName, null);
    }

    public void createRoom(String roomName, String prompt) {
        String normalizedPrompt = normalizePrompt(prompt);
        roomRepository.saveIfNotExists(new RoomEntity(roomName, normalizedPrompt));
        if (normalizedPrompt != null) {
            aiRoomPrompts.put(roomName, normalizedPrompt);
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

        return saveMessage(room.get(), user.get(), content);
    }

    public boolean hasAiPrompt(String roomName) {
        if (aiRoomPrompts.containsKey(roomName)) {
            return true;
        }
        Optional<RoomEntity> room = roomRepository.findByName(roomName);
        if (room.isEmpty()) {
            return false;
        }
        String prompt = normalizePrompt(room.get().getPrompt());
        if (prompt == null) {
            return false;
        }
        aiRoomPrompts.put(roomName, prompt);
        return true;
    }

    public void triggerAiReply(String roomName) {
        String prompt = aiRoomPrompts.get(roomName);
        if (prompt == null) {
            return;
        }
        Optional<RoomEntity> room = roomRepository.findByName(roomName);
        if (room.isEmpty()) {
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                List<Message> history = buildAiHistory(room.get());
                String botReply = aiService.query(prompt, history, botName);
                saveBotMessage(room.get(), botReply);
                sessionManager.broadcastToRoom(roomName, botName + ": " + botReply);
            } catch (Exception e) {
                sessionManager.broadcastToRoom(roomName, botName + ": [error: " + e.getMessage() + "]");
            }
        });
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

    private List<Message> buildAiHistory(RoomEntity room) {
        List<MessageEntity> entities = messageRepository.findRecentByRoom(room, MAX_AI_HISTORY);
        List<Message> history = new ArrayList<>(entities.size());
        for (int i = entities.size() - 1; i >= 0; i--) {
            MessageEntity entity = entities.get(i);
            String sender = entity.getSender().getUsername();
            history.add(new Message(sender, entity.getContent(), room.getName(), entity.getCreatedAt()));
        }
        return history;
    }

    private String normalizePrompt(String prompt) {
        if (prompt == null) {
            return null;
        }
        String trimmed = prompt.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void loadAiPrompts() {
        for (RoomEntity room : roomRepository.findAllWithPrompt()) {
            String prompt = normalizePrompt(room.getPrompt());
            if (prompt != null) {
                aiRoomPrompts.put(room.getName(), prompt);
            }
        }
    }

    private Message saveMessage(RoomEntity room, UserEntity user, String content) {
        MessageEntity entity = new MessageEntity(room, user, content);
        messageRepository.save(entity);
        return new Message(user.getUsername(), content, room.getName(), entity.getCreatedAt());
    }

    private void saveBotMessage(RoomEntity room, String content) {
        Optional<UserEntity> botUser = userRepository.findByUsername(botName);
        if (botUser.isEmpty()) {
            ensureBotUser();
            botUser = userRepository.findByUsername(botName);
            if (botUser.isEmpty()) {
                return;
            }
        }
        saveMessage(room, botUser.get(), content);
    }

    private void ensureBotUser() {
        if (userRepository.existsByUsername(botName)) {
            return;
        }
        String salt = PasswordUtils.generateSalt();
        String password = UUID.randomUUID().toString();
        String hash = PasswordUtils.hash(password, salt);
        userRepository.save(new UserEntity(botName, hash, salt));
    }
}
