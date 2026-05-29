package pt.up.fe.t06g10.server.database;

import pt.up.fe.t06g10.server.util.EnvUtils;

public final class DatabaseConfig {
    private DatabaseConfig() {
    }

    public static String getJdbcUrl() {
        return EnvUtils.getRequiredEnv("DB_URL");
    }

    public static String getUsername() {
        return EnvUtils.getRequiredEnv("DB_USER");
    }

    public static String getPassword() {
        return EnvUtils.getRequiredEnv("DB_PASSWORD");
    }
}
