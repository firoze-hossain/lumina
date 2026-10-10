package dev.lumina.database;

import dev.lumina.util.Settings;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Database User Parameters settings in Lumina IDE.
 */
public class DatabaseUserParametersSettingsManager {

    private static final String KEY_PREFIX = "database.user_parameters.";
    private static final String KEY_ENABLE_CONSOLES = KEY_PREFIX + "enable_in_consoles";
    private static final String KEY_ENABLE_LITERALS = KEY_PREFIX + "enable_in_literals";
    private static final String KEY_SUBSTITUTE_STRINGS = KEY_PREFIX + "substitute_inside_strings";
    private static final String KEY_PATTERN_COUNT = KEY_PREFIX + "pattern_count";
    private static final String KEY_PATTERN_PREFIX = KEY_PREFIX + "pattern_";

    private static volatile DatabaseUserParametersSettingsManager instance;

    private DatabaseUserParametersSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseUserParametersSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseUserParametersSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseUserParametersSettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseUserParametersSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseUserParametersSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseUserParametersSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseUserParametersSettings();
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

    private DatabaseUserParametersSettings loadSettings() {
        DatabaseUserParametersSettings settings = new DatabaseUserParametersSettings();

        String enableConsoles = Settings.get(KEY_ENABLE_CONSOLES);
        if (enableConsoles != null) {
            settings.setEnableInConsolesAndSqlFiles(Boolean.parseBoolean(enableConsoles));
        }

        String enableLiterals = Settings.get(KEY_ENABLE_LITERALS);
        if (enableLiterals != null) {
            settings.setEnableInLiteralsWithSqlInjection(Boolean.parseBoolean(enableLiterals));
        }

        String subStrings = Settings.get(KEY_SUBSTITUTE_STRINGS);
        if (subStrings != null) {
            settings.setSubstituteInsideSqlStrings(Boolean.parseBoolean(subStrings));
        }

        String countStr = Settings.get(KEY_PATTERN_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<DatabaseUserParameterPattern> loaded = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String pat = Settings.get(KEY_PATTERN_PREFIX + i + "_pattern");
                    String scope = Settings.get(KEY_PATTERN_PREFIX + i + "_scope");
                    String lang = Settings.get(KEY_PATTERN_PREFIX + i + "_language");
                    if (pat != null) {
                        loaded.add(new DatabaseUserParameterPattern(pat, scope != null ? scope : "everywhere", lang != null ? lang : ""));
                    }
                }
                if (!loaded.isEmpty()) {
                    settings.setPatterns(loaded);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return settings;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_ENABLE_CONSOLES, String.valueOf(currentSettings.isEnableInConsolesAndSqlFiles()));
        Settings.put(KEY_ENABLE_LITERALS, String.valueOf(currentSettings.isEnableInLiteralsWithSqlInjection()));
        Settings.put(KEY_SUBSTITUTE_STRINGS, String.valueOf(currentSettings.isSubstituteInsideSqlStrings()));

        List<DatabaseUserParameterPattern> patterns = currentSettings.getPatterns();
        Settings.put(KEY_PATTERN_COUNT, String.valueOf(patterns.size()));
        for (int i = 0; i < patterns.size(); i++) {
            DatabaseUserParameterPattern p = patterns.get(i);
            Settings.put(KEY_PATTERN_PREFIX + i + "_pattern", p.getPattern());
            Settings.put(KEY_PATTERN_PREFIX + i + "_scope", p.getInScope());
            Settings.put(KEY_PATTERN_PREFIX + i + "_language", p.getLanguage());
        }
    }
}
