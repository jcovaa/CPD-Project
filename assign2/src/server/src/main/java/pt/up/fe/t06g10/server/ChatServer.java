package pt.up.fe.t06g10.server;

import pt.up.fe.t06g10.server.auth.AuthService;
import pt.up.fe.t06g10.server.auth.TokenService;
import pt.up.fe.t06g10.server.connection.ClientWriter;
import pt.up.fe.t06g10.server.room.RoomManager;
import pt.up.fe.t06g10.server.room.SessionManager;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;

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
        SSLServerSocketFactory sslSrvFact =
                (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        try (SSLServerSocket serverSocket = (SSLServerSocket) sslSrvFact.createServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                SSLSocket socket = (SSLSocket) serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                try {
                    ClientWriter clientWriter = new ClientWriter(socket);
                    Thread.ofVirtual().start(new ConnectionHandler(socket, authService, tokenService, sessionManager, roomManager, clientWriter));
                } catch (IOException ex) {
                    System.out.println("Failed to initialize client writer: " + ex.getMessage());
                    try {
                        socket.close();
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
