package dev.lumina.tools;

import dev.lumina.util.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manager handling persistence and change notifications for Tools > JPA Entity Declaration settings.
 */
public class JpaEntityDeclarationSettingsManager {

    private static final JpaEntityDeclarationSettingsManager INSTANCE = new JpaEntityDeclarationSettingsManager();

    private static final String KEY_PREFIX = "tools.jpa.declaration.";
    private static final String KEY_ANNOTATIONS_ON_GETTERS = KEY_PREFIX + "annotations_on_getters";
    private static final String KEY_SERIAL_VERSION_UID = KEY_PREFIX + "serial_version_uid";
    private static final String KEY_HIBERNATE_CUSTOM_TYPES = KEY_PREFIX + "hibernate_custom_types";
    private static final String KEY_FETCH_TYPE_LAZY = KEY_PREFIX + "fetch_type_lazy";
    private static final String KEY_RETURN_THIS_SETTERS = KEY_PREFIX + "return_this_setters";
    private static final String KEY_SCAFFOLDING_LANG = KEY_PREFIX + "scaffolding_lang";
    private static final String KEY_ACCESS_MODIFIER = KEY_PREFIX + "access_modifier";
    private static final String KEY_INDEX_NAMES_CASE = KEY_PREFIX + "index_names_case";

    private static final String KEY_TEMPLATE_COUNT = KEY_PREFIX + "template.count";
    private static final String KEY_TEMPLATE_ENABLED = KEY_PREFIX + "template.enabled.";
    private static final String KEY_TEMPLATE_TARGET = KEY_PREFIX + "template.target.";
    private static final String KEY_TEMPLATE_CASE = KEY_PREFIX + "template.case.";
    private static final String KEY_TEMPLATE_PREFIX = KEY_PREFIX + "template.prefix.";
    private static final String KEY_TEMPLATE_POSTFIX = KEY_PREFIX + "template.postfix.";
    private static final String KEY_TEMPLATE_UNDERSCORE = KEY_PREFIX + "template.underscore.";
    private static final String KEY_TEMPLATE_PLURALIZE = KEY_PREFIX + "template.pluralize.";

    private static final String KEY_LOMBOK_GETTER_SETTER = KEY_PREFIX + "lombok.getter_setter";
    private static final String KEY_LOMBOK_BUILDER = KEY_PREFIX + "lombok.builder";
    private static final String KEY_LOMBOK_ALL_ARGS = KEY_PREFIX + "lombok.all_args";
    private static final String KEY_LOMBOK_NO_ARGS = KEY_PREFIX + "lombok.no_args";
    private static final String KEY_LOMBOK_TO_STRING = KEY_PREFIX + "lombok.to_string";
    private static final String KEY_LOMBOK_TO_STRING_EXPLICIT = KEY_PREFIX + "lombok.to_string_explicit";

    private static final String KEY_CONSTANTS_GENERATE = KEY_PREFIX + "constants.generate";
    private static final String KEY_CONSTANTS_ENTITY = KEY_PREFIX + "constants.entity";
    private static final String KEY_CONSTANTS_TABLE = KEY_PREFIX + "constants.table";
    private static final String KEY_CONSTANTS_COLUMN = KEY_PREFIX + "constants.column";
    private static final String KEY_CONSTANTS_PLACEMENT = KEY_PREFIX + "constants.placement";

    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private JpaEntityDeclarationSettingsManager() {
    }

    public static JpaEntityDeclarationSettingsManager getInstance() {
        return INSTANCE;
    }

    public JpaEntityDeclarationSettings load() {
        return getSettings();
    }

