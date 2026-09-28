package dev.lumina.settings;

import dev.lumina.util.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Universal dynamic configuration and model for Editor > Code Style.
 * Supports:
 * - Scheme architecture (Global IDE schemes vs Project-level scheme)
 * - General and Formatter settings (line separator, hard wrap, visual guides, indents detection, editorconfig)
 * - Extensible language code style provider registry (Java, Kotlin, Python, JS, Go, Rust, HTML, etc.)
 * - Zero hardcoding: dynamic providers supply defaults, supported tabs, and sample code for real-time live preview.
 */
public final class CodeStyleSettings {

    private static final CodeStyleSettings INSTANCE = new CodeStyleSettings();

    public static CodeStyleSettings getInstance() {
        return INSTANCE;
    }

    public static void ensureInitialized() {
        getInstance();
    }

    public enum LineSeparator {
        SYSTEM("System-Dependent", System.lineSeparator()),
        UNIX("Unix and macOS (\\n)", "\n"),
        WINDOWS("Windows (\\r\\n)", "\r\n"),
        MAC_CLASSIC("Classic Mac OS (\\r)", "\r");

        private final String display;
        private final String characters;

        LineSeparator(String display, String characters) {
            this.display = display;
            this.characters = characters;
        }

        public String getDisplay() {
            return display;
        }

        public String getCharacters() {
            return characters;
        }

        public static LineSeparator fromDisplay(String str) {
            if (str == null) return SYSTEM;
            for (LineSeparator ls : values()) {
                if (ls.display.equalsIgnoreCase(str.trim())) return ls;
            }
            return SYSTEM;
        }
    }

    /**
     * Model representing a single Code Style Scheme.
     * Can be stored either in Project (Project.xml) or in IDE (global preferences).
     */
    public static class CodeStyleScheme {
        private String name;
        private boolean isProjectLevel;
        private LineSeparator lineSeparator = LineSeparator.SYSTEM;
        private int hardWrapAt = 120;
        private boolean wrapOnTyping = false;
        private String visualGuides = "";
        private boolean detectAndUseExistingFileIndents = true;
        private boolean enableEditorConfigSupport = true;

        // Formatter options
        private boolean enableFormatterMarkers = true;
        private String formatterOffMarker = "@formatter:off";
        private String formatterOnMarker = "@formatter:on";
        private boolean enableRegexInFormatterMarkers = false;
        private String doNotFormatPatterns = "";

        // Per-language settings map
        private final Map<String, LanguageCodeStyleSettings> languageSettings = new ConcurrentHashMap<>();

        public CodeStyleScheme(String name, boolean isProjectLevel) {
            this.name = name;
            this.isProjectLevel = isProjectLevel;
        }

        public CodeStyleScheme copy(String newName, boolean newProjectLevel) {
            CodeStyleScheme c = new CodeStyleScheme(newName, newProjectLevel);
            c.lineSeparator = this.lineSeparator;
            c.hardWrapAt = this.hardWrapAt;
            c.wrapOnTyping = this.wrapOnTyping;
            c.visualGuides = this.visualGuides;
            c.detectAndUseExistingFileIndents = this.detectAndUseExistingFileIndents;
            c.enableEditorConfigSupport = this.enableEditorConfigSupport;
            c.enableFormatterMarkers = this.enableFormatterMarkers;
            c.formatterOffMarker = this.formatterOffMarker;
            c.formatterOnMarker = this.formatterOnMarker;
            c.enableRegexInFormatterMarkers = this.enableRegexInFormatterMarkers;
            c.doNotFormatPatterns = this.doNotFormatPatterns;
            for (Map.Entry<String, LanguageCodeStyleSettings> entry : this.languageSettings.entrySet()) {
                c.languageSettings.put(entry.getKey(), entry.getValue().copy());
            }
            return c;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isProjectLevel() {
            return isProjectLevel;
        }

        public void setProjectLevel(boolean projectLevel) {
            isProjectLevel = projectLevel;
        }

        public LineSeparator getLineSeparator() {
            return lineSeparator;
        }

        public void setLineSeparator(LineSeparator lineSeparator) {
            this.lineSeparator = lineSeparator != null ? lineSeparator : LineSeparator.SYSTEM;
        }

        public int getHardWrapAt() {
            return hardWrapAt;
        }

        public void setHardWrapAt(int hardWrapAt) {
            this.hardWrapAt = hardWrapAt;
        }

        public boolean isWrapOnTyping() {
            return wrapOnTyping;
        }

        public void setWrapOnTyping(boolean wrapOnTyping) {
            this.wrapOnTyping = wrapOnTyping;
        }

        public String getVisualGuides() {
            return visualGuides != null ? visualGuides : "";
        }

        public void setVisualGuides(String visualGuides) {
            this.visualGuides = visualGuides != null ? visualGuides : "";
        }

        public boolean isDetectAndUseExistingFileIndents() {
            return detectAndUseExistingFileIndents;
        }

        public void setDetectAndUseExistingFileIndents(boolean detect) {
            this.detectAndUseExistingFileIndents = detect;
        }

        public boolean isEnableEditorConfigSupport() {
            return enableEditorConfigSupport;
        }

        public void setEnableEditorConfigSupport(boolean enable) {
            this.enableEditorConfigSupport = enable;
        }

        public boolean isEnableFormatterMarkers() {
            return enableFormatterMarkers;
        }

        public void setEnableFormatterMarkers(boolean enableFormatterMarkers) {
            this.enableFormatterMarkers = enableFormatterMarkers;
        }

        public String getFormatterOffMarker() {
            return formatterOffMarker;
        }

        public void setFormatterOffMarker(String formatterOffMarker) {
            this.formatterOffMarker = formatterOffMarker;
        }

        public String getFormatterOnMarker() {
            return formatterOnMarker;
        }

        public void setFormatterOnMarker(String formatterOnMarker) {
            this.formatterOnMarker = formatterOnMarker;
        }

        public boolean isEnableRegexInFormatterMarkers() {
            return enableRegexInFormatterMarkers;
        }

        public void setEnableRegexInFormatterMarkers(boolean enableRegexInFormatterMarkers) {
            this.enableRegexInFormatterMarkers = enableRegexInFormatterMarkers;
        }

        public String getDoNotFormatPatterns() {
            return doNotFormatPatterns;
        }

        public void setDoNotFormatPatterns(String doNotFormatPatterns) {
            this.doNotFormatPatterns = doNotFormatPatterns;
        }

        public LanguageCodeStyleSettings getLanguageSettings(String languageId) {
            return languageSettings.computeIfAbsent(languageId, id -> {
                LanguageCodeStyleProvider provider = LanguageCodeStyleProvider.getProvider(id);
                if (provider != null) {
                    return provider.createDefaultSettings();
                }
                return new LanguageCodeStyleSettings(id);
            });
        }

        public void setLanguageSettings(String languageId, LanguageCodeStyleSettings settings) {
            if (settings != null) {
                languageSettings.put(languageId, settings);
            }
        }

        public Map<String, LanguageCodeStyleSettings> getAllLanguageSettings() {
            return Collections.unmodifiableMap(languageSettings);
        }
    }

