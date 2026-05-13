package pt.up.fe.t06g10.server.room;

import pt.up.fe.t06g10.shared.model.Session;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SessionManager {
    private final Map<String, UserSession> activeSessions;
    private final Map<String, Runnable> disconnectHandlers;

    public SessionManager() {
        this.activeSessions = new ConcurrentHashMap<>();
        this.disconnectHandlers = new ConcurrentHashMap<>();
    }

    public void registerSession(String token, Session session) {
        activeSessions.put(token, new UserSession(token, session.getUsername()));
    }

    public void unregisterSession(String token) {
        activeSessions.remove(token);
    }

    public UserSession getUserSession(String token) {
        return activeSessions.get(token);
    }

    public boolean isAuthenticated(String token) {
        UserSession userSession = activeSessions.get(token);
        return userSession != null;
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

    public Collection<UserSession> getAllUserSessions() {
        return Collections.unmodifiableCollection(activeSessions.values());
    }

    public Collection<UserSession> getUsersInRoom(String roomId) {
        return activeSessions.values().stream().filter(s -> roomId.equals(s.getCurrentRoom())).collect(Collectors.toList());
    }

    public void registerDisconnectHandler(String token, Runnable disconnectHandler) {
        Runnable old = disconnectHandlers.put(token, disconnectHandler);
        if (old != null) {
            old.run();
        }
    }

    public void unregisterDisconnectHandler(String token) {
        disconnectHandlers.remove(token);
    }

    public Runnable getDisconnectHandler(String token) {
        return disconnectHandlers.get(token);
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }

    public static class UserSession {
        private final String token;
        private final String username;
        private String currentRoom;

        public UserSession(String token, String username) {
            this.token = token;
            this.username = username;
            this.currentRoom = null;
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

        public boolean isInRoom() {
            return currentRoom != null;
        }
    }
}