package pt.up.fe.t06g10.server.database;

import io.github.cdimascio.dotenv.Dotenv;

public final class DatabaseConfig {
    private static final Dotenv DOTENV = Dotenv.load();

    private DatabaseConfig() {
    }

    public static String getJdbcUrl() {
        return getRequiredEnv("DB_URL");
    }

    public static String getUsername() {
        return getRequiredEnv("DB_USER");
    }

    public static String getPassword() {
        return getRequiredEnv("DB_PASSWORD");
    }

    private static String getRequiredEnv(String name) {
        String value = DOTENV.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