    /**
     * Code style settings for an individual programming/markup language.
     */
    public static class LanguageCodeStyleSettings {
        private String languageId;
        private boolean useTabCharacter = false;
        private boolean smartTabs = false;
        private int tabSize = 4;
        private int indent = 4;
        private int continuationIndent = 8;
        private boolean keepIndentsOnEmptyLines = false;
        private int labelIndent = 0;
        private boolean absoluteLabelIndent = false;
        private boolean doNotIndentTopLevelMembers = false;
        private boolean useIndentsRelativeToExpressionStart = false;

        // Dynamic custom properties map (for any language, any tab)
        private final Map<String, Object> properties = new ConcurrentHashMap<>();

        public LanguageCodeStyleSettings(String languageId) {
            this.languageId = languageId;
        }

        public LanguageCodeStyleSettings copy() {
            LanguageCodeStyleSettings c = new LanguageCodeStyleSettings(this.languageId);
            c.useTabCharacter = this.useTabCharacter;
            c.smartTabs = this.smartTabs;
            c.tabSize = this.tabSize;
            c.indent = this.indent;
            c.continuationIndent = this.continuationIndent;
            c.keepIndentsOnEmptyLines = this.keepIndentsOnEmptyLines;
            c.labelIndent = this.labelIndent;
            c.absoluteLabelIndent = this.absoluteLabelIndent;
            c.doNotIndentTopLevelMembers = this.doNotIndentTopLevelMembers;
            c.useIndentsRelativeToExpressionStart = this.useIndentsRelativeToExpressionStart;
            c.properties.putAll(this.properties);
            return c;
        }

        public String getLanguageId() {
            return languageId;
        }

        public boolean isUseTabCharacter() {
            return useTabCharacter;
        }

        public void setUseTabCharacter(boolean useTabCharacter) {
            this.useTabCharacter = useTabCharacter;
        }

        public boolean isSmartTabs() {
            return smartTabs;
        }

        public void setSmartTabs(boolean smartTabs) {
            this.smartTabs = smartTabs;
        }

        public int getTabSize() {
            return tabSize;
        }

        public void setTabSize(int tabSize) {
            this.tabSize = Math.max(1, tabSize);
        }

        public int getIndent() {
            return indent;
        }

        public void setIndent(int indent) {
            this.indent = Math.max(1, indent);
        }

        public int getContinuationIndent() {
            return continuationIndent;
        }

        public void setContinuationIndent(int continuationIndent) {
            this.continuationIndent = Math.max(0, continuationIndent);
        }

        public boolean isKeepIndentsOnEmptyLines() {
            return keepIndentsOnEmptyLines;
        }

        public void setKeepIndentsOnEmptyLines(boolean keepIndentsOnEmptyLines) {
            this.keepIndentsOnEmptyLines = keepIndentsOnEmptyLines;
        }

        public int getLabelIndent() {
            return labelIndent;
        }

        public void setLabelIndent(int labelIndent) {
            this.labelIndent = labelIndent;
        }

        public boolean isAbsoluteLabelIndent() {
            return absoluteLabelIndent;
        }

        public void setAbsoluteLabelIndent(boolean absoluteLabelIndent) {
            this.absoluteLabelIndent = absoluteLabelIndent;
        }

        public boolean isDoNotIndentTopLevelMembers() {
            return doNotIndentTopLevelMembers;
        }

        public void setDoNotIndentTopLevelMembers(boolean doNotIndentTopLevelMembers) {
            this.doNotIndentTopLevelMembers = doNotIndentTopLevelMembers;
        }

        public boolean isUseIndentsRelativeToExpressionStart() {
            return useIndentsRelativeToExpressionStart;
        }

        public void setUseIndentsRelativeToExpressionStart(boolean useIndentsRelativeToExpressionStart) {
            this.useIndentsRelativeToExpressionStart = useIndentsRelativeToExpressionStart;
        }

        public Object getProperty(String key) {
            return properties.get(key);
        }

        public void setProperty(String key, Object value) {
            if (value != null) {
                properties.put(key, value);
            } else {
                properties.remove(key);
            }
        }

        public boolean getBoolean(String key, boolean defaultValue) {
            Object v = properties.get(key);
            if (v instanceof Boolean b) return b;
            if (v != null) return Boolean.parseBoolean(v.toString());
            return defaultValue;
        }

        public void setBoolean(String key, boolean value) {
            properties.put(key, value);
        }

        public int getInt(String key, int defaultValue) {
            Object v = properties.get(key);
            if (v instanceof Number n) return n.intValue();
            if (v != null) {
                try { return Integer.parseInt(v.toString().trim()); } catch (Exception ignored) {}
            }
            return defaultValue;
        }

        public void setInt(String key, int value) {
            properties.put(key, value);
        }

        public String getString(String key, String defaultValue) {
            Object v = properties.get(key);
            return v != null ? v.toString() : defaultValue;
        }

        public void setString(String key, String value) {
            if (value != null) {
                properties.put(key, value);
            } else {
                properties.remove(key);
            }
        }

        public Map<String, Object> getAllProperties() {
            return Collections.unmodifiableMap(properties);
        }

        public void setProperties(Map<String, Object> props) {
            if (props != null) {
                properties.putAll(props);
            }
        }
    }

    public enum CodeStyleOptionType {
        CHECKBOX,
        NUMBER,
        COMBO,
        SEPARATOR
    }

    public static class CodeStyleOption {
        private final String key;
        private final String label;
        private final CodeStyleOptionType type;
        private final Object defaultValue;
        private final List<String> choices;
        private final int indentLevel;
        private final boolean rightAligned;

        public CodeStyleOption(String key, String label, CodeStyleOptionType type, Object defaultValue, List<String> choices, boolean indented) {
            this(key, label, type, defaultValue, choices, indented ? 1 : 0, false);
        }

        public CodeStyleOption(String key, String label, CodeStyleOptionType type, Object defaultValue, List<String> choices, int indentLevel) {
            this(key, label, type, defaultValue, choices, indentLevel, false);
        }

