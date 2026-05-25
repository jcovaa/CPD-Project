package pt.up.fe.t06g10.server.room;

import pt.up.fe.t06g10.server.ai.AiService;
import pt.up.fe.t06g10.shared.model.Message;
import pt.up.fe.t06g10.shared.model.Room;

import java.util.ArrayList;
import java.util.List;

public class AiRoom extends Room {
    private static final String DEFAULT_BOT_NAME = "Bot";
    private static final int MAX_HISTORY = 50;

    private final String systemPrompt;
    private final AiService aiService;
    private final SessionManager sessionManager;
    private final RoomManager roomManager;
    private final String botName;

    public AiRoom(String name, String systemPrompt, AiService aiService, SessionManager sessionManager, RoomManager roomManager) {
        this(name, systemPrompt, aiService, sessionManager, roomManager, DEFAULT_BOT_NAME);
    }

    public AiRoom(String name, String systemPrompt, AiService aiService, SessionManager sessionManager, RoomManager roomManager, String botName) {
        super(name);
        this.systemPrompt = systemPrompt == null ? "" : systemPrompt;
        this.aiService = aiService;
        this.sessionManager = sessionManager;
        this.roomManager = roomManager;
        this.botName = botName == null || botName.isBlank() ? DEFAULT_BOT_NAME : botName;
    }

    @Override
    protected void onMessageAdded(List<Message> historySnapshot, Message addedMessage) {
        if (addedMessage.getSender().equalsIgnoreCase(botName)) {
            return;
        }

        List<Message> context = trimHistory(historySnapshot);
        Thread.ofVirtual().start(() -> {
            try {
                String botReply = aiService.query(systemPrompt, context, botName);
                roomManager.addMessage(getName(), botName, botReply, false);
                sessionManager.broadcastToRoom(getName(), botName + ": " + botReply);
            } catch (Exception e) {
                sessionManager.broadcastToRoom(getName(), botName + ": [error: " + e.getMessage() + "]");
            }
        });
    }

    private List<Message> trimHistory(List<Message> historySnapshot) {
        if (historySnapshot.size() <= MAX_HISTORY) {
            return historySnapshot;
        }
        return new ArrayList<>(historySnapshot.subList(historySnapshot.size() - MAX_HISTORY, historySnapshot.size()));
    }
}

