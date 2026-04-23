package pt.up.fe.t06g10.server;

import pt.up.fe.t06g10.shared.Protocol;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Simple TCP/IP socket server for the distributed chat system.
 */
public class ChatServer {

    public static void main(String[] args) {
        if (args.length < 1) return;

        int port = Integer.parseInt(args[0]);

        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                Thread.ofVirtual().start(() -> handleClient(socket));
            }

        } catch (IOException ex) {
            System.out.println("Server exception: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static void handleClient(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream())
             );
             PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream()),
                     true
             )
        ) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Client: " + line);

                    if (Protocol.isValidClientCommand(line)) {
                        writer.println("OK " + line);
                    } else {
                        writer.println(Protocol.BAD_REQUEST + " Invalid command: " + line);
                    }
                }
            } catch (IOException ex) {
                System.out.println("Client error: " + ex.getMessage());
            }
        } catch (IOException ignored) {
        }
    }
}
