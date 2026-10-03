package dev.lumina.ui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.lumina.LuminaApp;
import dev.lumina.util.Settings;
import javafx.application.Platform;
import javafx.scene.Scene;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Dynamic IDE Theme Manager.
 * Supports built-in themes and dynamically loaded theme plugins (JAR / ZIP / directory)
 * installed from RozeHub Marketplace.
 */
public class ThemeManager {

    public static final String DEFAULT_THEME = "Dark";
    public static final String WHITE_THEME = "IntelliJ Clean White";

    private static final String SETTINGS_THEME_KEY = "ide.appearance.theme";
    private static final Path PLUGINS_DIR = Path.of(System.getProperty("user.home"), ".lumina", "plugins");

    private static ThemeManager instance;

    private final Map<String, String> themeStylesheets = new LinkedHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private String currentThemeName;

    private ThemeManager() {
        // Register built-in Dark theme
        String darkCss = getClass().getResource("/css/lumina-dark.css") != null
                ? getClass().getResource("/css/lumina-dark.css").toExternalForm()
                : null;
        if (darkCss != null) {
            themeStylesheets.put(DEFAULT_THEME, darkCss);
        }

        // Check if white theme is bundled or available in resources
        if (getClass().getResource("/css/lumina-light.css") != null) {
            themeStylesheets.put(WHITE_THEME, getClass().getResource("/css/lumina-light.css").toExternalForm());
        }

        // Scan installed plugin directories for dynamic theme plugins
        scanPluginThemes();

        // Restore saved theme or default to Dark
        String saved = Settings.get(SETTINGS_THEME_KEY);
        if (saved != null && themeStylesheets.containsKey(saved)) {
            currentThemeName = saved;
        } else {
            currentThemeName = DEFAULT_THEME;
        }
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public synchronized List<String> getAvailableThemeNames() {
        scanPluginThemes();
        return new ArrayList<>(themeStylesheets.keySet());
    }

    public synchronized String getCurrentThemeName() {
        return currentThemeName;
    }

    public synchronized String getCurrentThemeStylesheetUrl() {
        String url = themeStylesheets.get(currentThemeName);
        if (url == null && themeStylesheets.containsKey(DEFAULT_THEME)) {
            url = themeStylesheets.get(DEFAULT_THEME);
        }
        return url;
    }

    public synchronized void registerTheme(String name, String stylesheetUrl) {
        if (name == null || stylesheetUrl == null) return;
        themeStylesheets.put(name, stylesheetUrl);
        notifyListeners();
    }

    /**
     * Scans ~/.lumina/plugins/ for any theme plugins (JARs or extracted dirs with theme.css / plugin.json)
     */
    public synchronized void scanPluginThemes() {
        if (!Files.isDirectory(PLUGINS_DIR)) return;

        try (var stream = Files.list(PLUGINS_DIR)) {
            stream.forEach(path -> {
                try {
                    if (Files.isRegularFile(path) && path.toString().endsWith(".jar")) {
                        loadThemeFromJar(path);
                    } else if (Files.isDirectory(path)) {
                        loadThemeFromDir(path);
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    private void loadThemeFromJar(Path jarPath) {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry manifestEntry = jar.getJarEntry("plugin.json");
            String themeName = null;
            String cssEntryName = "theme.css";

            if (manifestEntry != null) {
                String json = new String(jar.getInputStream(manifestEntry).readAllBytes(), StandardCharsets.UTF_8);
                JsonObject obj = new Gson().fromJson(json, JsonObject.class);
                if (obj != null) {
                    if (obj.has("name")) themeName = obj.get("name").getAsString();
                    if (obj.has("themeCss")) cssEntryName = obj.get("themeCss").getAsString();
                }
            }

            JarEntry cssEntry = jar.getJarEntry(cssEntryName);
            if (cssEntry != null) {
                if (themeName == null || themeName.isBlank()) {
                    themeName = jarPath.getFileName().toString().replace(".jar", "");
                }
                // Extract to local temp or cache so JavaFX can read it reliably
                Path extractedDir = Path.of(System.getProperty("user.home"), ".lumina", "themes");
                Files.createDirectories(extractedDir);
                Path targetCss = extractedDir.resolve(jarPath.getFileName().toString().replace(".jar", "") + ".css");
                try (InputStream in = jar.getInputStream(cssEntry)) {
                    Files.copy(in, targetCss, StandardCopyOption.REPLACE_EXISTING);
                }
                themeStylesheets.put(themeName, targetCss.toUri().toURL().toExternalForm());
            }
        } catch (Exception ignored) {}
    }

    private void loadThemeFromDir(Path dir) {
        Path manifestPath = dir.resolve("plugin.json");
        String themeName = null;
        Path cssPath = dir.resolve("theme.css");

        if (Files.isRegularFile(manifestPath)) {
            try {
                String json = Files.readString(manifestPath, StandardCharsets.UTF_8);
                JsonObject obj = new Gson().fromJson(json, JsonObject.class);
                if (obj != null) {
                    if (obj.has("name")) themeName = obj.get("name").getAsString();
                    if (obj.has("themeCss")) cssPath = dir.resolve(obj.get("themeCss").getAsString());
                }
            } catch (Exception ignored) {}
        }

        if (Files.isRegularFile(cssPath)) {
            if (themeName == null || themeName.isBlank()) {
                themeName = dir.getFileName().toString();
            }
            try {
                themeStylesheets.put(themeName, cssPath.toUri().toURL().toExternalForm());
            } catch (Exception ignored) {}
        }
    }

    /**
     * Applies a theme by name across all active LuminaApp instances and remembers it.
     */
    public synchronized void applyTheme(String themeName) {
        if (themeName == null || !themeStylesheets.containsKey(themeName)) return;

        this.currentThemeName = themeName;
        Settings.put(SETTINGS_THEME_KEY, themeName);

        String cssUrl = themeStylesheets.get(themeName);
        if (cssUrl == null) return;

        Platform.runLater(() -> {
            for (LuminaApp app : LuminaApp.ACTIVE_INSTANCES) {
                if (app != null && app.getStage() != null && app.getStage().getScene() != null) {
                    applyToScene(app.getStage().getScene(), cssUrl);
                }
            }
            notifyListeners();
        });
    }

    /**
     * Applies the current theme stylesheet to a newly created Scene.
     */
    public void applyCurrentTheme(Scene scene) {
        if (scene == null) return;
        String cssUrl = getCurrentThemeStylesheetUrl();
        if (cssUrl != null) {
            applyToScene(scene, cssUrl);
        }
    }

    private void applyToScene(Scene scene, String cssUrl) {
        if (scene == null || cssUrl == null) return;
        scene.getStylesheets().clear();
        scene.getStylesheets().add(cssUrl);
    }

    public void addListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        if (listener != null) listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable r : listeners) {
            try {
                r.run();
            } catch (Exception ignored) {}
        }
    }
}
