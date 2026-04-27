package pt.up.fe.t06g10.client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ChatClient {
    private static final String OK = "200";
    private static final String UNAUTHORIZED = "401";

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: ChatClient <hostname> <port>");
            return;
        }

        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        Scanner scanner = new Scanner(System.in);
        String token = null;

        try (Socket socket = new Socket(hostname, port)) {
            socket.setSoTimeout(30000);
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (true) {
                System.out.println("\n=== Chat Menu ===");
                System.out.println("1. REGISTER");
                System.out.println("2. AUTH (login)");
                if (token != null) {
                    System.out.println("3. Use token to reconnect");
                    System.out.println("4. EXIT");
                } else {
                    System.out.println("3. EXIT");
                }
                System.out.print("Choose: ");

                String choice = scanner.nextLine().trim();

                if (token != null && choice.equals("3")) {
                    System.out.println("Reconnecting with token...");
                    writer.println("TOKEN " + token);
                    String response = reader.readLine();
                    if (response.startsWith(OK)) {
                        System.out.println("Reconnected as: " + response.substring(4));
                        continue;
                    } else {
                        System.out.println("Token expired, please login again");
                        token = null;
                        continue;
                    }
                }

                if (choice.equals("1") || choice.equalsIgnoreCase("REGISTER")) {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    System.out.print("Password: ");
                    String password = scanner.nextLine();
                    writer.println("REGISTER " + username + " " + password);
                    System.out.println("Server: " + reader.readLine());
                } 
                else if (choice.equals("2") || choice.equalsIgnoreCase("AUTH")) {
                    System.out.print("Username: ");
                    String username = scanner.nextLine().trim();
                    System.out.print("Password: ");
                    String password = scanner.nextLine();
                    writer.println("AUTH " + username + " " + password);
                    String response = reader.readLine();
                    System.out.println("Server: " + response);
                    if (response.startsWith(OK)) {
                        token = response.substring(4).trim();
                        System.out.println("Logged in! Token: " + token);
                    }
                } 
                else if (choice.equals("3") || choice.equals("4")) {
                    System.out.println("Goodbye!");
                    break;
                } 
                else {
                    System.out.println("Invalid choice");
                }
            }

        } catch (UnknownHostException ex) {
            System.out.println("Server not found: " + ex.getMessage());
        } catch (IOException ex) {
            System.out.println("I/O error: " + ex.getMessage());
        }
    }
}