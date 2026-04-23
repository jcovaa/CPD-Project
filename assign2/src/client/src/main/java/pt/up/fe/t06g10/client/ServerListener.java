package pt.up.fe.t06g10.client;

import java.io.BufferedReader;
import java.io.IOException;

public class ServerListener implements Runnable {
    private final BufferedReader reader;
    private final ConsoleUI ui;

    public ServerListener(BufferedReader reader, ConsoleUI ui) {
        this.reader = reader;
        this.ui = ui;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                ui.printLine(line);
            }
        } catch (IOException ex) {
            ui.printError("Server listener error: " + ex.getMessage());
        }
    }
}