        public CodeStyleOption(String key, String label, CodeStyleOptionType type, Object defaultValue, List<String> choices, int indentLevel, boolean rightAligned) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.defaultValue = defaultValue;
            this.choices = choices != null ? List.copyOf(choices) : Collections.emptyList();
            this.indentLevel = indentLevel;
            this.rightAligned = rightAligned;
        }

        public static CodeStyleOption checkbox(String key, String label, boolean defaultValue) {
            return new CodeStyleOption(key, label, CodeStyleOptionType.CHECKBOX, defaultValue, null, 0, false);
        }

        public static CodeStyleOption indentedCheckbox(String key, String label, boolean defaultValue) {
            return new CodeStyleOption(key, label, CodeStyleOptionType.CHECKBOX, defaultValue, null, 1, false);
        }

        public static CodeStyleOption doubleIndentedCheckbox(String key, String label, boolean defaultValue) {
            return new CodeStyleOption(key, label, CodeStyleOptionType.CHECKBOX, defaultValue, null, 2, false);
        }

        public static CodeStyleOption rightAlignedCheckbox(String key, String label, boolean defaultValue) {
            return new CodeStyleOption(key, label, CodeStyleOptionType.CHECKBOX, defaultValue, null, 0, true);
        }

        public static CodeStyleOption number(String key, String label, int defaultValue) {
            return new CodeStyleOption(key, label, CodeStyleOptionType.NUMBER, defaultValue, null, 0, false);
        }

        public static CodeStyleOption combo(String key, String label, List<String> choices, String defaultValue) {
            return new CodeStyleOption(key, label, CodeStyleOptionType.COMBO, defaultValue, choices, 0, false);
        }

        public static CodeStyleOption separator(String label) {
            return new CodeStyleOption(null, label, CodeStyleOptionType.SEPARATOR, null, null, 0, false);
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public CodeStyleOptionType getType() { return type; }
        public Object getDefaultValue() { return defaultValue; }
        public List<String> getChoices() { return choices; }
        public boolean isIndented() { return indentLevel > 0; }
        public int getIndentLevel() { return indentLevel; }
        public boolean isRightAligned() { return rightAligned; }
    }

    public static class CodeStyleGroup {
        private final String name;
        private final boolean collapsible;
        private boolean expanded = true;
        private final boolean divider;
        private final String headerComboKey;
        private final List<String> headerComboChoices;
        private final String headerComboDefault;
        private final List<CodeStyleOption> options = new ArrayList<>();

        public CodeStyleGroup(String name, boolean collapsible, boolean divider, String headerComboKey, List<String> headerComboChoices, String headerComboDefault) {
            this.name = name;
            this.collapsible = collapsible;
            this.divider = divider;
            this.headerComboKey = headerComboKey;
            this.headerComboChoices = headerComboChoices != null ? List.copyOf(headerComboChoices) : Collections.emptyList();
            this.headerComboDefault = headerComboDefault;
        }

        public static CodeStyleGroup collapsible(String name, CodeStyleOption... options) {
            CodeStyleGroup g = new CodeStyleGroup(name, true, false, null, null, null);
            if (options != null) Collections.addAll(g.options, options);
            return g;
        }

        public static CodeStyleGroup collapsibleWithCombo(String name, String comboKey, List<String> choices, String defaultChoice, CodeStyleOption... options) {
            CodeStyleGroup g = new CodeStyleGroup(name, true, false, comboKey, choices, defaultChoice);
            if (options != null) Collections.addAll(g.options, options);
            return g;
        }

        public static CodeStyleGroup divider(String name, CodeStyleOption... options) {
            CodeStyleGroup g = new CodeStyleGroup(name, false, true, null, null, null);
            if (options != null) Collections.addAll(g.options, options);
            return g;
        }

        public static CodeStyleGroup flat(String name, CodeStyleOption... options) {
            CodeStyleGroup g = new CodeStyleGroup(name, false, false, null, null, null);
            if (options != null) Collections.addAll(g.options, options);
            return g;
        }

        public String getName() { return name; }
        public boolean isCollapsible() { return collapsible; }
        public boolean isExpanded() { return expanded; }
        public void setExpanded(boolean expanded) { this.expanded = expanded; }
        public boolean isDivider() { return divider; }
        public String getHeaderComboKey() { return headerComboKey; }
        public List<String> getHeaderComboChoices() { return headerComboChoices; }
        public String getHeaderComboDefault() { return headerComboDefault; }
        public List<CodeStyleOption> getOptions() { return options; }
        public CodeStyleGroup addOption(CodeStyleOption option) {
            if (option != null) options.add(option);
            return this;
        }
    }

    public interface CodeStyleSettingsCustomizer {
        void addGroup(CodeStyleGroup group);
        CodeStyleGroup addCollapsibleGroup(String name);
        CodeStyleGroup addCollapsibleGroupWithCombo(String name, String comboKey, List<String> choices, String defaultChoice);
        CodeStyleGroup addDividerSection(String name);
        void addCheckbox(String groupName, String key, String label, boolean defaultValue);
        void addIndentedCheckbox(String groupName, String key, String label, boolean defaultValue);
        void addDoubleIndentedCheckbox(String groupName, String key, String label, boolean defaultValue);
        void addRightAlignedCheckbox(String groupName, String key, String label, boolean defaultValue);
        void addCombo(String groupName, String key, String label, List<String> choices, String defaultValue);
        void addNumber(String groupName, String key, String label, int defaultValue);
        List<CodeStyleGroup> getGroups();
    }

    public static class DefaultCodeStyleCustomizer implements CodeStyleSettingsCustomizer {
        private final List<CodeStyleGroup> groups = new ArrayList<>();

        @Override
        public void addGroup(CodeStyleGroup group) {
            if (group != null) groups.add(group);
        }

        @Override
        public CodeStyleGroup addCollapsibleGroup(String name) {
            CodeStyleGroup g = CodeStyleGroup.collapsible(name);
            groups.add(g);
            return g;
        }

        @Override
        public CodeStyleGroup addCollapsibleGroupWithCombo(String name, String comboKey, List<String> choices, String defaultChoice) {
            CodeStyleGroup g = CodeStyleGroup.collapsibleWithCombo(name, comboKey, choices, defaultChoice);
            groups.add(g);
            return g;
        }

        @Override
        public CodeStyleGroup addDividerSection(String name) {
            CodeStyleGroup g = CodeStyleGroup.divider(name);
            groups.add(g);
            return g;
        }

        private CodeStyleGroup findOrCreateGroup(String groupName) {
            for (CodeStyleGroup g : groups) {
                if (g.getName().equals(groupName)) return g;
            }
            return addCollapsibleGroup(groupName);
        }

