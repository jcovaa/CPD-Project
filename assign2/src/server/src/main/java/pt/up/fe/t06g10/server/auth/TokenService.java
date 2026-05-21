package pt.up.fe.t06g10.server.auth;

import pt.up.fe.t06g10.server.model.Session;
import pt.up.fe.t06g10.shared.util.ThreadSafeMap;

public class TokenService {
    private final ThreadSafeMap<String, Session> sessions;
    private final int sessionDurationMinutes;

    public TokenService() {
        this(60);
    }

    public TokenService(int sessionDurationMinutes) {
        this.sessions = new ThreadSafeMap<>();
        this.sessionDurationMinutes = sessionDurationMinutes;
    }

    public Session createSession(String username) {
        Session session = new Session(username, sessionDurationMinutes);
        sessions.put(session.getToken(), session);
        return session;
    }

    public Session validateToken(String token) {
        Session session = sessions.get(token);

        if (session == null) return null;

        if (!session.isValid()) {
            sessions.remove(token);
            return null;
        }
        return session;
    }

    public void removeSession(String token) {
        sessions.remove(token);
    }

    public int getSessionCount() {
        return sessions.size();
    }
}
