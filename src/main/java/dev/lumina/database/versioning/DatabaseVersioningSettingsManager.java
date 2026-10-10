package dev.lumina.database.versioning;

import dev.lumina.util.Settings;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Database Versioning settings in Lumina IDE.
 */
public class DatabaseVersioningSettingsManager {

    private static final String KEY_PREFIX = "database.versioning.";
    private static final String KEY_PHYSICAL_NAMING = KEY_PREFIX + "physical_naming_strategy";
    private static final String KEY_SEQUENCE_NAMING = KEY_PREFIX + "sequence_naming_strategy";
    private static final String KEY_MAX_IDENTIFIER = KEY_PREFIX + "max_identifier_length";
    private static final String KEY_CREATE_INDEX_FK = KEY_PREFIX + "create_index_for_fk";
    private static final String KEY_PK_NAMED = KEY_PREFIX + "pk_constraint_named";
    private static final String KEY_PK_PREFIX = KEY_PREFIX + "pk_prefix";
    private static final String KEY_PK_PATTERN = KEY_PREFIX + "pk_pattern";
    private static final String KEY_PK_SUFFIX = KEY_PREFIX + "pk_suffix";

    private static volatile DatabaseVersioningSettingsManager instance;

    private DatabaseVersioningSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseVersioningSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseVersioningSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseVersioningSettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseVersioningSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseVersioningSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseVersioningSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseVersioningSettings();
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

    private DatabaseVersioningSettings loadSettings() {
        DatabaseVersioningSettings s = new DatabaseVersioningSettings();

        String physical = Settings.get(KEY_PHYSICAL_NAMING);
        if (physical != null) s.setPhysicalNamingStrategy(physical);

        String seq = Settings.get(KEY_SEQUENCE_NAMING);
        if (seq != null) s.setSequenceNamingStrategy(seq);

        String maxLen = Settings.get(KEY_MAX_IDENTIFIER);
        if (maxLen != null) {
            try {
                s.setMaxDbIdentifierLength(Integer.parseInt(maxLen));
            } catch (NumberFormatException ignored) {
            }
        }

        String createIdx = Settings.get(KEY_CREATE_INDEX_FK);
        if (createIdx != null) s.setCreateIndexForAssociationFk(Boolean.parseBoolean(createIdx));

        String pkNamed = Settings.get(KEY_PK_NAMED);
        if (pkNamed != null) s.setPrimaryKeyConstraintNamed(Boolean.parseBoolean(pkNamed));

        String pkPrefix = Settings.get(KEY_PK_PREFIX);
        if (pkPrefix != null) s.setPkConstraintPrefix(pkPrefix);

        String pkPat = Settings.get(KEY_PK_PATTERN);
        if (pkPat != null) s.setPkConstraintPattern(pkPat);

        String pkSuffix = Settings.get(KEY_PK_SUFFIX);
        if (pkSuffix != null) s.setPkConstraintSuffix(pkSuffix);

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_PHYSICAL_NAMING, currentSettings.getPhysicalNamingStrategy());
        Settings.put(KEY_SEQUENCE_NAMING, currentSettings.getSequenceNamingStrategy());
        Settings.put(KEY_MAX_IDENTIFIER, String.valueOf(currentSettings.getMaxDbIdentifierLength()));
        Settings.put(KEY_CREATE_INDEX_FK, String.valueOf(currentSettings.isCreateIndexForAssociationFk()));
        Settings.put(KEY_PK_NAMED, String.valueOf(currentSettings.isPrimaryKeyConstraintNamed()));
        Settings.put(KEY_PK_PREFIX, currentSettings.getPkConstraintPrefix());
        Settings.put(KEY_PK_PATTERN, currentSettings.getPkConstraintPattern());
        Settings.put(KEY_PK_SUFFIX, currentSettings.getPkConstraintSuffix());
    }
}
