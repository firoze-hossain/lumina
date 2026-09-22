package dev.lumina.git;

import dev.lumina.util.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages VCS Shelf settings matching IntelliJ IDEA's "Version Control > Shelf".
 * Dynamically resolves project shelf directory and persists custom path in Java Preferences.
 */
public class VcsShelfSettingsManager {

    private static final VcsShelfSettingsManager INSTANCE = new VcsShelfSettingsManager();

    private final Preferences prefs = Preferences.userNodeForPackage(VcsShelfSettingsManager.class);
    private final List<Runnable> listeners = new ArrayList<>();

    private boolean removeAppliedFiles = false;
    private boolean shelveBaseRevisions = true;
    private String customShelvesLocation = "";

    private Path currentProjectPath;

    private VcsShelfSettingsManager() {
        loadPreferences();
    }

    public static VcsShelfSettingsManager getInstance() {
        return INSTANCE;
    }

    public synchronized void setCurrentProjectPath(Path projectPath) {
        this.currentProjectPath = projectPath;
        notifyListeners();
    }

    public synchronized Path getCurrentProjectPath() {
        if (currentProjectPath != null) {
            return currentProjectPath;
        }
        String last = Settings.get(Settings.LAST_PROJECT);
        if (last != null && !last.isBlank()) {
            try {
                Path p = Path.of(last);
                if (Files.isDirectory(p)) {
                    currentProjectPath = p;
                    return currentProjectPath;
                }
            } catch (Exception ignored) {}
        }
        try {
            return Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        } catch (Exception e) {
            return Path.of(System.getProperty("user.home"), ".lumina", "shelf");
        }
    }

    public synchronized Path getDefaultShelvesLocation() {
        Path root = getCurrentProjectPath();
        if (root != null) {
            return root.resolve(".idea").resolve("shelf").toAbsolutePath().normalize();
        }
        return Path.of(System.getProperty("user.home"), ".lumina", "shelf");
    }

    public synchronized Path getCurrentShelvesLocation() {
        if (customShelvesLocation != null && !customShelvesLocation.isBlank()) {
            try {
                return Path.of(customShelvesLocation).toAbsolutePath().normalize();
            } catch (Exception ignored) {}
        }
        return getDefaultShelvesLocation();
    }

    public synchronized boolean isCustomLocationConfigured() {
        return customShelvesLocation != null && !customShelvesLocation.isBlank();
    }

    public synchronized String getCustomShelvesLocation() {
        return customShelvesLocation != null ? customShelvesLocation : "";
    }

    public synchronized boolean isRemoveAppliedFiles() {
        return removeAppliedFiles;
    }

    public synchronized void setRemoveAppliedFiles(boolean val) {
        if (this.removeAppliedFiles != val) {
            this.removeAppliedFiles = val;
            prefs.putBoolean("shelf_remove_applied_files", val);
            notifyListeners();
        }
    }

    public synchronized boolean isShelveBaseRevisions() {
        return shelveBaseRevisions;
    }

    public synchronized void setShelveBaseRevisions(boolean val) {
        if (this.shelveBaseRevisions != val) {
            this.shelveBaseRevisions = val;
            prefs.putBoolean("shelf_shelve_base_revisions", val);
            notifyListeners();
        }
    }

    /**
     * Updates the shelves location. If customLocation is null or equal to default, resets to default.
     * Optionally moves existing shelves files from the current location to the target location.
     */
    public synchronized boolean setShelvesLocation(String newCustomLocation, boolean moveExistingFiles) {
        Path oldLocation = getCurrentShelvesLocation();
        Path targetLocation;

        String targetStr = (newCustomLocation != null) ? newCustomLocation.trim() : "";
        Path defaultLoc = getDefaultShelvesLocation();

        if (targetStr.isBlank() || (targetStr.equals(defaultLoc.toString()))) {
            customShelvesLocation = "";
            targetLocation = defaultLoc;
        } else {
            customShelvesLocation = targetStr;
            targetLocation = Path.of(targetStr).toAbsolutePath().normalize();
        }

        prefs.put("shelf_custom_location", customShelvesLocation);

        if (moveExistingFiles && !oldLocation.equals(targetLocation) && Files.isDirectory(oldLocation)) {
            try {
                Files.createDirectories(targetLocation);
                try (var stream = Files.walk(oldLocation)) {
                    for (Path source : stream.toList()) {
                        if (!source.equals(oldLocation)) {
                            Path rel = oldLocation.relativize(source);
                            Path dest = targetLocation.resolve(rel);
                            if (Files.isDirectory(source)) {
                                Files.createDirectories(dest);
                            } else {
                                Files.createDirectories(dest.getParent());
                                Files.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            } catch (IOException ignored) {}
        }

        notifyListeners();
        return true;
    }

    public synchronized void revertToDefaults() {
        removeAppliedFiles = false;
        shelveBaseRevisions = true;
        customShelvesLocation = "";
        prefs.remove("shelf_remove_applied_files");
        prefs.remove("shelf_shelve_base_revisions");
        prefs.remove("shelf_custom_location");
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
        removeAppliedFiles = prefs.getBoolean("shelf_remove_applied_files", false);
        shelveBaseRevisions = prefs.getBoolean("shelf_shelve_base_revisions", true);
        customShelvesLocation = prefs.get("shelf_custom_location", "");
    }
}
