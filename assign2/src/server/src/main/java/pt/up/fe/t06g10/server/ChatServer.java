package pt.up.fe.t06g10.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Simple TCP/IP socket server for the distributed chat system.
 */
public class ChatServer {

    private final int port;

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                Thread.ofVirtual().start(new ConnectionHandler(socket));
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
