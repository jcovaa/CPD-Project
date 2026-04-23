package pt.up.fe.t06g10.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleUI {

    private final BufferedReader console;

    public ConsoleUI() {
        this.console = new BufferedReader(new InputStreamReader(System.in));
    }

    public String readCommand() throws IOException {
        System.out.print("> ");
        System.out.flush();
        return console.readLine();
    }

    public void printLine(String line) {
        System.out.println(line);
    }

    public void printError(String message) {
        System.err.println("[Error] " + message);
    }
}