        @Override
        public void addCheckbox(String groupName, String key, String label, boolean defaultValue) {
            findOrCreateGroup(groupName).addOption(CodeStyleOption.checkbox(key, label, defaultValue));
        }

        @Override
        public void addIndentedCheckbox(String groupName, String key, String label, boolean defaultValue) {
            findOrCreateGroup(groupName).addOption(CodeStyleOption.indentedCheckbox(key, label, defaultValue));
        }

        @Override
        public void addDoubleIndentedCheckbox(String groupName, String key, String label, boolean defaultValue) {
            findOrCreateGroup(groupName).addOption(CodeStyleOption.doubleIndentedCheckbox(key, label, defaultValue));
        }

        @Override
        public void addRightAlignedCheckbox(String groupName, String key, String label, boolean defaultValue) {
            findOrCreateGroup(groupName).addOption(CodeStyleOption.rightAlignedCheckbox(key, label, defaultValue));
        }

        @Override
        public void addCombo(String groupName, String key, String label, List<String> choices, String defaultValue) {
            findOrCreateGroup(groupName).addOption(CodeStyleOption.combo(key, label, choices, defaultValue));
        }

        @Override
        public void addNumber(String groupName, String key, String label, int defaultValue) {
            findOrCreateGroup(groupName).addOption(CodeStyleOption.number(key, label, defaultValue));
        }

        @Override
        public List<CodeStyleGroup> getGroups() {
            return groups;
        }
    }

    /**
     * Provider contract for languages registering their Code Style tabs, default indent parameters,
     * and live preview sample code.
     */
    public interface LanguageCodeStyleProvider {
        String getLanguageId();
        String getDisplayName();
        List<String> getSupportedTabs();
        LanguageCodeStyleSettings createDefaultSettings();
        String getSampleCode();
        default String getSampleCode(String tabName) { return getSampleCode(); }
        default boolean hasPreview(String tabName) {
            return "Tabs and Indents".equals(tabName)
                    || "Spaces".equals(tabName)
                    || "Wrapping and Braces".equals(tabName)
                    || "Blank Lines".equals(tabName);
        }
        default List<CodeStyleGroup> getOptionGroups(String tabName) {
            DefaultCodeStyleCustomizer customizer = new DefaultCodeStyleCustomizer();
            customizeSettings(customizer, tabName);
            return customizer.getGroups();
        }
        default void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {}

        Map<String, LanguageCodeStyleProvider> REGISTRY = new LinkedHashMap<>();

        static void register(LanguageCodeStyleProvider provider) {
            REGISTRY.put(provider.getLanguageId(), provider);
        }

        static LanguageCodeStyleProvider getProvider(String languageId) {
            CodeStyleSettings.ensureInitialized();
            return REGISTRY.get(languageId);
        }

        static List<LanguageCodeStyleProvider> getAllProviders() {
            CodeStyleSettings.ensureInitialized();
            return new ArrayList<>(REGISTRY.values());
        }
    }

    // Default Schemes
    private final Map<String, CodeStyleScheme> schemes = new LinkedHashMap<>();
    private String activeSchemeName = "Default";
    private final List<Consumer<CodeStyleScheme>> changeListeners = new CopyOnWriteArrayList<>();

    private CodeStyleSettings() {
        initProviders();
        initDefaultSchemes();
        loadSettings();
    }

    private void initDefaultSchemes() {
        // 1. Stored in IDE: Default
        CodeStyleScheme defaultIdeScheme = new CodeStyleScheme("Default", false);
        for (LanguageCodeStyleProvider p : LanguageCodeStyleProvider.getAllProviders()) {
            defaultIdeScheme.setLanguageSettings(p.getLanguageId(), p.createDefaultSettings());
        }
        schemes.put(defaultIdeScheme.getName(), defaultIdeScheme);

        // 2. Stored in Project: Project
        CodeStyleScheme projectScheme = new CodeStyleScheme("Project", true);
        for (LanguageCodeStyleProvider p : LanguageCodeStyleProvider.getAllProviders()) {
            projectScheme.setLanguageSettings(p.getLanguageId(), p.createDefaultSettings());
        }
        schemes.put(projectScheme.getName(), projectScheme);
    }

    public List<CodeStyleScheme> getSchemes() {
        return new ArrayList<>(schemes.values());
    }

    public CodeStyleScheme getScheme(String name) {
        return schemes.get(name);
    }

    public CodeStyleScheme getActiveScheme() {
        CodeStyleScheme s = schemes.get(activeSchemeName);
        if (s == null) {
            s = schemes.get("Default");
        }
        if (s == null && !schemes.isEmpty()) {
            s = schemes.values().iterator().next();
        }
        return s;
    }

    public void setActiveSchemeName(String name) {
        if (name != null && schemes.containsKey(name)) {
            this.activeSchemeName = name;
            Settings.put("codeStyle.activeScheme", name);
            notifyChanged();
        }
    }

    public CodeStyleScheme duplicateScheme(String sourceName, String newName) {
        CodeStyleScheme src = schemes.get(sourceName);
        if (src == null) src = getActiveScheme();
        CodeStyleScheme copy = src.copy(newName, false);
        schemes.put(newName, copy);
        setActiveSchemeName(newName);
        saveSettings();
        return copy;
    }

    public void removeScheme(String name) {
        if (!"Default".equals(name) && !"Project".equals(name)) {
            schemes.remove(name);
            if (activeSchemeName.equals(name)) {
                setActiveSchemeName("Default");
            }
            saveSettings();
        }
    }

    public void restoreDefaults() {
        CodeStyleScheme active = getActiveScheme();
        if (active != null) {
            CodeStyleScheme def = schemes.get("Default");
            if (def != null && def != active) {
                CodeStyleScheme fresh = def.copy(active.getName(), active.isProjectLevel());
                schemes.put(active.getName(), fresh);
            } else {
                active.setLineSeparator(LineSeparator.SYSTEM);
                active.setHardWrapAt(120);
                active.setWrapOnTyping(false);
                active.setVisualGuides("");
                active.setDetectAndUseExistingFileIndents(true);
                active.setEnableEditorConfigSupport(true);
                for (LanguageCodeStyleProvider p : LanguageCodeStyleProvider.getAllProviders()) {
                    active.setLanguageSettings(p.getLanguageId(), p.createDefaultSettings());
                }
            }
            saveSettings();
            notifyChanged();
        }
    }

    public void addChangeListener(Consumer<CodeStyleScheme> listener) {
        if (listener != null) changeListeners.add(listener);
    }

    public void removeChangeListener(Consumer<CodeStyleScheme> listener) {
        changeListeners.remove(listener);
    }

