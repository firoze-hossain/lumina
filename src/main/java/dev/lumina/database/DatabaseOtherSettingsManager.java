package dev.lumina.database;

import dev.lumina.util.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Tools > Database > Other settings in Lumina IDE.
 */
public class DatabaseOtherSettingsManager {

    private static final String KEY_PREFIX = "database.other.";
    private static final String KEY_CONFIRM_CANCEL = KEY_PREFIX + "confirm_cancellation_modify_schema";
    private static final String KEY_PREVIEW_SCRIPT = KEY_PREFIX + "show_preview_valid_script";
    private static final String KEY_SUGGEST_DDL = KEY_PREFIX + "suggest_dumping_ddl";
    private static final String KEY_GENERATE_TEMPLATES = KEY_PREFIX + "generate_context_templates";
    private static final String KEY_REMEMBER_FILTER = KEY_PREFIX + "remember_filter_on";
    private static final String KEY_VFK_COUNT = KEY_PREFIX + "vfk_count";
    private static final String KEY_VFK_PREFIX = KEY_PREFIX + "vfk_";
    private static final String KEY_DEFAULT_RESOLVE = KEY_PREFIX + "default_resolve_mode";
    private static final String KEY_STATEMENT_DELIM = KEY_PREFIX + "statement_delimiter";

    private static volatile DatabaseOtherSettingsManager instance;

    private DatabaseOtherSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseOtherSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseOtherSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseOtherSettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseOtherSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseOtherSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseOtherSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseOtherSettings();
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

    private DatabaseOtherSettings loadSettings() {
        DatabaseOtherSettings settings = new DatabaseOtherSettings();

        String confirmCancel = Settings.get(KEY_CONFIRM_CANCEL);
        if (confirmCancel != null) {
            settings.setConfirmCancellationForDialogsModifySchema(Boolean.parseBoolean(confirmCancel));
        }

        String previewScript = Settings.get(KEY_PREVIEW_SCRIPT);
        if (previewScript != null) {
            settings.setShowPreviewOfValidScript(Boolean.parseBoolean(previewScript));
        }

        String suggestDdl = Settings.get(KEY_SUGGEST_DDL);
        if (suggestDdl != null) {
            settings.setSuggestDumpingDdl(Boolean.parseBoolean(suggestDdl));
        }

        String templates = Settings.get(KEY_GENERATE_TEMPLATES);
        if (templates != null) {
            settings.setGenerateContextTemplates(templates);
        }

        String rememberFilter = Settings.get(KEY_REMEMBER_FILTER);
        if (rememberFilter != null) {
            settings.setRememberWhetherFilterIsOn(Boolean.parseBoolean(rememberFilter));
        }

        String vfkCountStr = Settings.get(KEY_VFK_COUNT);
        if (vfkCountStr != null) {
            try {
                int count = Integer.parseInt(vfkCountStr);
                List<DatabaseVirtualForeignKey> loaded = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String col = Settings.get(KEY_VFK_PREFIX + i + "_col");
                    String target = Settings.get(KEY_VFK_PREFIX + i + "_target");
                    if (col != null && target != null) {
                        loaded.add(new DatabaseVirtualForeignKey(col, target));
                    }
                }
                if (!loaded.isEmpty()) {
                    settings.setVirtualForeignKeys(loaded);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        String resolveMode = Settings.get(KEY_DEFAULT_RESOLVE);
        if (resolveMode != null) {
            settings.setDefaultResolveModeForConsoles(resolveMode);
        }

        String delim = Settings.get(KEY_STATEMENT_DELIM);
        if (delim != null) {
            settings.setStatementDelimiter(delim);
        }

        return settings;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_CONFIRM_CANCEL, String.valueOf(currentSettings.isConfirmCancellationForDialogsModifySchema()));
        Settings.put(KEY_PREVIEW_SCRIPT, String.valueOf(currentSettings.isShowPreviewOfValidScript()));
        Settings.put(KEY_SUGGEST_DDL, String.valueOf(currentSettings.isSuggestDumpingDdl()));
        Settings.put(KEY_GENERATE_TEMPLATES, currentSettings.getGenerateContextTemplates());
        Settings.put(KEY_REMEMBER_FILTER, String.valueOf(currentSettings.isRememberWhetherFilterIsOn()));

        List<DatabaseVirtualForeignKey> keys = currentSettings.getVirtualForeignKeys();
        Settings.put(KEY_VFK_COUNT, String.valueOf(keys.size()));
        for (int i = 0; i < keys.size(); i++) {
            DatabaseVirtualForeignKey k = keys.get(i);
            Settings.put(KEY_VFK_PREFIX + i + "_col", k.getColumnPattern());
            Settings.put(KEY_VFK_PREFIX + i + "_target", k.getTargetColumnPattern());
        }

        Settings.put(KEY_DEFAULT_RESOLVE, currentSettings.getDefaultResolveModeForConsoles());
        Settings.put(KEY_STATEMENT_DELIM, currentSettings.getStatementDelimiter());
    }
}
