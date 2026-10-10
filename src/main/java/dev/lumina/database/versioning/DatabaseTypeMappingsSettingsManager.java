package dev.lumina.database.versioning;

import dev.lumina.util.Settings;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Database Versioning Type Mappings in Lumina IDE.
 */
public class DatabaseTypeMappingsSettingsManager {

    private static final String KEY_PREFIX = "database.versioning.typemappings.";
    private static final String KEY_SELECTED = KEY_PREFIX + "selected_db";
    private static final String KEY_DB_PREFIX = KEY_PREFIX + "db.";

    private static volatile DatabaseTypeMappingsSettingsManager instance;

    private DatabaseTypeMappingsSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseTypeMappingsSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseTypeMappingsSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseTypeMappingsSettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseTypeMappingsSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseTypeMappingsSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseTypeMappingsSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseTypeMappingsSettings();
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

    private DatabaseTypeMappingsSettings loadSettings() {
        DatabaseTypeMappingsSettings s = new DatabaseTypeMappingsSettings();

        String sel = Settings.get(KEY_SELECTED);
        if (sel != null) s.setSelectedDatabase(sel);

        for (String db : DatabaseTypeMappingsSettings.SUPPORTED_DATABASES) {
            String countStr = Settings.get(KEY_DB_PREFIX + db + ".count");
            if (countStr != null) {
                try {
                    int count = Integer.parseInt(countStr);
                    List<DatabaseTypeMappingItem> list = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) {
                        String attr = Settings.get(KEY_DB_PREFIX + db + "." + i + ".attr");
                        String target = Settings.get(KEY_DB_PREFIX + db + "." + i + ".target");
                        String params = Settings.get(KEY_DB_PREFIX + db + "." + i + ".params");
                        if (attr != null || target != null) {
                            list.add(new DatabaseTypeMappingItem(attr, target, params));
                        }
                    }
                    s.getDatabaseMappings().put(db, list);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_SELECTED, currentSettings.getSelectedDatabase());

        for (String db : DatabaseTypeMappingsSettings.SUPPORTED_DATABASES) {
            List<DatabaseTypeMappingItem> list = currentSettings.getMappingsForDatabase(db);
            Settings.put(KEY_DB_PREFIX + db + ".count", String.valueOf(list.size()));
            for (int i = 0; i < list.size(); i++) {
                DatabaseTypeMappingItem item = list.get(i);
                Settings.put(KEY_DB_PREFIX + db + "." + i + ".attr", item.getAttributeType());
                Settings.put(KEY_DB_PREFIX + db + "." + i + ".target", item.getTargetType());
                Settings.put(KEY_DB_PREFIX + db + "." + i + ".params", item.getTypeParameters());
            }
        }
    }
}
