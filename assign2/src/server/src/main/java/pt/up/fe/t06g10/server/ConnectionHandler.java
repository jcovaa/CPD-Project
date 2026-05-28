package pt.up.fe.t06g10.server;

import pt.up.fe.t06g10.server.auth.AuthService;
import pt.up.fe.t06g10.server.auth.TokenService;
import pt.up.fe.t06g10.server.connection.ClientWriter;
import pt.up.fe.t06g10.server.room.RoomManager;
import pt.up.fe.t06g10.server.room.SessionManager;
import pt.up.fe.t06g10.server.Protocol;
import pt.up.fe.t06g10.server.model.Message;
import pt.up.fe.t06g10.server.model.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ConnectionHandler implements Runnable {
    private static final String[] PRE_AUTH_COMMANDS = {"AUTH", "TOKEN", "RECONNECT", "REGISTER", "HELP", "QUIT"};

    private final Socket socket;
    private final AuthService authService;
    private final TokenService tokenService;
    private final SessionManager sessionManager;
    private final RoomManager roomManager;
    private final ClientWriter clientWriter;

    private volatile ClientWriter activeWriter;

    private boolean authenticated = false;
    private String currentToken = null;
    private String currentUsername = null;
    private final Runnable disconnectHandler = this::disconnect;

    public ConnectionHandler(Socket socket, AuthService authService, TokenService tokenService, SessionManager sessionManager, RoomManager roomManager, ClientWriter clientWriter) {
        this.socket = socket;
        this.authService = authService;
        this.tokenService = tokenService;
        this.sessionManager = sessionManager;
        this.roomManager = roomManager;
        this.clientWriter = clientWriter;
    }

    private void send(String message) {
        ClientWriter w = activeWriter != null ? activeWriter : clientWriter;
        if (w != null) {
            w.enqueue(message);
        }
    }

    @Override
    public void run() {
        clientWriter.start(socket.getRemoteSocketAddress().toString());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println("SERVER DEBUG: Received line: '" + line + "'");
                String[] parts = Protocol.parse(line);
                System.err.println("SERVER DEBUG: Parsed parts: " + Arrays.toString(parts));
                if (parts.length == 0) continue;

                String command = parts[0];
                String args = parts.length > 1 ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)) : "";
                System.err.println("SERVER DEBUG: command='" + command + "' args='" + args + "'");

                boolean knownCommand = Protocol.isValidClientCommand(command);
                if (!knownCommand) {
                    send(Protocol.BAD_REQUEST + " Unknown command: " + command);
                    continue;
                }

                if (!authenticated && !isPreAuthCommand(command)) {
                    send(Protocol.UNAUTHORIZED + " Authentication required");
                    continue;
                }

                String response = processCommand(command, args);
                if (response != null) send(response);

                if (command.equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        } finally {
            clientWriter.stop();
            try {
                clientWriter.awaitStop(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
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
                case "LOGOUT" -> handleLogout();
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
            this.activeWriter = clientWriter;

            sessionManager.registerSession(currentToken, session, clientWriter);
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
        this.activeWriter = clientWriter;

        sessionManager.registerSession(currentToken, session, clientWriter);
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
        this.activeWriter = clientWriter;

        sessionManager.registerSession(currentToken, session, clientWriter);
        sessionManager.registerDisconnectHandler(currentToken, disconnectHandler);

        if (previousRoom != null) {
            sessionManager.setUserRoom(currentToken, previousRoom);
        }

        return Protocol.OK + " Reconnected as " + currentUsername;
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

    private String handleLogout() {
        if (!authenticated) {
            return Protocol.BAD_REQUEST + " Not authenticated";
        }

        if (currentToken != null) {
            tokenService.removeSession(currentToken);
            sessionManager.unregisterSession(currentToken);
            sessionManager.unregisterDisconnectHandler(currentToken);
        }

        authenticated = false;
        currentToken = null;
        currentUsername = null;
        activeWriter = null;

        return Protocol.OK + " Logged out successfully";
    }

    private String handleListRooms() {
        List<String> rooms = roomManager.listRoomNames();
        if (rooms.isEmpty()) {
            return Protocol.OK + " (no rooms)";
        }
        return Protocol.OK + " " + String.join(",", rooms);
    }

    private String handleCreateRoom(String args) {
        String[] parts = args == null ? new String[0] : args.trim().split("\\s+", 2);
        String roomName = parts.length > 0 ? parts[0] : "";
        if (roomName.isEmpty()) {
            return Protocol.BAD_REQUEST + " Usage: CREATE_ROOM <roomName> [prompt]";
        }
        if (roomManager.roomExists(roomName)) {
            return Protocol.BAD_REQUEST + " Room already exists";
        }
        String prompt = parts.length > 1 ? parts[1] : null;
        roomManager.createRoom(roomName, prompt);
        return Protocol.OK + " Room created";
    }

    private String handleJoinRoom(String args) {
        String roomName = args == null ? "" : args.trim();
        if (roomName.isEmpty()) {
            return Protocol.BAD_REQUEST + " Usage: JOIN_ROOM <roomName>";
        }
        if (!roomManager.roomExists(roomName)) {
            return Protocol.NOT_FOUND + " Room not found";
        }
        if (currentToken == null) {
            return Protocol.UNAUTHORIZED + " Authentication required";
        }

        String currentRoom = sessionManager.getUserRoom(currentToken);
        if (currentRoom != null && currentRoom.equals(roomName)) {
            return Protocol.BAD_REQUEST + " Already in that room";
        }
        if (currentRoom != null) {
            roomManager.removeUserFromRoom(currentRoom, currentUsername);
            sessionManager.broadcastToRoom(currentRoom, "[" + currentUsername + " left the room]");
        }
        sessionManager.setUserRoom(currentToken, roomName);
        roomManager.addUserToRoom(roomName, currentUsername);
        sessionManager.broadcastToRoom(roomName, "[" + currentUsername + " entered the room]");
        return Protocol.OK + " Joined room " + roomName;
    }

    private String handleLeaveRoom() {
        if (currentToken == null) {
            return Protocol.UNAUTHORIZED + " Authentication required";
        }
        String roomName = sessionManager.getUserRoom(currentToken);
        if (roomName == null) {
            return Protocol.BAD_REQUEST + " Not currently in a room";
        }
        roomManager.removeUserFromRoom(roomName, currentUsername);
        sessionManager.setUserRoom(currentToken, null);
        sessionManager.broadcastToRoom(roomName, "[" + currentUsername + " left the room]");
        return Protocol.OK + " Left room " + roomName;
    }

    private String handleSend(String args) {
        if (currentToken == null) {
            return Protocol.UNAUTHORIZED + " Authentication required";
        }
        String roomName = sessionManager.getUserRoom(currentToken);
        if (roomName == null) {
            return Protocol.BAD_REQUEST + " Join a room first";
        }
        String content = args == null ? "" : args.trim();
        if (content.isEmpty()) {
            return Protocol.BAD_REQUEST + " Usage: SEND <message>";
        }
        Message message = roomManager.addMessage(roomName, currentUsername, content);
        if (message == null) {
            return Protocol.NOT_FOUND + " Room not found";
        }
        sessionManager.broadcastToRoom(roomName, currentUsername + ": " + content);
        return null;
    }

    private String handleHistory(String args) {
        String[] parts = args == null ? new String[0] : args.trim().split("\\s+", 2);
        if (parts.length == 0 || parts[0].isEmpty()) {
            return Protocol.BAD_REQUEST + " Usage: HISTORY <roomName> [count]";
        }
        String roomName = parts[0];
        int count = 0;
        if (parts.length > 1 && !parts[1].isBlank()) {
            try {
                count = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return Protocol.BAD_REQUEST + " Invalid count";
            }
        }
        List<Message> history = roomManager.getHistory(roomName, count);
        if (history.isEmpty() && !roomManager.roomExists(roomName)) {
            return Protocol.NOT_FOUND + " Room not found";
        }
        StringBuilder builder = new StringBuilder(Protocol.OK);
        for (Message message : history) {
            builder.append(" ").append(message.getSender()).append(":").append(message.getContent());
        }
        return builder.toString();
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
                String room = sessionManager.getUserRoom(currentToken);
                if (room != null) {
                    roomManager.removeUserFromRoom(room, currentUsername);
                    sessionManager.broadcastToRoom(room, "[" + currentUsername + " disconnected]");
                }
                sessionManager.unregisterSession(currentToken);
                sessionManager.unregisterDisconnectHandler(currentToken);
            }
            currentToken = null;
        }
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (IOException ignored) {

        }
    }
}
