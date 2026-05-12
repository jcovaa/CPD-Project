package pt.up.fe.t06g10.server;

import pt.up.fe.t06g10.server.auth.AuthService;
import pt.up.fe.t06g10.server.auth.TokenService;
import pt.up.fe.t06g10.server.room.RoomManager;
import pt.up.fe.t06g10.server.room.SessionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
0
/**
 * Simple TCP/IP socket server for the distributed chat system.
 */
public class ChatServer {

    private final int port;
    private final AuthService authService;
    private final TokenService tokenService;
    private final SessionManager sessionManager;
    private final RoomManager roomManager;

    public ChatServer(int port, AuthService authService, TokenService tokenService, SessionManager sessionManager, RoomManager roomManager) {
        this.port = port;
        this.authService = authService;
        this.tokenService = tokenService;
        this.sessionManager = sessionManager;
        this.roomManager = roomManager;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                Thread.ofVirtual().start(new ConnectionHandler(socket, authService, tokenService, sessionManager, roomManager));
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
