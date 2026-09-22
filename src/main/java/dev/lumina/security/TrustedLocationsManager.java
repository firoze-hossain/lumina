package dev.lumina.security;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * Central singleton managing trusted local directories for projects.
 * Strictly matches media_1790046849995.png.
 */
public class TrustedLocationsManager {

    private static final TrustedLocationsManager INSTANCE = new TrustedLocationsManager();

    private final Preferences prefs = Preferences.userNodeForPackage(TrustedLocationsManager.class);
    private final List<String> locations = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private TrustedLocationsManager() {
        loadPreferences();
        if (locations.isEmpty()) {
            initDefaults();
        }
    }

    public static TrustedLocationsManager getInstance() {
        return INSTANCE;
    }

    private void initDefaults() {
        locations.clear();
        String home = System.getProperty("user.home");
        // Initial defaults matching user environment / media_1790046849995.png
        String devProjects = home + File.separator + "Development" + File.separator + "projects" + File.separator + "others";
        String downloads = home + File.separator + "Downloads";
        String erp = home + File.separator + "Development" + File.separator + "ERPApplicationServer";
        String docs = home + File.separator + "Documents";

        addLocationInternal(devProjects);
        addLocationInternal(downloads);
        addLocationInternal(erp);
        addLocationInternal(docs);
    }

    private void addLocationInternal(String path) {
        if (path == null || path.isBlank()) return;
        String normalized = normalizePath(path);
        if (!locations.contains(normalized)) {
            locations.add(normalized);
        }
    }

    public static String normalizePath(String path) {
        if (path == null) return "";
        return path.trim().replace('\\', '/');
    }

    public synchronized List<String> getLocations() {
        return new ArrayList<>(locations);
    }

    public synchronized void addLocation(String path) {
        if (path == null || path.isBlank()) return;
        String normalized = normalizePath(path);
        if (!locations.contains(normalized)) {
            locations.add(normalized);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void removeLocation(int index) {
        if (index >= 0 && index < locations.size()) {
            locations.remove(index);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void removeLocation(String path) {
        if (path == null) return;
        String normalized = normalizePath(path);
        if (locations.remove(normalized)) {
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void moveUp(int index) {
        if (index > 0 && index < locations.size()) {
            String item = locations.remove(index);
            locations.add(index - 1, item);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized void moveDown(int index) {
        if (index >= 0 && index < locations.size() - 1) {
            String item = locations.remove(index);
            locations.add(index + 1, item);
            savePreferences();
            notifyListeners();
        }
    }

    public synchronized boolean isTrusted(Path path) {
        if (path == null) return false;
        String target = normalizePath(path.toAbsolutePath().toString());
        for (String trusted : locations) {
            String cleanTrusted = normalizePath(trusted);
            if (target.equalsIgnoreCase(cleanTrusted) || target.startsWith(cleanTrusted.endsWith("/") ? cleanTrusted : cleanTrusted + "/")) {
                return true;
            }
        }
        return false;
    }

    public synchronized void revertToDefaults() {
        initDefaults();
        savePreferences();
        notifyListeners();
    }

    public synchronized void clear() {
        locations.clear();
        savePreferences();
        notifyListeners();
    }

    public synchronized void addListener(Runnable listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable l : new ArrayList<>(listeners)) {
            try {
                l.run();
            } catch (Exception ignored) {}
        }
    }

    private void loadPreferences() {
        locations.clear();
        String data = prefs.get("trusted_locations_data", "");
        if (!data.isBlank()) {
            String[] lines = data.split("\n");
            for (String line : lines) {
                if (!line.isBlank()) {
                    addLocationInternal(line);
                }
            }
        }
    }

    private void savePreferences() {
        StringBuilder sb = new StringBuilder();
        for (String loc : locations) {
            sb.append(loc).append("\n");
        }
        prefs.put("trusted_locations_data", sb.toString());
    }
}
