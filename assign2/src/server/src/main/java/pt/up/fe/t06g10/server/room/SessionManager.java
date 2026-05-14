package pt.up.fe.t06g10.server.room;

import pt.up.fe.t06g10.server.connection.ClientWriter;
import pt.up.fe.t06g10.shared.model.Session;
import pt.up.fe.t06g10.shared.util.ThreadSafeMap;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

public class SessionManager {
    private final ThreadSafeMap<String, UserSession> activeSessions = new ThreadSafeMap<>();

    public void registerSession(String token, Session session, ClientWriter clientWriter) {
        activeSessions.put(token, new UserSession(token, session.getUsername(), clientWriter));
    }

    public void unregisterSession(String token) {
        activeSessions.remove(token);
    }

    public UserSession getUserSession(String token) {
        return activeSessions.get(token);
    }

    public ClientWriter getClientWriter(String token) {
        UserSession userSession = activeSessions.get(token);
        return userSession != null ? userSession.getWriter() : null;

    }

    public boolean isAuthenticated(String token) {
        return activeSessions.get(token) != null;
    }

    public void setUserRoom(String token, String roomId) {
        UserSession userSession = activeSessions.get(token);
        if (userSession != null) {
            userSession.setCurrentRoom(roomId);
        }
    }

    public String getUserRoom(String token) {
        UserSession userSession = activeSessions.get(token);
        return userSession != null ? userSession.getCurrentRoom() : null;
    }

    public void broadcastToRoom(String roomId, String message) {
        for (UserSession userSession : activeSessions.values()) {
            if (roomId.equals(userSession.getCurrentRoom()) && userSession.getWriter() != null) {
                userSession.getWriter().enqueue(message);
            }
        }
    }

    public Collection<UserSession> getAllUserSessions() {
        return Collections.unmodifiableCollection(activeSessions.values());
    }

    public Collection<UserSession> getUsersInRoom(String roomId) {
        return activeSessions.values().stream().filter(s -> roomId.equals(s.getCurrentRoom())).collect(Collectors.toList());
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public static class UserSession {
        private final String token;
        private final String username;
        private volatile String currentRoom;
        private final ClientWriter clientWriter;

        public UserSession(String token, String username, ClientWriter clientWriter) {
            this.token = token;
            this.username = username;
            this.currentRoom = null;
            this.clientWriter = clientWriter;
        }

        public String getToken() {
            return token;
        }

        public String getUsername() {
            return username;
        }

        public String getCurrentRoom() {
            return currentRoom;
        }

        public void setCurrentRoom(String currentRoom) {
            this.currentRoom = currentRoom;
        }

        public ClientWriter getWriter() { return clientWriter; }

        public boolean isInRoom() {
            return currentRoom != null;
        }
    }
}