package dev.lumina.database.versioning;

import dev.lumina.util.Settings;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Flyway settings in Lumina IDE.
 */
public class DatabaseFlywaySettingsManager {

    private static final String KEY_PREFIX = "database.versioning.flyway.";
    private static final String KEY_PREFIX_FIELD = KEY_PREFIX + "migration_prefix";
    private static final String KEY_VERSION_PAT = KEY_PREFIX + "version_pattern";
    private static final String KEY_SEPARATOR = KEY_PREFIX + "migration_separator";
    private static final String KEY_DESC = KEY_PREFIX + "migration_description";
    private static final String KEY_NO_DEP = KEY_PREFIX + "without_dependency";

    private static volatile DatabaseFlywaySettingsManager instance;

    private DatabaseFlywaySettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseFlywaySettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseFlywaySettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseFlywaySettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseFlywaySettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseFlywaySettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseFlywaySettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseFlywaySettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private DatabaseFlywaySettings loadSettings() {
        DatabaseFlywaySettings s = new DatabaseFlywaySettings();

        String pfx = Settings.get(KEY_PREFIX_FIELD);
        if (pfx != null) s.setMigrationPrefix(pfx);

        String ver = Settings.get(KEY_VERSION_PAT);
        if (ver != null) s.setVersionPattern(ver);

        String sep = Settings.get(KEY_SEPARATOR);
        if (sep != null) s.setMigrationSeparator(sep);

        String desc = Settings.get(KEY_DESC);
        if (desc != null) s.setMigrationDescription(desc);

        String noDep = Settings.get(KEY_NO_DEP);
        if (noDep != null) s.setUseFlywayWithoutDependency(Boolean.parseBoolean(noDep));

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_PREFIX_FIELD, currentSettings.getMigrationPrefix());
        Settings.put(KEY_VERSION_PAT, currentSettings.getVersionPattern());
        Settings.put(KEY_SEPARATOR, currentSettings.getMigrationSeparator());
        Settings.put(KEY_DESC, currentSettings.getMigrationDescription());
        Settings.put(KEY_NO_DEP, String.valueOf(currentSettings.isUseFlywayWithoutDependency()));
    }
}
