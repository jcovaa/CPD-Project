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

public class ConnectionHandler implements Runnable {
    private static final String[] PRE_AUTH_COMMANDS = {"AUTH", "TOKEN", "RECONNECT", "REGISTER", "HELP", "QUIT"};

    private final Socket socket;
    private final AuthService authService;
    private final TokenService tokenService;
    private final SessionManager sessionManager;

    private boolean authenticated = false;
    private String currentToken = null;
    private String currentUsername = null;
    private final Runnable disconnectHandler = this::disconnect;

    public ConnectionHandler(Socket socket, AuthService authService, TokenService tokenService, SessionManager sessionManager) {
        this.socket = socket;
        this.authService = authService;
        this.tokenService = tokenService;
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream())); PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println("SERVER DEBUG: Received line: '" + line + "'");
                String[] parts = Protocol.parse(line);
                System.err.println("SERVER DEBUG: Parsed parts: " + java.util.Arrays.toString(parts));
                if (parts.length == 0) continue;

                String command = parts[0];
                String args = parts.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length)) : "";
                System.err.println("SERVER DEBUG: command='" + command + "' args='" + args + "'");

                boolean knownCommand = Protocol.isValidClientCommand(command);
                if (!knownCommand) {
                    writer.println(Protocol.BAD_REQUEST + " Unknown command: " + command);
                    continue;
                }

                if (!authenticated && !isPreAuthCommand(command)) {
                    writer.println(Protocol.UNAUTHORIZED + " Authentication required");
                    continue;
                }

                String response = processCommand(command, args);
                writer.println(response);

                if (command.equalsIgnoreCase("QUIT")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            cleanup();
            try {
                socket.close();
            } catch (IOException ignored) {
            }
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
            return switch (command.toUpperCase()) {
                case "AUTH" -> handleAuth(args);
                case "TOKEN" -> handleToken(args);
                case "RECONNECT" -> handleReconnect(args);
                case "REGISTER" -> {
                    if (authenticated) {
                        yield Protocol.BAD_REQUEST + " Already authenticated";
                    }
                    yield handleRegister(args);
                }
                case "LOGOUT" -> handleLogout(args);
                case "LIST_ROOMS" -> handleListRooms();
                case "CREATE_ROOM" -> handleCreateRoom(args);
                case "JOIN_ROOM" -> handleJoinRoom(args);
                case "LEAVE_ROOM" -> handleLeaveRoom();
                case "SEND" -> handleSend(args);
                case "HISTORY" -> handleHistory(args);
                case "HELP" -> handleHelp();
                case "QUIT" -> handleQuit();
                default -> Protocol.BAD_REQUEST + " Unknown command: " + command;
            };
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
            sessionManager.registerDisconnectHandler(currentToken, disconnectHandler);

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
        sessionManager.registerDisconnectHandler(currentToken, disconnectHandler);

        return Protocol.OK + " " + currentUsername;
    }

    private String handleReconnect(String args) {
        if (authenticated) {
            return Protocol.BAD_REQUEST + " Already authenticated";
        }

        String token = args.trim();
        if (token.isEmpty()) {
            return Protocol.BAD_REQUEST + " Usage: RECONNECT <token>";
        }

        Session session = tokenService.validateToken(token);
        if (session == null) {
            return Protocol.UNAUTHORIZED + " Invalid or expired token";
        }

        String previousRoom = sessionManager.getUserRoom(token);

        this.currentToken = token;
        this.currentUsername = session.getUsername();
        this.authenticated = true;

        sessionManager.registerSession(currentToken, session);
        sessionManager.registerDisconnectHandler(currentToken, disconnectHandler);

        if (previousRoom != null) {
            sessionManager.setUserRoom(currentToken, previousRoom);
        }

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

    private String handleHelp() {
        return """
                --------------------------------------------------
                               WELCOME TO THE CHAT APP
                --------------------------------------------------
                
                Usage: <COMMAND> [arguments]
                
                Available commands:
                  AUTH <username> <password>        - Login with username and password
                  REGISTER <username> <password>    - Create a new user account
                  TOKEN <token>                     - Authenticate using a session token
                  RECONNECT <token>                  - Reconnect an existing session
                  LOGOUT                            - Log out of the current session
                  LIST_ROOMS                        - List available chat rooms
                  CREATE_ROOM <roomName> [prompt]   - Create a new room
                  JOIN_ROOM <roomName>              - Join the specified room
                  LEAVE_ROOM                        - Leave the current room
                  SEND <message>                    - Send a message to the current room
                  MESSAGE <roomName> <content>      - Send a message to a specific room
                  BOT <room> <prompt> <context>     - Ask the bot to post a message to a room
                  HISTORY <roomName> [count]        - Show recent messages from a room
                  HELP                              - Show this help text
                  QUIT                              - Disconnect
                
                Notes:
                  - Arguments in <> are required; in [] are optional.
                --------------------------------------------------
                """;
    }

    private String handleQuit() {
        cleanup();
        return Protocol.OK + " Logged out successfully. Closing the application";
    }

    private void cleanup() {
        if (currentToken != null) {
            Runnable registeredHandler = sessionManager.getDisconnectHandler(currentToken);
            if (registeredHandler == disconnectHandler) {
                sessionManager.unregisterSession(currentToken);
                sessionManager.unregisterDisconnectHandler(currentToken);
            }
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