    private void notifyChanged() {
        CodeStyleScheme cur = getActiveScheme();
        for (Consumer<CodeStyleScheme> listener : changeListeners) {
            try {
                listener.accept(cur);
            } catch (Exception ignored) {}
        }
    }

    public void saveSettings() {
        Settings.put("codeStyle.activeScheme", activeSchemeName);
        CodeStyleScheme active = getActiveScheme();
        if (active != null) {
            Settings.put("codeStyle.lineSeparator", active.getLineSeparator().name());
            Settings.put("codeStyle.hardWrapAt", String.valueOf(active.getHardWrapAt()));
            Settings.put("codeStyle.wrapOnTyping", String.valueOf(active.isWrapOnTyping()));
            Settings.put("codeStyle.visualGuides", active.getVisualGuides());
            Settings.put("codeStyle.detectIndents", String.valueOf(active.isDetectAndUseExistingFileIndents()));
            Settings.put("codeStyle.editorConfig", String.valueOf(active.isEnableEditorConfigSupport()));

            for (Map.Entry<String, LanguageCodeStyleSettings> e : active.getAllLanguageSettings().entrySet()) {
                String prefix = "codeStyle.lang." + e.getKey() + ".";
                LanguageCodeStyleSettings l = e.getValue();
                Settings.put(prefix + "useTab", String.valueOf(l.isUseTabCharacter()));
                Settings.put(prefix + "smartTabs", String.valueOf(l.isSmartTabs()));
                Settings.put(prefix + "tabSize", String.valueOf(l.getTabSize()));
                Settings.put(prefix + "indent", String.valueOf(l.getIndent()));
                Settings.put(prefix + "continuationIndent", String.valueOf(l.getContinuationIndent()));
                Settings.put(prefix + "keepIndentsOnEmptyLines", String.valueOf(l.isKeepIndentsOnEmptyLines()));
                Settings.put(prefix + "labelIndent", String.valueOf(l.getLabelIndent()));
                Settings.put(prefix + "absoluteLabelIndent", String.valueOf(l.isAbsoluteLabelIndent()));
                Settings.put(prefix + "doNotIndentTopLevelMembers", String.valueOf(l.isDoNotIndentTopLevelMembers()));
                Settings.put(prefix + "useIndentsRelativeToExpressionStart", String.valueOf(l.isUseIndentsRelativeToExpressionStart()));

                for (Map.Entry<String, Object> pe : l.getAllProperties().entrySet()) {
                    if (pe.getValue() != null) {
                        Settings.put(prefix + "prop." + pe.getKey(), String.valueOf(pe.getValue()));
                    }
                }
            }
        }
        notifyChanged();
    }

