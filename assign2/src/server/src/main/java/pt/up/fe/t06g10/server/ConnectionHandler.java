package pt.up.fe.t06g10.server;

import pt.up.fe.t06g10.server.auth.AuthService;
import pt.up.fe.t06g10.server.auth.TokenService;
import pt.up.fe.t06g10.server.room.SessionManager;
import pt.up.fe.t06g10.shared.Protocol;
import pt.up.fe.t06g10.shared.model.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConnectionHandler {
    private static final String[] PRE_AUTH_COMMANDS = { "AUTH", "TOKEN", "RECONNECT", "REGISTER" };

    private final Socket socket;
    private final AuthService authService;
    private final TokenService tokenService;
    private final SessionManager sessionManager;

    private boolean authenticated = false;
    private String currentToken = null;
    private String currentUsername = null;

    public ConnectionHandler(Socket socket, AuthService authService, TokenService tokenService, SessionManager sessionManager) {
        this.socket = socket;
        this.authService = authService;
        this.tokenService = tokenService;
        this.sessionManager = sessionManager;
    }

    public void handle() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = Protocol.parse(line);
                if (parts.length == 0) continue;

                String command = parts[0];
                String args = parts.length > 1 ? parts[1] : "";

                if (!authenticated && !isPreAuthCommand(command)) {
                    writer.println(Protocol.UNAUTHORIZED + " Authentication required");
                    continue;
                }

                String response = processCommand(command, args);
                writer.println(response);
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            cleanup();
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private boolean isPreAuthCommand(String command) {
        String upperCmd = command.toUpperCase();
        for (String preAuth : PRE_AUTH_COMMANDS) {
            if (preAuth.equals(upperCmd)) {
                return true;
            }
        }
        return false;
    }

    private String processCommand(String command, String args) {
        try {
            switch (command.toUpperCase()) {
                case "AUTH": {
                    return handleAuth(args);
                }

                case "TOKEN": {
                    return handleToken(args);
                }

                case "RECONNECT": {
                    return handleReconnect(args);
                }

                case "REGISTER": {
                    if (authenticated) {
                        return Protocol.BAD_REQUEST + " Already authenticated";
                    }
                    return handleRegister(args);
                }

                case "LOGOUT": {
                    return handleLogout(args);
                }

                case "LIST_ROOMS": {
                    return handleListRooms();
                }

                case "CREATE_ROOM": {
                    return handleCreateRoom(args);
                }

                case "JOIN_ROOM": {
                    return handleJoinRoom(args);
                }

                case "LEAVE_ROOM": {
                    return handleLeaveRoom();
                }

                case "SEND": {
                    return handleSend(args);
                }

                case "HISTORY": {
                    return handleHistory(args);
                }

                default:
                    return Protocol.BAD_REQUEST + " Unknown command: " + command;
            }
        } catch (Exception e) {
            return Protocol.INTERNAL_ERROR + " " + e.getMessage();
        }
    }

    private String handleAuth(String args) {
        if (authenticated) {
            return Protocol.BAD_REQUEST + " Already authenticated";
        }

        String[] creds = args.split("\\s+", 2);
        if (creds.length < 2) {
            return Protocol.BAD_REQUEST + " Usage: AUTH <username> <password>";
        }

        String username = creds[0];
        String password = creds[1];

        try {
            Session session = authService.authenticate(username, password);
            this.currentToken = session.getToken();
            this.currentUsername = username;
            this.authenticated = true;

            sessionManager.registerSession(currentToken, session);

            return Protocol.OK + " " + session.getToken();
        } catch (AuthService.AuthException e) {
            return Protocol.UNAUTHORIZED + " " + e.getMessage();
        }
    }

    private String handleToken(String args) {
        if (authenticated) {
            return Protocol.BAD_REQUEST + " Already authenticated";
        }

        Session session = tokenService.validateToken(args);
        if (session == null) {
            return Protocol.UNAUTHORIZED + " Invalid or expired token";
        }

        this.currentToken = args;
        this.currentUsername = session.getUsername();
        this.authenticated = true;

        sessionManager.registerSession(currentToken, session);

        return Protocol.OK + " " + currentUsername;
    }

    private String handleReconnect(String args) {
        if (authenticated) {
            return Protocol.BAD_REQUEST + " Already authenticated";
        }

        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            return Protocol.BAD_REQUEST + " Usage: RECONNECT <username> <token>";
        }

        String username = parts[0];
        String token = parts[1];

        Session session = tokenService.validateToken(token);
        if (session == null || !session.getUsername().equals(username)) {
            return Protocol.UNAUTHORIZED + " Invalid token for user";
        }

        this.currentToken = token;
        this.currentUsername = username;
        this.authenticated = true;

        sessionManager.registerSession(currentToken, session);

        return Protocol.OK + " Reconnected successfully";
    }

    private String handleRegister(String args) {
        String[] creds = args.split("\\s+", 2);
        if (creds.length < 2) {
            return Protocol.BAD_REQUEST + " Usage: REGISTER <username> <password>";
        }

        String username = creds[0];
        String password = creds[1];

        try {
            authService.registerUser(username, password);
            return Protocol.OK + " User registered successfully";
        } catch (AuthService.AuthException e) {
            return Protocol.USER_EXISTS + " " + e.getMessage();
        }
    }

    private String handleLogout(String args) {
        if (!authenticated) {
            return Protocol.BAD_REQUEST + " Not authenticated";
        }

        if (currentToken != null) {
            tokenService.removeSession(currentToken);
            sessionManager.unregisterSession(currentToken);
        }

        authenticated = false;
        currentToken = null;
        currentUsername = null;

        return Protocol.OK + " Logged out successfully";
    }

    private String handleListRooms() {
        return Protocol.NOT_FOUND + " No rooms implemented yet";
    }

    private String handleCreateRoom(String args) {
        return Protocol.NOT_FOUND + " Room creation not implemented yet";
    }

    private String handleJoinRoom(String args) {
        return Protocol.NOT_FOUND + " Room joining not implemented yet";
    }

    private String handleLeaveRoom() {
        return Protocol.NOT_FOUND + " Room leaving not implemented yet";
    }

    private String handleSend(String args) {
        return Protocol.NOT_FOUND + " Messaging not implemented yet";
    }

    private String handleHistory(String args) {
        return Protocol.NOT_FOUND + " History not implemented yet";
    }

    private void cleanup() {
        if (currentToken != null) {
            sessionManager.unregisterSession(currentToken);
        }
    }
}