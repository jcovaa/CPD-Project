package pt.up.fe.t06g10.server;

import pt.up.fe.t06g10.server.auth.AuthService;
import pt.up.fe.t06g10.server.auth.TokenService;
import pt.up.fe.t06g10.server.room.SessionManager;
import pt.up.fe.t06g10.shared.database.UserDatabase;

import java.io.IOException;

public class Main {
    private static final String USER_DB_FILE = "users.txt";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: Main <port>");
            System.exit(1);
        }

        int port = 0;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port: " + args[0]);
            System.exit(1);
        }

        UserDatabase userDB;
        try {
            userDB = new UserDatabase(USER_DB_FILE);
        } catch (IOException e) {
            System.err.println("Failed to load user database: " + e.getMessage());
            System.exit(1);
            return;
        }

        TokenService tokenService = new TokenService();
        AuthService authService = new AuthService(userDB, tokenService);
        SessionManager sessionManager = new SessionManager();

        ChatServer server = new ChatServer(port, authService, tokenService, sessionManager);
        server.start();
    }
}
