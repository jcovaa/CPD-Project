package pt.up.fe.t06g10.server.model;

import java.time.Instant;
import java.util.UUID;

public class Session {
    private static final int DEFAULT_DURATION_MINUTES = 60;
    private final String token;
    private final String username;
    private final Instant createdAt;
    private final Instant expiresAt;

    public Session(String username) {
        this(username, DEFAULT_DURATION_MINUTES);
    }

    public Session(String username, int durationMinutes) {
        this.token = UUID.randomUUID().toString();
        this.username = username;
        this.createdAt = Instant.now();
        this.expiresAt = createdAt.plusSeconds(durationMinutes * 60L);
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isValid() {
        return Instant.now().isBefore(expiresAt);
    }

    public boolean isValid(String token) {
        return this.token.equals(token) && isValid();
    }
}
