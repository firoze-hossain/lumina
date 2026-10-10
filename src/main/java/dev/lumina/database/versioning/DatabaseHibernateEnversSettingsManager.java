package dev.lumina.database.versioning;

import dev.lumina.util.Settings;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Hibernate Envers settings in Lumina IDE.
 */
public class DatabaseHibernateEnversSettingsManager {

    private static final String KEY_PREFIX = "database.versioning.envers.";
    private static final String KEY_USE_PROP = KEY_PREFIX + "use_properties_files";
    private static final String KEY_AUDIT_PREFIX = KEY_PREFIX + "audit_table_prefix";
    private static final String KEY_AUDIT_SUFFIX = KEY_PREFIX + "audit_table_suffix";
    private static final String KEY_REV_FIELD = KEY_PREFIX + "revision_field_name";
    private static final String KEY_REV_TYPE = KEY_PREFIX + "revision_type_field_name";
    private static final String KEY_SCHEMA = KEY_PREFIX + "default_schema_name";
    private static final String KEY_OPT_LOCK = KEY_PREFIX + "treat_optimistic_locking_unversioned";
    private static final String KEY_TRACK_ENTITY = KEY_PREFIX + "track_entity_names_changed";
    private static final String KEY_ACT_MOD = KEY_PREFIX + "activate_modified_properties_flag";
    private static final String KEY_MOD_SUFFIX = KEY_PREFIX + "suffix_modified_flag_columns";

    private static volatile DatabaseHibernateEnversSettingsManager instance;

    private DatabaseHibernateEnversSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseHibernateEnversSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseHibernateEnversSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseHibernateEnversSettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseHibernateEnversSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseHibernateEnversSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseHibernateEnversSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseHibernateEnversSettings();
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

    private DatabaseHibernateEnversSettings loadSettings() {
        DatabaseHibernateEnversSettings s = new DatabaseHibernateEnversSettings();

        String useProp = Settings.get(KEY_USE_PROP);
        if (useProp != null) s.setUseValuesInPropertiesFiles(Boolean.parseBoolean(useProp));

        String pfx = Settings.get(KEY_AUDIT_PREFIX);
        if (pfx != null) s.setAuditTablePrefix(pfx);

        String sfx = Settings.get(KEY_AUDIT_SUFFIX);
        if (sfx != null) s.setAuditTableSuffix(sfx);

        String rev = Settings.get(KEY_REV_FIELD);
        if (rev != null) s.setRevisionFieldName(rev);

        String revType = Settings.get(KEY_REV_TYPE);
        if (revType != null) s.setRevisionTypeFieldName(revType);

        String schema = Settings.get(KEY_SCHEMA);
        if (schema != null) s.setDefaultSchemaName(schema);

        String optLock = Settings.get(KEY_OPT_LOCK);
        if (optLock != null) s.setTreatOptimisticLockingUnversioned(Boolean.parseBoolean(optLock));

        String track = Settings.get(KEY_TRACK_ENTITY);
        if (track != null) s.setTrackEntityNamesChanged(Boolean.parseBoolean(track));

        String actMod = Settings.get(KEY_ACT_MOD);
        if (actMod != null) s.setActivateModifiedPropertiesFlag(Boolean.parseBoolean(actMod));

        String modSfx = Settings.get(KEY_MOD_SUFFIX);
        if (modSfx != null) s.setSuffixModifiedFlagColumns(modSfx);

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_USE_PROP, String.valueOf(currentSettings.isUseValuesInPropertiesFiles()));
        Settings.put(KEY_AUDIT_PREFIX, currentSettings.getAuditTablePrefix());
        Settings.put(KEY_AUDIT_SUFFIX, currentSettings.getAuditTableSuffix());
        Settings.put(KEY_REV_FIELD, currentSettings.getRevisionFieldName());
        Settings.put(KEY_REV_TYPE, currentSettings.getRevisionTypeFieldName());
        Settings.put(KEY_SCHEMA, currentSettings.getDefaultSchemaName());
        Settings.put(KEY_OPT_LOCK, String.valueOf(currentSettings.isTreatOptimisticLockingUnversioned()));
        Settings.put(KEY_TRACK_ENTITY, String.valueOf(currentSettings.isTrackEntityNamesChanged()));
        Settings.put(KEY_ACT_MOD, String.valueOf(currentSettings.isActivateModifiedPropertiesFlag()));
        Settings.put(KEY_MOD_SUFFIX, currentSettings.getSuffixModifiedFlagColumns());
    }
}
