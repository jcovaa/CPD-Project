package pt.up.fe.t06g10.server;

import pt.up.fe.t06g10.shared.Protocol;

import java.io.*;
import java.net.Socket;

public class ConnectionHandler implements Runnable {

    private final Socket socket;

    public ConnectionHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())
                );
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream()),
                        true
                )
        ) {
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
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }
}
