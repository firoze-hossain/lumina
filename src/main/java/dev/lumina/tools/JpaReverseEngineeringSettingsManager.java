package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > JPA Reverse Engineering settings.
 */
public class JpaReverseEngineeringSettingsManager {

    private static final JpaReverseEngineeringSettingsManager INSTANCE = new JpaReverseEngineeringSettingsManager();

    private static final String KEY_PREFIX = "tools.jpa.reverse_eng.";
    private static final String KEY_FETCH_TYPE_LAZY = KEY_PREFIX + "fetch_type_lazy";
    private static final String KEY_VALIDATION_ANNOTATIONS = KEY_PREFIX + "validation_annotations";
    private static final String KEY_SINGULAR_CLASS_NAME = KEY_PREFIX + "singular_class_name";
    private static final String KEY_REPLACE_ORM_BASIC_TYPES = KEY_PREFIX + "replace_orm_basic_types";
    private static final String KEY_TABLE_COLUMN_COMMENTS = KEY_PREFIX + "table_column_comments";
    private static final String KEY_NAMING_RULES_MODE = KEY_PREFIX + "naming_rules_mode";
    private static final String KEY_PREFIXES_TABLE = KEY_PREFIX + "prefixes_table";
    private static final String KEY_PREFIXES_COLUMN = KEY_PREFIX + "prefixes_column";
    private static final String KEY_SUFFIXES_TABLE = KEY_PREFIX + "suffixes_table";
    private static final String KEY_SUFFIXES_COLUMN = KEY_PREFIX + "suffixes_column";
    private static final String KEY_KEYWORD_SUFFIX = KEY_PREFIX + "keyword_suffix";
    private static final String KEY_SELECTED_DB_ENGINE = KEY_PREFIX + "selected_db_engine";

    private static final String KEY_MAPPINGS_COUNT = KEY_PREFIX + "mappings.count";
    private static final String KEY_MAPPING_ENGINE = KEY_PREFIX + "mappings.engine.";
    private static final String KEY_MAPPING_SQL = KEY_PREFIX + "mappings.sql.";
    private static final String KEY_MAPPING_TARGET = KEY_PREFIX + "mappings.target.";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private JpaReverseEngineeringSettingsManager() {
    }

    public static JpaReverseEngineeringSettingsManager getInstance() {
        return INSTANCE;
    }

    public JpaReverseEngineeringSettings load() {
        return getSettings();
    }

