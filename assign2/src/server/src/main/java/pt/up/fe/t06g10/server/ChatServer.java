package pt.up.fe.t06g10.server;

import pt.up.fe.t06g10.server.auth.AuthService;
import pt.up.fe.t06g10.server.auth.TokenService;
import pt.up.fe.t06g10.server.room.SessionManager;
import pt.up.fe.t06g10.shared.Protocol;
import pt.up.fe.t06g10.shared.database.UserDatabase;

import java.io.*;
import java.net.*;

public class ChatServer {
    private static final String USER_DB_FILE = "users.txt";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: ChatServer <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        UserDatabase userDB;

        try {
            userDB = new UserDatabase(USER_DB_FILE);
        } catch (IOException e) {
            System.out.println("Failed to load user database: " + e.getMessage());
            return;
        }

        TokenService tokenService = new TokenService();
        AuthService authService = new AuthService(userDB, tokenService);
        SessionManager sessionManager = new SessionManager();

        System.out.println("Server starting on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                ConnectionHandler handler = new ConnectionHandler(socket, authService, tokenService, sessionManager);
                Thread.startVirtualThread(handler::handle);
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
        }
    }
}