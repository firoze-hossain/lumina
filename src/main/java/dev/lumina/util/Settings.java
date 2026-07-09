package dev.lumina.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Tiny persistent settings store at ~/.lumina/lumina.properties. */
public final class Settings {

    public static final String LAST_PROJECT = "lastProject";
    public static final String DB_URL = "db.url";
    public static final String DB_USER = "db.user";

    private static final Path FILE = Path.of(
            System.getProperty("user.home"), ".lumina", "lumina.properties");

    private Settings() {
    }

    public static String get(String key) {
        return load().getProperty(key);
    }

    public static void put(String key, String value) {
        Properties props = load();
        if (value == null || value.isBlank()) {
            props.remove(key);
        } else {
            props.setProperty(key, value);
        }
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                props.store(out, "Lumina IDE settings");
            }
        } catch (IOException ignored) {
            // Settings are best-effort; never break the IDE over them.
        }
    }

    private static Properties load() {
        Properties props = new Properties();
        if (Files.isRegularFile(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                props.load(in);
            } catch (IOException ignored) {
            }
        }
        return props;
    }
}