    public JpaReverseEngineeringSettings getSettings() {
        JpaReverseEngineeringSettings s = new JpaReverseEngineeringSettings();

        String val = Settings.get(KEY_FETCH_TYPE_LAZY);
        if (val != null) s.setUseFetchTypeLazy(Boolean.parseBoolean(val));

        val = Settings.get(KEY_VALIDATION_ANNOTATIONS);
        if (val != null) s.setUseValidationAnnotations(Boolean.parseBoolean(val));

        val = Settings.get(KEY_SINGULAR_CLASS_NAME);
        if (val != null) s.setConvertTableNameToSingular(Boolean.parseBoolean(val));

        val = Settings.get(KEY_REPLACE_ORM_BASIC_TYPES);
        if (val != null) s.setReplaceOrmReferencesWithBasicTypes(Boolean.parseBoolean(val));

        val = Settings.get(KEY_TABLE_COLUMN_COMMENTS);
        if (val != null) s.setTableAndColumnComments(val);

        val = Settings.get(KEY_NAMING_RULES_MODE);
        if (val != null) s.setNamingRulesMode(val);

        val = Settings.get(KEY_PREFIXES_TABLE);
        if (val != null) s.setPrefixesToSkipInTableName(val);

        val = Settings.get(KEY_PREFIXES_COLUMN);
        if (val != null) s.setPrefixesToSkipInColumnName(val);

        val = Settings.get(KEY_SUFFIXES_TABLE);
        if (val != null) s.setSuffixesToSkipInTableName(val);

        val = Settings.get(KEY_SUFFIXES_COLUMN);
        if (val != null) s.setSuffixesToSkipInColumnName(val);

        val = Settings.get(KEY_KEYWORD_SUFFIX);
        if (val != null) s.setReservedKeywordFieldSuffix(val);

        val = Settings.get(KEY_SELECTED_DB_ENGINE);
        if (val != null) s.setSelectedDatabaseEngine(val);

        // Mappings
        String countStr = Settings.get(KEY_MAPPINGS_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<JpaTypeMappingEntry> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    String eng = Settings.get(KEY_MAPPING_ENGINE + i);
                    String sql = Settings.get(KEY_MAPPING_SQL + i);
                    String tgt = Settings.get(KEY_MAPPING_TARGET + i);
                    list.add(new JpaTypeMappingEntry(eng, sql, tgt));
                }
                s.setTypeMappings(list);
            } catch (Exception ignored) {
            }
        }

        return s;
    }

    public void setSettings(JpaReverseEngineeringSettings s) {
        if (s == null) return;

        Settings.put(KEY_FETCH_TYPE_LAZY, String.valueOf(s.isUseFetchTypeLazy()));
        Settings.put(KEY_VALIDATION_ANNOTATIONS, String.valueOf(s.isUseValidationAnnotations()));
        Settings.put(KEY_SINGULAR_CLASS_NAME, String.valueOf(s.isConvertTableNameToSingular()));
        Settings.put(KEY_REPLACE_ORM_BASIC_TYPES, String.valueOf(s.isReplaceOrmReferencesWithBasicTypes()));
        Settings.put(KEY_TABLE_COLUMN_COMMENTS, s.getTableAndColumnComments());
        Settings.put(KEY_NAMING_RULES_MODE, s.getNamingRulesMode());
        Settings.put(KEY_PREFIXES_TABLE, s.getPrefixesToSkipInTableName());
        Settings.put(KEY_PREFIXES_COLUMN, s.getPrefixesToSkipInColumnName());
        Settings.put(KEY_SUFFIXES_TABLE, s.getSuffixesToSkipInTableName());
        Settings.put(KEY_SUFFIXES_COLUMN, s.getSuffixesToSkipInColumnName());
        Settings.put(KEY_KEYWORD_SUFFIX, s.getReservedKeywordFieldSuffix());
        Settings.put(KEY_SELECTED_DB_ENGINE, s.getSelectedDatabaseEngine());

        List<JpaTypeMappingEntry> mappings = s.getTypeMappings();
        Settings.put(KEY_MAPPINGS_COUNT, String.valueOf(mappings.size()));
        for (int i = 0; i < mappings.size(); i++) {
            JpaTypeMappingEntry e = mappings.get(i);
            Settings.put(KEY_MAPPING_ENGINE + i, e.getDatabaseEngine());
            Settings.put(KEY_MAPPING_SQL + i, e.getSqlType());
            Settings.put(KEY_MAPPING_TARGET + i, e.getTargetType());
        }

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void clear() {
        Settings.put(KEY_FETCH_TYPE_LAZY, null);
        Settings.put(KEY_VALIDATION_ANNOTATIONS, null);
        Settings.put(KEY_SINGULAR_CLASS_NAME, null);
        Settings.put(KEY_REPLACE_ORM_BASIC_TYPES, null);
        Settings.put(KEY_TABLE_COLUMN_COMMENTS, null);
        Settings.put(KEY_NAMING_RULES_MODE, null);
        Settings.put(KEY_PREFIXES_TABLE, null);
        Settings.put(KEY_PREFIXES_COLUMN, null);
        Settings.put(KEY_SUFFIXES_TABLE, null);
        Settings.put(KEY_SUFFIXES_COLUMN, null);
        Settings.put(KEY_KEYWORD_SUFFIX, null);
        Settings.put(KEY_SELECTED_DB_ENGINE, null);

        String countStr = Settings.get(KEY_MAPPINGS_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                for (int i = 0; i < count; i++) {
                    Settings.put(KEY_MAPPING_ENGINE + i, null);
                    Settings.put(KEY_MAPPING_SQL + i, null);
                    Settings.put(KEY_MAPPING_TARGET + i, null);
                }
            } catch (Exception ignored) {
            }
            Settings.put(KEY_MAPPINGS_COUNT, null);
        }

        notifyListeners();
    }

    public void resetDefaults() {
        setSettings(new JpaReverseEngineeringSettings());
    }

    private void notifyListeners() {
        for (Runnable l : listeners) {
            try {
                l.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