    public void loadSettings() {
        String active = Settings.get("codeStyle.activeScheme");
        if (active != null && !active.isBlank() && schemes.containsKey(active)) {
            this.activeSchemeName = active;
        }

        CodeStyleScheme current = getActiveScheme();
        if (current != null) {
            String ls = Settings.get("codeStyle.lineSeparator");
            if (ls != null) {
                try {
                    current.setLineSeparator(LineSeparator.valueOf(ls));
                } catch (Exception ignored) {}
            }
            String hw = Settings.get("codeStyle.hardWrapAt");
            if (hw != null) {
                try { current.setHardWrapAt(Integer.parseInt(hw)); } catch (Exception ignored) {}
            }
            String wot = Settings.get("codeStyle.wrapOnTyping");
            if (wot != null) current.setWrapOnTyping(Boolean.parseBoolean(wot));
            String vg = Settings.get("codeStyle.visualGuides");
            if (vg != null) current.setVisualGuides(vg);
            String di = Settings.get("codeStyle.detectIndents");
            if (di != null) current.setDetectAndUseExistingFileIndents(Boolean.parseBoolean(di));
            String ec = Settings.get("codeStyle.editorConfig");
            if (ec != null) current.setEnableEditorConfigSupport(Boolean.parseBoolean(ec));

            for (LanguageCodeStyleProvider p : LanguageCodeStyleProvider.getAllProviders()) {
                String prefix = "codeStyle.lang." + p.getLanguageId() + ".";
                String tab = Settings.get(prefix + "tabSize");
                if (tab != null) {
                    LanguageCodeStyleSettings l = current.getLanguageSettings(p.getLanguageId());
                    try { l.setTabSize(Integer.parseInt(tab)); } catch (Exception ignored) {}
                    String ind = Settings.get(prefix + "indent");
                    if (ind != null) {
                        try { l.setIndent(Integer.parseInt(ind)); } catch (Exception ignored) {}
                    }
                    String cind = Settings.get(prefix + "continuationIndent");
                    if (cind != null) {
                        try { l.setContinuationIndent(Integer.parseInt(cind)); } catch (Exception ignored) {}
                    }
                    String useTab = Settings.get(prefix + "useTab");
                    if (useTab != null) l.setUseTabCharacter(Boolean.parseBoolean(useTab));
                    String smartTabs = Settings.get(prefix + "smartTabs");
                    if (smartTabs != null) l.setSmartTabs(Boolean.parseBoolean(smartTabs));
                    String keepEmpty = Settings.get(prefix + "keepIndentsOnEmptyLines");
                    if (keepEmpty != null) l.setKeepIndentsOnEmptyLines(Boolean.parseBoolean(keepEmpty));
                    String lblIndent = Settings.get(prefix + "labelIndent");
                    if (lblIndent != null) {
                        try { l.setLabelIndent(Integer.parseInt(lblIndent)); } catch (Exception ignored) {}
                    }
                    String absLbl = Settings.get(prefix + "absoluteLabelIndent");
                    if (absLbl != null) l.setAbsoluteLabelIndent(Boolean.parseBoolean(absLbl));
                    String noIndentTop = Settings.get(prefix + "doNotIndentTopLevelMembers");
                    if (noIndentTop != null) l.setDoNotIndentTopLevelMembers(Boolean.parseBoolean(noIndentTop));
                    String relExpr = Settings.get(prefix + "useIndentsRelativeToExpressionStart");
                    if (relExpr != null) l.setUseIndentsRelativeToExpressionStart(Boolean.parseBoolean(relExpr));

                    // Load dynamic properties declared by provider
                    for (String tabName : p.getSupportedTabs()) {
                        List<CodeStyleGroup> groups = p.getOptionGroups(tabName);
                        for (CodeStyleGroup g : groups) {
                            if (g.getHeaderComboKey() != null) {
                                String val = Settings.get(prefix + "prop." + g.getHeaderComboKey());
                                if (val != null) l.setProperty(g.getHeaderComboKey(), val);
                            }
                            for (CodeStyleOption opt : g.getOptions()) {
                                if (opt.getKey() != null) {
                                    String val = Settings.get(prefix + "prop." + opt.getKey());
                                    if (val != null) {
                                        if (opt.getType() == CodeStyleOptionType.CHECKBOX) {
                                            l.setBoolean(opt.getKey(), Boolean.parseBoolean(val));
                                        } else if (opt.getType() == CodeStyleOptionType.NUMBER) {
                                            try { l.setInt(opt.getKey(), Integer.parseInt(val)); } catch (Exception ignored) {}
                                        } else {
                                            l.setString(opt.getKey(), val);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public String exportToXml(CodeStyleScheme scheme) {
        StringBuilder sb = new StringBuilder();
        sb.append("<code_scheme name=\"").append(scheme.getName()).append("\" version=\"173\">\n");
        sb.append("  <option name=\"LINE_SEPARATOR\" value=\"").append(scheme.getLineSeparator().name()).append("\" />\n");
        sb.append("  <option name=\"RIGHT_MARGIN\" value=\"").append(scheme.getHardWrapAt()).append("\" />\n");
        sb.append("  <option name=\"WRAP_WHEN_TYPING_REACHES_RIGHT_MARGIN\" value=\"").append(scheme.isWrapOnTyping()).append("\" />\n");
        sb.append("  <option name=\"FORMATTER_TAGS_ENABLED\" value=\"").append(scheme.isEnableFormatterMarkers()).append("\" />\n");
        for (Map.Entry<String, LanguageCodeStyleSettings> e : scheme.getAllLanguageSettings().entrySet()) {
            LanguageCodeStyleSettings s = e.getValue();
            sb.append("  <codeStyleSettings language=\"").append(e.getKey()).append("\">\n");
            sb.append("    <indentOptions>\n");
            sb.append("      <option name=\"INDENT_SIZE\" value=\"").append(s.getIndent()).append("\" />\n");
            sb.append("      <option name=\"CONTINUATION_INDENT_SIZE\" value=\"").append(s.getContinuationIndent()).append("\" />\n");
            sb.append("      <option name=\"TAB_SIZE\" value=\"").append(s.getTabSize()).append("\" />\n");
            sb.append("      <option name=\"USE_TAB_CHARACTER\" value=\"").append(s.isUseTabCharacter()).append("\" />\n");
            sb.append("      <option name=\"SMART_TABS\" value=\"").append(s.isSmartTabs()).append("\" />\n");
            sb.append("      <option name=\"LABEL_INDENT_SIZE\" value=\"").append(s.getLabelIndent()).append("\" />\n");
            sb.append("      <option name=\"LABEL_INDENT_ABSOLUTE\" value=\"").append(s.isAbsoluteLabelIndent()).append("\" />\n");
            sb.append("      <option name=\"DO_NOT_INDENT_TOP_LEVEL_CLASS_MEMBERS\" value=\"").append(s.isDoNotIndentTopLevelMembers()).append("\" />\n");
            sb.append("      <option name=\"KEEP_INDENTS_ON_EMPTY_LINES\" value=\"").append(s.isKeepIndentsOnEmptyLines()).append("\" />\n");
            sb.append("    </indentOptions>\n");
            sb.append("  </codeStyleSettings>\n");
        }
        sb.append("</code_scheme>\n");
        return sb.toString();
    }

    public String exportToEditorConfig(CodeStyleScheme scheme) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Generated by Lumina Code Style\n");
        sb.append("root = true\n\n");
        sb.append("[*]\n");
        sb.append("end_of_line = ").append(switch (scheme.getLineSeparator()) {
            case UNIX -> "lf";
            case WINDOWS -> "crlf";
            case MAC_CLASSIC -> "cr";
            case SYSTEM -> System.lineSeparator().equals("\r\n") ? "crlf" : "lf";
        }).append("\n");
        sb.append("max_line_length = ").append(scheme.getHardWrapAt()).append("\n");
        sb.append("charset = utf-8\n\n");

        for (Map.Entry<String, LanguageCodeStyleSettings> e : scheme.getAllLanguageSettings().entrySet()) {
            LanguageCodeStyleSettings s = e.getValue();
            String pattern = switch (e.getKey()) {
                case "Java" -> "[*.java]";
                case "Kotlin" -> "[*.{kt,kts}]";
                case "Python" -> "[*.py]";
                case "JavaScript" -> "[*.{js,mjs,cjs}]";
                case "TypeScript" -> "[*.{ts,tsx}]";
                case "HTML" -> "[*.{html,htm}]";
                case "JSON" -> "[*.json]";
                case "YAML" -> "[*.{yaml,yml}]";
                case "Rust" -> "[*.rs]";
                case "Go" -> "[*.go]";
                default -> null;
            };
            if (pattern != null) {
                sb.append(pattern).append("\n");
                sb.append("indent_style = ").append(s.isUseTabCharacter() ? "tab" : "space").append("\n");
                sb.append("indent_size = ").append(s.getIndent()).append("\n");
                sb.append("tab_width = ").append(s.getTabSize()).append("\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * Initializes all dynamic language code style providers.
     */
    private static void initProviders() {
        // 1. Java
        LanguageCodeStyleProvider.register(new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() { return "Java"; }
            @Override
            public String getDisplayName() { return "Java"; }
            @Override
            public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines", "JavaDoc", "Imports", "Arrangement", "Code Generation", "Java EE Names");
            }
            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                LanguageCodeStyleSettings s = new LanguageCodeStyleSettings("Java");
                s.setTabSize(4);
                s.setIndent(4);
                s.setContinuationIndent(8);
                s.setUseTabCharacter(false);
                s.setLabelIndent(0);
                return s;
            }
            @Override
            public String getSampleCode() {
                return """
public class Foo {
    public int[] X = new int[]{1, 3, 5, 7, 9, 11};

    public void foo(boolean a, int x, int y, int z) {
    label1:
        do {
            try {
                if (x > 0) {
                    int someVariable = a ? x : y;
                    int anotherVariable = a ? x : y;
                } else if (x < 0) {
                    int someVariable = (y + z);
                    someVariable = x = x + y;
                } else {
                label2:
                    for (int i = 0; i < 5; i++) doSomething(i);
                }
                switch (a) {
                    case 0:
                        doCase0();
                        break;
                    default:
                        doDefault();
                }
            } catch (Exception e) {
                processException(e.getMessage(), x + y, z, a);
            } finally {
                processFinally();
            }
        }
        while (true);

        if (2 < 3) return;
        if (3 < 4) return;
        do {
            x++;
        }
        while (x < 10000);
        while (x < 50000) x++;
        for (int i = 0; i < 5; i++) System.out.println(i);
    }

    private class InnerClass implements I1, I2 {
        public void bar() throws E1, E2 {
        }
    }
}
""";
            }
        });

        // 2. Kotlin
        LanguageCodeStyleProvider.register(KotlinCodeStyleSettings.createProvider());

        // 3. Angular HTML template
        LanguageCodeStyleProvider.register(AngularHtmlCodeStyleSettings.createProvider());

        // 4. EditorConfig
        LanguageCodeStyleProvider.register(EditorConfigCodeStyleSettings.createProvider());

        // 5. ERB
        registerSimpleProvider("ERB", "ERB", 2, 2, 4, """
<% if @user.admin? %>
  <div class="admin-panel">
    <h1>Welcome, <%= @user.name %></h1>
    <% @projects.each do |project| %>
      <p><%= project.title %></p>
    <% end %>
  </div>
<% end %>
""");

        // 6. Go
        LanguageCodeStyleProvider.register(new LanguageCodeStyleProvider() {
            @Override public String getLanguageId() { return "Go"; }
            @Override public String getDisplayName() { return "Go"; }
            @Override public List<String> getSupportedTabs() { return List.of("Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines", "Imports"); }
            @Override public LanguageCodeStyleSettings createDefaultSettings() {
                LanguageCodeStyleSettings s = new LanguageCodeStyleSettings("Go");
                s.setUseTabCharacter(true);
                s.setTabSize(4);
                s.setIndent(4);
                s.setContinuationIndent(8);
                return s;
            }
            @Override public String getSampleCode() {
                return """
package main

import (
\t"fmt"
\t"net/http"
)

type Server struct {
\tPort int
\tHost string
}

func (s *Server) Start() error {
\thttp.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
\t\tfmt.Fprintf(w, "Hello, Lumina!")
\t})
\treturn http.ListenAndServe(fmt.Sprintf("%s:%d", s.Host, s.Port), nil)
}
""";
            }
        });

        // 7. Gradle Declarative Configuration
        registerSimpleProvider("Gradle Declarative Configuration", "Gradle Declarative Configuration", 4, 4, 8, """
plugins {
    id("java")
    id("application")
}

application {
    mainClass.set("dev.lumina.Main")
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.9")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}
""");

        // 8. Groovy
        registerSimpleProvider("Groovy", "Groovy", 4, 4, 8, """
class Person {
    String name
    int age

    def greet() {
        def greeting = "Hello, ${name}"
        println greeting
    }
}
""");

        // 9. HTML
        registerSimpleProvider("HTML", "HTML", 2, 2, 4, """
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8">
    <title>Sample Application</title>
  </head>
  <body>
    <header class="app-header">
      <nav>
        <ul>
          <li><a href="/">Home</a></li>
          <li><a href="/settings">Settings</a></li>
        </ul>
      </nav>
    </header>
  </body>
</html>
""");

        // 10. HTTP Request
        registerSimpleProvider("HTTP Request", "HTTP Request", 2, 2, 4, """
### Get User Profile
GET https://api.example.com/v1/users/42
Authorization: Bearer {{auth_token}}
Accept: application/json

### Update Settings
POST https://api.example.com/v1/settings
Content-Type: application/json

{
  "theme": "Dark",
  "autoSave": true
}
""");

        // 11. JavaScript
        registerSimpleProvider("JavaScript", "JavaScript", 2, 2, 4, """
function calculateTotal(items, discountRate = 0) {
  let subtotal = 0;
  for (const item of items) {
    if (item.available) {
      subtotal += item.price * item.quantity;
    }
  }
  const discount = subtotal * discountRate;
  return subtotal - discount;
}
""");

        // 12. JSON
        registerSimpleProvider("JSON", "JSON", 2, 2, 4, """
{
  "name": "lumina-ide",
  "version": "0.1.0",
  "private": true,
  "dependencies": {
    "core": "^1.0.0",
    "parser": "^2.1.0"
  },
  "scripts": {
    "build": "mvn compile",
    "test": "mvn test"
  }
}
""");

        // 13. JSP
        registerSimpleProvider("JSP", "JSP", 2, 2, 4, """
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
  <body>
    <h2>User Dashboard</h2>
    <%
      String username = (String) session.getAttribute("user");
      if (username != null) {
    %>
      <p>Welcome back, <%= username %>!</p>
    <% } %>
  </body>
</html>
""");

        // 14. JSPX
        registerSimpleProvider("JSPX", "JSPX", 2, 2, 4, """
<jsp:root xmlns:jsp="http://java.sun.com/JSP/Page" version="2.0">
  <jsp:directive.page contentType="text/html;charset=UTF-8"/>
  <html>
    <body>
      <jsp:element name="title">
        <jsp:body>Lumina Portal</jsp:body>
      </jsp:element>
    </body>
  </html>
</jsp:root>
""");

        // 15. Markdown
        registerSimpleProvider("Markdown", "Markdown", 2, 2, 4, """
# Project Documentation

Lumina is a next-generation high performance IDE.

## Features
- Dynamic Code Style configuration
- Real-time syntax and formatting preview
- Zero-hardcoding architecture
""");

        // 16. PHP
        registerSimpleProvider("PHP", "PHP", 4, 4, 8, """
<?php

namespace App\\Services;

class UserService
{
    private Database $db;

    public function __construct(Database $db)
    {
        $this->db = $db;
    }

    public function findById(int $id): ?User
    {
        return $this->db->query("SELECT * FROM users WHERE id = :id", ['id' => $id]);
    }
}
""");

        // 17. Properties
        registerSimpleProvider("Properties", "Properties", 4, 4, 8, """
app.name=Lumina IDE
app.version=0.1.0
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/app
spring.datasource.username=postgres
""");

        // 18. Protocol Buffer
        registerSimpleProvider("Protocol Buffer", "Protocol Buffer", 2, 2, 4, """
syntax = "proto3";

package lumina.rpc;

message UserRequest {
  int64 id = 1;
  string query = 2;
}

message UserResponse {
  int64 id = 1;
  string username = 2;
  repeated string roles = 3;
}
""");

        // 19. Protocol Buffer Text
        registerSimpleProvider("Protocol Buffer Text", "Protocol Buffer Text", 2, 2, 4, """
id: 1001
query: "status"
timestamp {
  seconds: 1727481600
}
""");

        // 20. Python
        registerSimpleProvider("Python", "Python", 4, 4, 8, """
class DataProcessor:
    def __init__(self, batch_size: int = 64):
        self.batch_size = batch_size
        self._cache = {}

    def process_records(self, records: list[dict]) -> int:
        count = 0
        for record in records:
            if record.get("active"):
                self._cache[record["id"]] = record["value"]
                count += 1
        return count
""");

        // 21. Qute
        registerSimpleProvider("Qute", "Qute", 2, 2, 4, """
{#include base}
  {#title}User Profile{/title}
  {#body}
    <h1>{user.name}</h1>
    <ul>
      {#for order in user.orders}
        <li>Order #{order.id}: {order.total}</li>
      {/for}
    </ul>
  {/body}
{/include}
""");

        // 22. Ruby
        registerSimpleProvider("Ruby", "Ruby", 2, 2, 4, """
class OrderController < ApplicationController
  before_action :authenticate_user!

  def create
    @order = current_user.orders.build(order_params)
    if @order.save
      render json: @order, status: :created
    else
      render json: @order.errors, status: :unprocessable_entity
    end
  end
end
""");

        // 23. Rust
        registerSimpleProvider("Rust", "Rust", 4, 4, 8, """
pub struct Point {
    pub x: f64,
    pub y: f64,
}

impl Point {
    pub fn distance(&self, other: &Point) -> f64 {
        let dx = self.x - other.x;
        let dy = self.y - other.y;
        (dx * dx + dy * dy).sqrt()
    }
}
""");

        // 24. Scala
        registerSimpleProvider("Scala", "Scala", 2, 2, 4, """
case class Employee(id: Long, name: String, department: String)

object EmployeeDirectory {
  def filterByDept(list: List[Employee], dept: String): List[Employee] = {
    list.filter(_.department == dept)
  }
}
""");

        // 25. Shell Script
        registerSimpleProvider("Shell Script", "Shell Script", 4, 4, 8, """
#!/usr/bin/env bash
set -euo pipefail

deploy_service() {
    local target="$1"
    echo "Deploying to: ${target}"
    if [[ -d "${target}" ]]; then
        rsync -avz --delete dist/ "${target}/"
    fi
}
""");

        // 26. SQL
        registerSimpleProvider("SQL", "SQL", 4, 4, 8, """
SELECT
    u.id,
    u.username,
    COUNT(o.id) AS total_orders,
    SUM(o.amount) AS total_spent
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
WHERE u.active = true
GROUP BY u.id, u.username
HAVING COUNT(o.id) > 5
ORDER BY total_spent DESC;
""");

        // 27. TOML
        registerSimpleProvider("TOML", "TOML", 2, 2, 4, """
[package]
name = "lumina-core"
version = "0.1.0"
edition = "2024"

[dependencies]
serde = { version = "1.0", features = ["derive"] }
tokio = { version = "1.36", features = ["full"] }
""");

        // 28. TypeScript
        registerSimpleProvider("TypeScript", "TypeScript", 2, 2, 4, """
export interface UserRecord {
  id: string;
  email: string;
  roles: string[];
}

export async function fetchUser(userId: string): Promise<UserRecord> {
  const response = await fetch(`/api/users/${userId}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch user: ${response.statusText}`);
  }
  return response.json();
}
""");

        // 29. XML
        registerSimpleProvider("XML", "XML", 2, 2, 4, """
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>dev.lumina</groupId>
  <artifactId>lumina-ide</artifactId>
  <version>0.1.0</version>
</project>
""");

        // 30. YAML
        registerSimpleProvider("YAML", "YAML", 2, 2, 4, """
version: '3.8'
services:
  app:
    image: lumina:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=production
""");
    }

    private static void registerSimpleProvider(String id, String displayName, int tabSize, int indent, int contIndent, String sample) {
        LanguageCodeStyleProvider.register(new LanguageCodeStyleProvider() {
            @Override public String getLanguageId() { return id; }
            @Override public String getDisplayName() { return displayName; }
            @Override public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents", "Spaces", "Wrapping and Braces", "Blank Lines", "Code Generation");
            }
            @Override public LanguageCodeStyleSettings createDefaultSettings() {
                LanguageCodeStyleSettings s = new LanguageCodeStyleSettings(id);
                s.setTabSize(tabSize);
                s.setIndent(indent);
                s.setContinuationIndent(contIndent);
                s.setUseTabCharacter(false);
                return s;
            }
            @Override public String getSampleCode() { return sample; }
        });
    }

    /**
     * Formats code sample live based on active LanguageCodeStyleSettings.
     * Computes indentation, tabs, and label adjustments.
     */
    public static String formatCodeSample(String sample, LanguageCodeStyleSettings settings) {
        if (sample == null || sample.isBlank() || settings == null) {
            return sample;
        }

        int targetIndent = settings.getIndent();
        int baseIndentUnit = 4; // All sample snippets are written with 4 spaces per standard indent level
        if (targetIndent <= 0) targetIndent = 4;

        String[] lines = sample.split("\r?\n", -1);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int leadingSpaces = 0;
            while (leadingSpaces < line.length() && (line.charAt(leadingSpaces) == ' ' || line.charAt(leadingSpaces) == '\t')) {
                if (line.charAt(leadingSpaces) == '\t') leadingSpaces += 4;
                else leadingSpaces++;
            }

            String content = line.trim();
            if (content.isEmpty()) {
                if (settings.isKeepIndentsOnEmptyLines()) {
                    result.append(buildIndentString(leadingSpaces, settings));
                }
                result.append("\n");
                continue;
            }

            // Check if label (e.g. label1:)
            if (content.endsWith(":") && !content.startsWith("//") && !content.contains(" ") && !content.equals("default:")) {
                int labelSpaces = settings.isAbsoluteLabelIndent()
                        ? settings.getLabelIndent()
                        : Math.max(0, leadingSpaces + settings.getLabelIndent());
                result.append(buildIndentString(labelSpaces, settings)).append(content).append("\n");
                continue;
            }

            // Check top level class members indent
            if (settings.isDoNotIndentTopLevelMembers() && leadingSpaces == baseIndentUnit) {
                result.append(content).append("\n");
                continue;
            }

            // Scale indent levels
            int indentLevel = leadingSpaces / baseIndentUnit;
            int remainder = leadingSpaces % baseIndentUnit;
            int newSpaces = (indentLevel * targetIndent) + remainder;

            result.append(buildIndentString(newSpaces, settings)).append(content).append("\n");
        }

        return result.toString();
    }

    private static String buildIndentString(int spaces, LanguageCodeStyleSettings settings) {
        if (spaces <= 0) return "";
        if (settings.isUseTabCharacter()) {
            int tabSize = settings.getTabSize() > 0 ? settings.getTabSize() : 4;
            int tabs = spaces / tabSize;
            int rem = spaces % tabSize;
            return "\t".repeat(tabs) + " ".repeat(rem);
        }
        return " ".repeat(spaces);
    }
}
