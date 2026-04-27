package pt.up.fe.t06g10.server.auth;

import pt.up.fe.t06g10.shared.database.UserDatabase;
import pt.up.fe.t06g10.shared.database.UserDatabase.UserExistsException;
import pt.up.fe.t06g10.shared.model.Session;

public class AuthService {
    private final UserDatabase userDatabase;
    private final TokenService tokenService;

    public AuthService(UserDatabase userDatabase, TokenService tokenService) {
        this.userDatabase = userDatabase;
        this.tokenService = tokenService;
    }

    public Session authenticate(String username, String password) throws AuthException {
        if (username == null || username.isBlank()) {
            throw new AuthException("Username cannot be empty");
        }
        if (password == null || password.isBlank()) {
            throw new AuthException("Password cannot be empty");
        }

        if (!userDatabase.validateUser(username, password)) {
            throw new AuthException("Invalid credentials");
        }

        return tokenService.createSession(username);
    }

    public void registerUser(String username, String password) throws AuthException {
        if (username == null || username.isBlank()) {
            throw new AuthException("Username cannot be empty");
        }
        if (password == null || password.isBlank()) {
            throw new AuthException("Password cannot be empty");
        }

        try {
            userDatabase.registerUser(username, password);
        } catch (UserExistsException e) {
            throw new AuthException("User already exists: " + username);
        } catch (java.io.IOException e) {
            throw new AuthException("Failed to register user: " + e.getMessage());
        }
    }

    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }
}
