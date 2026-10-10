package dev.lumina.database.versioning;

import dev.lumina.util.Settings;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Liquibase settings in Lumina IDE.
 */
public class DatabaseLiquibaseSettingsManager {

    private static final String KEY_PREFIX = "database.versioning.liquibase.";
    private static final String KEY_VER = KEY_PREFIX + "version";
    private static final String KEY_AUTHOR = KEY_PREFIX + "author";
    private static final String KEY_FILE_TYPE = KEY_PREFIX + "file_type";
    private static final String KEY_ROLLBACK = KEY_PREFIX + "empty_rollback";
    private static final String KEY_PRI_DIR = KEY_PREFIX + "primary_dir";
    private static final String KEY_PRI_NAME = KEY_PREFIX + "primary_name";
    private static final String KEY_SEC_DIR = KEY_PREFIX + "secondary_dir";
    private static final String KEY_SEC_NAME = KEY_PREFIX + "secondary_name";
    private static final String KEY_DB_TYPES = KEY_PREFIX + "enabled_db_types";
    private static final String KEY_TPL_COUNT = KEY_PREFIX + "templates.count";
    private static final String KEY_TPL_PREFIX = KEY_PREFIX + "template.";

    private static volatile DatabaseLiquibaseSettingsManager instance;

    private DatabaseLiquibaseSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseLiquibaseSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseLiquibaseSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseLiquibaseSettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseLiquibaseSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseLiquibaseSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseLiquibaseSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseLiquibaseSettings();
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

    private DatabaseLiquibaseSettings loadSettings() {
        DatabaseLiquibaseSettings s = new DatabaseLiquibaseSettings();

        String ver = Settings.get(KEY_VER);
        if (ver != null) s.setLiquibaseVersion(ver);

        String author = Settings.get(KEY_AUTHOR);
        if (author != null) s.setChangesetAuthor(author);

        String fType = Settings.get(KEY_FILE_TYPE);
        if (fType != null) s.setFileType(fType);

        String rb = Settings.get(KEY_ROLLBACK);
        if (rb != null) s.setAddEmptyRollback(Boolean.parseBoolean(rb));

        String priDir = Settings.get(KEY_PRI_DIR);
        if (priDir != null) s.setPrimaryDirectory(priDir);

        String priName = Settings.get(KEY_PRI_NAME);
        if (priName != null) s.setPrimaryName(priName);

        String secDir = Settings.get(KEY_SEC_DIR);
        if (secDir != null) s.setSecondaryDirectory(secDir);

        String secName = Settings.get(KEY_SEC_NAME);
        if (secName != null) s.setSecondaryName(secName);

        String dbTypes = Settings.get(KEY_DB_TYPES);
        if (dbTypes != null && !dbTypes.isBlank()) {
            Set<String> set = new HashSet<>();
            for (String t : dbTypes.split(",")) {
                String trimmed = t.trim();
                if (!trimmed.isEmpty()) set.add(trimmed);
            }
            s.setEnabledDbTypes(set);
        }

        String countStr = Settings.get(KEY_TPL_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<LiquibaseChangesetTemplateItem> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    String name = Settings.get(KEY_TPL_PREFIX + i + ".name");
                    boolean fail = Boolean.parseBoolean(Settings.get(KEY_TPL_PREFIX + i + ".fail"));
                    boolean run = Boolean.parseBoolean(Settings.get(KEY_TPL_PREFIX + i + ".run"));
                    boolean pre = Boolean.parseBoolean(Settings.get(KEY_TPL_PREFIX + i + ".pre"));
                    if (name != null) {
                        list.add(new LiquibaseChangesetTemplateItem(name, fail, run, pre));
                    }
                }
                if (!list.isEmpty()) {
                    s.setChangesetTemplates(list);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_VER, currentSettings.getLiquibaseVersion());
        Settings.put(KEY_AUTHOR, currentSettings.getChangesetAuthor());
        Settings.put(KEY_FILE_TYPE, currentSettings.getFileType());
        Settings.put(KEY_ROLLBACK, String.valueOf(currentSettings.isAddEmptyRollback()));
        Settings.put(KEY_PRI_DIR, currentSettings.getPrimaryDirectory());
        Settings.put(KEY_PRI_NAME, currentSettings.getPrimaryName());
        Settings.put(KEY_SEC_DIR, currentSettings.getSecondaryDirectory());
        Settings.put(KEY_SEC_NAME, currentSettings.getSecondaryName());

        if (currentSettings.getEnabledDbTypes() != null) {
            Settings.put(KEY_DB_TYPES, String.join(",", currentSettings.getEnabledDbTypes()));
        }

        List<LiquibaseChangesetTemplateItem> templates = currentSettings.getChangesetTemplates();
        Settings.put(KEY_TPL_COUNT, String.valueOf(templates.size()));
        for (int i = 0; i < templates.size(); i++) {
            LiquibaseChangesetTemplateItem item = templates.get(i);
            Settings.put(KEY_TPL_PREFIX + i + ".name", item.getName());
            Settings.put(KEY_TPL_PREFIX + i + ".fail", String.valueOf(item.isFailOnError()));
            Settings.put(KEY_TPL_PREFIX + i + ".run", String.valueOf(item.isRunOnChange()));
            Settings.put(KEY_TPL_PREFIX + i + ".pre", String.valueOf(item.isCreatePreconditions()));
        }
    }
}
