package pt.up.fe.t06g10.shared.database;

import pt.up.fe.t06g10.shared.util.PasswordUtils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class UserDatabase {
    private final Path filePath;
    private final Map<String, UserRecord> users;

    public UserDatabase(String filename) throws IOException {
        this.filePath = Paths.get(filename);
        this.users = new HashMap<>();
        loadUsers();
    }

    private void loadUsers() throws IOException {
        if (Files.exists(filePath)) {
            try (BufferedReader reader = Files.newBufferedReader(filePath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] parts = line.split(":", 3);
                    if (parts.length == 3) {
                        users.put(parts[0], new UserRecord(parts[0], parts[1], parts[2]));
                    }
                }
            }
        }
    }

    private void saveUser(UserRecord record) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile(), true))) {
            writer.println(record.username + ":" + record.salt + ":" + record.hash);
        }
    }

    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    public void registerUser(String username, String password) throws IOException, UserExistsException {
        if (userExists(username)) {
            throw new UserExistsException(username);
        }
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash(password, salt);
        UserRecord record = new UserRecord(username, salt, hash);
        users.put(username, record);
        saveUser(record);
    }

    public boolean validateUser(String username, String password) {
        UserRecord record = users.get(username);
        if (record == null) return false;
        return PasswordUtils.verify(password, record.hash, record.salt);
    }

    private static class UserRecord {
        final String username;
        final String salt;
        final String hash;

        UserRecord(String username, String salt, String hash) {
            this.username = username;
            this.salt = salt;
            this.hash = hash;
        }
    }

    public static class UserExistsException extends Exception {
        public UserExistsException(String username) {
            super("User already exists: " + username);
        }
    }
}