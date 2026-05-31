package pt.up.fe.t06g10.client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Simple TCP/IP socket client for the distributed chat system.
 */

public class ChatClient {
    private final String hostname;
    private final int port;
    private final ConsoleUI ui;

    public ChatClient(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ChatClient <hostname> <port>");
            System.exit(1);
        }

        String host = args[0];
        int p = 0;

        try {
            p = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port: " + args[1]);
            System.exit(1);
        }

        this.hostname = host;
        this.port = p;
        this.ui = new ConsoleUI();
    }


    public void start() {
        SSLSocketFactory sslFact =
                (SSLSocketFactory) SSLSocketFactory.getDefault();

        int retries = 3;
        while (retries > 0) {
            try (SSLSocket socket = (SSLSocket) sslFact.createSocket(hostname, port)) {

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);

                Thread listener = Thread.ofVirtual().start(new ServerListener(reader, ui));

                ui.printPrompt();
                while (true) {
                    String line = ui.readCommand();
                    if (line == null) break;

                    line = line.trim();
                    if (line.isEmpty()) continue;

                    writer.println(line);
                    if (line.equalsIgnoreCase("QUIT")) break;
                }

                listener.join();
                return;
            } catch (IOException e) {
                retries--;
                if (retries > 0) {
                    ui.printError("Connection failed, " + retries + " retries left");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    ui.printError("Client error: " + e.getMessage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
