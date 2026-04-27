package pt.up.fe.t06g10.server.auth;

import pt.up.fe.t06g10.shared.model.Session;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TokenService {
    private final Map<String, Session> sessions;
    private final int sessionDurationMinutes;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public TokenService() {
        this(60);
    }

    public TokenService(int sessionDurationMinutes) {
        this.sessions = new HashMap<>();
        this.sessionDurationMinutes = sessionDurationMinutes;
    }

    public Session createSession(String username) {
        lock.writeLock().lock();
        try {
            Session session = new Session(username, sessionDurationMinutes);
            sessions.put(session.getToken(), session);
            return session;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Session validateToken(String token) {
        lock.readLock().lock();
        Session session;
        try {
            session = sessions.get(token);
        } finally {
            lock.readLock().unlock();
        }

        if (session == null) return null;

        if (!session.isValid()) {
            lock.writeLock().lock();
            try {
                sessions.remove(token);
            } finally {
                lock.writeLock().unlock();
            }
            return null;
        }
        return session;
    }

    public void removeSession(String token) {
        lock.writeLock().lock();
        try {
            sessions.remove(token);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getSessionCount() {
        lock.readLock().lock();
        try {
            return sessions.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}