package pt.up.fe.t06g10.server.util;

import io.github.cdimascio.dotenv.Dotenv;

public final class EnvUtils {
    private static final Dotenv DOTENV = Dotenv.load();

    private EnvUtils() {
    }

    public static String getRequiredEnv(String name) {
        String value = DOTENV.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return value;
    }
}
