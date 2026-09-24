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
    public static final String GITHUB_TOKEN = "github.token";
    public static final String GITHUB_USER = "github.user";
    /** Remembered choice from the "Open Project" prompt: NEW_WINDOW or THIS_WINDOW. */
    public static final String OPEN_PROJECT_MODE = "openProject.mode";
    public static final String TERMINAL_SHELL_PATH = "terminal.shellPath";
    public static final String TERMINAL_FONT_SIZE = "terminal.fontSize";
    public static final String TERMINAL_CURSOR_SHAPE = "terminal.cursorShape";
    public static final String TERMINAL_TAB_NAME = "terminal.tabName";
    public static final String TERMINAL_START_DIR = "terminal.startDirectory";

    private static final Path FILE = Path.of(
            System.getProperty("user.home"), ".lumina", "lumina.properties");

    private static final Properties MEMORY_CACHE = new Properties();

    private Settings() {
    }

    public static String get(String key) {
        return load().getProperty(key);
    }

    public static void clear() {
        MEMORY_CACHE.clear();
        try {
            Files.deleteIfExists(FILE);
        } catch (Throwable ignored) {}
    }

    public static void put(String key, String value) {
        if (value == null || value.isBlank()) {
            MEMORY_CACHE.remove(key);
        } else {
            MEMORY_CACHE.setProperty(key, value);
        }
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
        } catch (Throwable ignored) {
            // Settings are best-effort; never break the IDE over them.
        }
    }

    private static Properties load() {
        Properties props = new Properties();
        props.putAll(MEMORY_CACHE);
        try {
            if (Files.isRegularFile(FILE)) {
                try (InputStream in = Files.newInputStream(FILE)) {
                    props.load(in);
                }
            }
        } catch (Throwable ignored) {
        }
        return props;
    }
}