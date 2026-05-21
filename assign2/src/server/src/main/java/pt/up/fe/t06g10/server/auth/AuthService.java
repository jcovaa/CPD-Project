package pt.up.fe.t06g10.server.auth;

import pt.up.fe.t06g10.server.entity.UserEntity;
import pt.up.fe.t06g10.server.repository.UserRepository;
import pt.up.fe.t06g10.server.model.Session;
import pt.up.fe.t06g10.shared.util.PasswordUtils;

import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public Session authenticate(String username, String password) throws AuthException {
        if (username == null || username.isBlank()) {
            throw new AuthException("Username cannot be empty");
        }
        if (password == null || password.isBlank()) {
            throw new AuthException("Password cannot be empty");
        }

        Optional<UserEntity> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new AuthException("Invalid credentials");
        }

        UserEntity user = userOptional.get();
        if (!PasswordUtils.verify(password, user.getPasswordHash(), user.getSalt())) {
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

        if (userRepository.existsByUsername(username)) {
            throw new AuthException("User already exists: " + username);
        }

        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash(password, salt);
        userRepository.save(new UserEntity(username, hash, salt));
    }

    public static class AuthException extends Exception {
        public AuthException(String message) {
            super(message);
        }
    }
}
