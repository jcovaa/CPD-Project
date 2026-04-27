package pt.up.fe.t06g10.client;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: Main <hostname> <port>");
            System.exit(1);
        }
        ChatClient.main(args);
    }
}
