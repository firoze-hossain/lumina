package dev.lumina.database.versioning;

import dev.lumina.util.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Database Diff Changes settings in Lumina IDE.
 */
public class DatabaseDiffChangesSettingsManager {

    private static final String KEY_PREFIX = "database.versioning.diff.";
    private static final String KEY_EXCLUDED_COUNT = KEY_PREFIX + "excluded_count";
    private static final String KEY_EXCLUDED_PREFIX = KEY_PREFIX + "excluded_";

    private static volatile DatabaseDiffChangesSettingsManager instance;

    private DatabaseDiffChangesSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseDiffChangesSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseDiffChangesSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseDiffChangesSettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseDiffChangesSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseDiffChangesSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseDiffChangesSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseDiffChangesSettings();
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

    private DatabaseDiffChangesSettings loadSettings() {
        DatabaseDiffChangesSettings settings = new DatabaseDiffChangesSettings();

        // Load modified rule properties if customized
        for (DiffChangeRule r : settings.getRules()) {
            String ruleKey = KEY_PREFIX + "rule." + r.getCategory() + "." + r.getAction().replace(" ", "_");
            String loc = Settings.get(ruleKey + ".location");
            if (loc != null) r.setLocation(loc);

            String danger = Settings.get(ruleKey + ".danger");
            if (danger != null) r.setDangerLevel(danger);

            String ctx = Settings.get(ruleKey + ".context");
            if (ctx != null) r.setContext(ctx);

            String labels = Settings.get(ruleKey + ".labels");
            if (labels != null) r.setLabels(labels);
        }

        String countStr = Settings.get(KEY_EXCLUDED_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<DiffChangeExcludedItem> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String tag = Settings.get(KEY_EXCLUDED_PREFIX + i + "_tag");
                    String target = Settings.get(KEY_EXCLUDED_PREFIX + i + "_target");
                    if (tag != null && target != null) {
                        list.add(new DiffChangeExcludedItem(tag, target));
                    }
                }
                settings.setExcludedChanges(list);
            } catch (NumberFormatException ignored) {
            }
        }

        return settings;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        for (DiffChangeRule r : currentSettings.getRules()) {
            String ruleKey = KEY_PREFIX + "rule." + r.getCategory() + "." + r.getAction().replace(" ", "_");
            Settings.put(ruleKey + ".location", r.getLocation());
            Settings.put(ruleKey + ".danger", r.getDangerLevel());
            Settings.put(ruleKey + ".context", r.getContext());
            Settings.put(ruleKey + ".labels", r.getLabels());
        }

        List<DiffChangeExcludedItem> list = currentSettings.getExcludedChanges();
        Settings.put(KEY_EXCLUDED_COUNT, String.valueOf(list.size()));
        for (int i = 0; i < list.size(); i++) {
            DiffChangeExcludedItem item = list.get(i);
            Settings.put(KEY_EXCLUDED_PREFIX + i + "_tag", item.getTagName());
            Settings.put(KEY_EXCLUDED_PREFIX + i + "_target", item.getTargetName());
        }
    }
}