    public JpaEntityDeclarationSettings getSettings() {
        JpaEntityDeclarationSettings s = new JpaEntityDeclarationSettings();

        String val = Settings.get(KEY_ANNOTATIONS_ON_GETTERS);
        if (val != null) s.setGenerateJpaAnnotationsOnGetterMethod(Boolean.parseBoolean(val));

        val = Settings.get(KEY_SERIAL_VERSION_UID);
        if (val != null) s.setGenerateSerialVersionUidField(Boolean.parseBoolean(val));

        val = Settings.get(KEY_HIBERNATE_CUSTOM_TYPES);
        if (val != null) s.setRegisterHibernateCustomTypesOnEntity(Boolean.parseBoolean(val));

        val = Settings.get(KEY_FETCH_TYPE_LAZY);
        if (val != null) s.setUseFetchTypeLazy(Boolean.parseBoolean(val));

        val = Settings.get(KEY_RETURN_THIS_SETTERS);
        if (val != null) s.setGenerateReturnThisInAttributeSetters(Boolean.parseBoolean(val));

        val = Settings.get(KEY_SCAFFOLDING_LANG);
        if (val != null) s.setScaffoldingLanguage(val);

        val = Settings.get(KEY_ACCESS_MODIFIER);
        if (val != null) s.setDefaultEntityAttributeAccessModifier(val);

        val = Settings.get(KEY_INDEX_NAMES_CASE);
        if (val != null) s.setIndexConstraintNamesCase(val);

        // Name templates
        String countStr = Settings.get(KEY_TEMPLATE_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                List<JpaNameTemplateEntry> list = new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    boolean enabled = Boolean.parseBoolean(Settings.get(KEY_TEMPLATE_ENABLED + i));
                    String target = Settings.get(KEY_TEMPLATE_TARGET + i);
                    String caseFmt = Settings.get(KEY_TEMPLATE_CASE + i);
                    String prefix = Settings.get(KEY_TEMPLATE_PREFIX + i);
                    String postfix = Settings.get(KEY_TEMPLATE_POSTFIX + i);
                    boolean underscore = Boolean.parseBoolean(Settings.get(KEY_TEMPLATE_UNDERSCORE + i));
                    boolean pluralize = Boolean.parseBoolean(Settings.get(KEY_TEMPLATE_PLURALIZE + i));
                    list.add(new JpaNameTemplateEntry(enabled, target, caseFmt, prefix, postfix, underscore, pluralize));
                }
                s.setNameTemplates(list);
            } catch (Exception ignored) {
            }
        }

        // Lombok
        val = Settings.get(KEY_LOMBOK_GETTER_SETTER);
        if (val != null) s.setLombokGenerateGetterAndSetter(Boolean.parseBoolean(val));

        val = Settings.get(KEY_LOMBOK_BUILDER);
        if (val != null) s.setLombokGenerateBuilder(Boolean.parseBoolean(val));

        val = Settings.get(KEY_LOMBOK_ALL_ARGS);
        if (val != null) s.setLombokGenerateAllArgsConstructor(Boolean.parseBoolean(val));

        val = Settings.get(KEY_LOMBOK_NO_ARGS);
        if (val != null) s.setLombokGenerateNoArgsConstructor(Boolean.parseBoolean(val));

        val = Settings.get(KEY_LOMBOK_TO_STRING);
        if (val != null) s.setLombokGenerateToString(Boolean.parseBoolean(val));

        val = Settings.get(KEY_LOMBOK_TO_STRING_EXPLICIT);
        if (val != null) s.setLombokGenerateToStringWithOnlyExplicitlyIncluded(Boolean.parseBoolean(val));

        // Constants
        val = Settings.get(KEY_CONSTANTS_GENERATE);
        if (val != null) s.setGenerateConstantsForNewObjectNames(Boolean.parseBoolean(val));

        val = Settings.get(KEY_CONSTANTS_ENTITY);
        if (val != null) s.setConstantsEntityName(Boolean.parseBoolean(val));

        val = Settings.get(KEY_CONSTANTS_TABLE);
        if (val != null) s.setConstantsTableName(Boolean.parseBoolean(val));

        val = Settings.get(KEY_CONSTANTS_COLUMN);
        if (val != null) s.setConstantsColumnName(Boolean.parseBoolean(val));

        val = Settings.get(KEY_CONSTANTS_PLACEMENT);
        if (val != null) s.setConstantsPlacement(val);

        return s;
    }

    public void setSettings(JpaEntityDeclarationSettings s) {
        if (s == null) return;

        Settings.put(KEY_ANNOTATIONS_ON_GETTERS, String.valueOf(s.isGenerateJpaAnnotationsOnGetterMethod()));
        Settings.put(KEY_SERIAL_VERSION_UID, String.valueOf(s.isGenerateSerialVersionUidField()));
        Settings.put(KEY_HIBERNATE_CUSTOM_TYPES, String.valueOf(s.isRegisterHibernateCustomTypesOnEntity()));
        Settings.put(KEY_FETCH_TYPE_LAZY, String.valueOf(s.isUseFetchTypeLazy()));
        Settings.put(KEY_RETURN_THIS_SETTERS, String.valueOf(s.isGenerateReturnThisInAttributeSetters()));
        Settings.put(KEY_SCAFFOLDING_LANG, s.getScaffoldingLanguage());
        Settings.put(KEY_ACCESS_MODIFIER, s.getDefaultEntityAttributeAccessModifier());
        Settings.put(KEY_INDEX_NAMES_CASE, s.getIndexConstraintNamesCase());

        // Name templates
        List<JpaNameTemplateEntry> templates = s.getNameTemplates();
        Settings.put(KEY_TEMPLATE_COUNT, String.valueOf(templates.size()));
        for (int i = 0; i < templates.size(); i++) {
            JpaNameTemplateEntry e = templates.get(i);
            Settings.put(KEY_TEMPLATE_ENABLED + i, String.valueOf(e.isEnabled()));
            Settings.put(KEY_TEMPLATE_TARGET + i, e.getTarget());
            Settings.put(KEY_TEMPLATE_CASE + i, e.getCaseFormat());
            Settings.put(KEY_TEMPLATE_PREFIX + i, e.getPrefix());
            Settings.put(KEY_TEMPLATE_POSTFIX + i, e.getPostfix());
            Settings.put(KEY_TEMPLATE_UNDERSCORE + i, String.valueOf(e.isUnderscore()));
            Settings.put(KEY_TEMPLATE_PLURALIZE + i, String.valueOf(e.isPluralize()));
        }

        // Lombok
        Settings.put(KEY_LOMBOK_GETTER_SETTER, String.valueOf(s.isLombokGenerateGetterAndSetter()));
        Settings.put(KEY_LOMBOK_BUILDER, String.valueOf(s.isLombokGenerateBuilder()));
        Settings.put(KEY_LOMBOK_ALL_ARGS, String.valueOf(s.isLombokGenerateAllArgsConstructor()));
        Settings.put(KEY_LOMBOK_NO_ARGS, String.valueOf(s.isLombokGenerateNoArgsConstructor()));
        Settings.put(KEY_LOMBOK_TO_STRING, String.valueOf(s.isLombokGenerateToString()));
        Settings.put(KEY_LOMBOK_TO_STRING_EXPLICIT, String.valueOf(s.isLombokGenerateToStringWithOnlyExplicitlyIncluded()));

        // Constants
        Settings.put(KEY_CONSTANTS_GENERATE, String.valueOf(s.isGenerateConstantsForNewObjectNames()));
        Settings.put(KEY_CONSTANTS_ENTITY, String.valueOf(s.isConstantsEntityName()));
        Settings.put(KEY_CONSTANTS_TABLE, String.valueOf(s.isConstantsTableName()));
        Settings.put(KEY_CONSTANTS_COLUMN, String.valueOf(s.isConstantsColumnName()));
        Settings.put(KEY_CONSTANTS_PLACEMENT, s.getConstantsPlacement());

        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public void clear() {
        Settings.put(KEY_ANNOTATIONS_ON_GETTERS, null);
        Settings.put(KEY_SERIAL_VERSION_UID, null);
        Settings.put(KEY_HIBERNATE_CUSTOM_TYPES, null);
        Settings.put(KEY_FETCH_TYPE_LAZY, null);
        Settings.put(KEY_RETURN_THIS_SETTERS, null);
        Settings.put(KEY_SCAFFOLDING_LANG, null);
        Settings.put(KEY_ACCESS_MODIFIER, null);
        Settings.put(KEY_INDEX_NAMES_CASE, null);

        String countStr = Settings.get(KEY_TEMPLATE_COUNT);
        if (countStr != null) {
            try {
                int count = Integer.parseInt(countStr);
                for (int i = 0; i < count; i++) {
                    Settings.put(KEY_TEMPLATE_ENABLED + i, null);
                    Settings.put(KEY_TEMPLATE_TARGET + i, null);
                    Settings.put(KEY_TEMPLATE_CASE + i, null);
                    Settings.put(KEY_TEMPLATE_PREFIX + i, null);
                    Settings.put(KEY_TEMPLATE_POSTFIX + i, null);
                    Settings.put(KEY_TEMPLATE_UNDERSCORE + i, null);
                    Settings.put(KEY_TEMPLATE_PLURALIZE + i, null);
                }
            } catch (Exception ignored) {
            }
            Settings.put(KEY_TEMPLATE_COUNT, null);
        }

        Settings.put(KEY_LOMBOK_GETTER_SETTER, null);
        Settings.put(KEY_LOMBOK_BUILDER, null);
        Settings.put(KEY_LOMBOK_ALL_ARGS, null);
        Settings.put(KEY_LOMBOK_NO_ARGS, null);
        Settings.put(KEY_LOMBOK_TO_STRING, null);
        Settings.put(KEY_LOMBOK_TO_STRING_EXPLICIT, null);

        Settings.put(KEY_CONSTANTS_GENERATE, null);
        Settings.put(KEY_CONSTANTS_ENTITY, null);
        Settings.put(KEY_CONSTANTS_TABLE, null);
        Settings.put(KEY_CONSTANTS_COLUMN, null);
        Settings.put(KEY_CONSTANTS_PLACEMENT, null);

        notifyListeners();
    }

    public void resetDefaults() {
        setSettings(new JpaEntityDeclarationSettings());
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
