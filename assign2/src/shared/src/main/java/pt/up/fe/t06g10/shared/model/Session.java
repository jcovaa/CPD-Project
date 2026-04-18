package pt.up.fe.t06g10.shared.model;

import java.util.UUID;

public class Session {
    private final String token;

    public Session() {
        this.token = UUID.randomUUID().toString();
    }
}
