package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.List;

/**
 * Dedicated code style settings and dynamic provider for EditorConfig.
 * Strictly decoupled and brand-isolated.
 * Supports the 2 tabs matching reference screenshots:
 * 1. Spaces (media_1790574580900.png)
 * 2. Wrapping and Braces (media_1790574588939.png)
 */
public class EditorConfigCodeStyleSettings extends LanguageCodeStyleSettings {

    // ==========================================
    // Property Keys - Spaces
    // ==========================================
    public static final String SPACES_AROUND_SEPARATOR = "editorconfig_spaces_around_separator";
    public static final String SPACES_BEFORE_COLON = "editorconfig_spaces_before_colon";
    public static final String SPACES_AFTER_COLON = "editorconfig_spaces_after_colon";
    public static final String SPACES_BEFORE_COMMA = "editorconfig_spaces_before_comma";
    public static final String SPACES_AFTER_COMMA = "editorconfig_spaces_after_comma";

    // ==========================================
    // Property Keys - Wrapping and Braces
    // ==========================================
    public static final String WRAP_VISUAL_GUIDES = "editorconfig_wrap_visual_guides";
    public static final String WRAP_ALIGN_FIELDS_IN_COLUMNS = "editorconfig_wrap_align_fields_in_columns";

    public EditorConfigCodeStyleSettings() {
        this("EditorConfig");
    }

    public EditorConfigCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        setTabSize(4);
        setIndent(4);
        setContinuationIndent(8);
        setUseTabCharacter(false);

        // Spaces
        setBoolean(SPACES_AROUND_SEPARATOR, true);
        setBoolean(SPACES_BEFORE_COLON, false);
        setBoolean(SPACES_AFTER_COLON, false);
        setBoolean(SPACES_BEFORE_COMMA, false);
        setBoolean(SPACES_AFTER_COMMA, true);

        // Wrapping and Braces
        setString(WRAP_VISUAL_GUIDES, "Default: None");
        setBoolean(WRAP_ALIGN_FIELDS_IN_COLUMNS, false);
    }

    @Override
    public EditorConfigCodeStyleSettings copy() {
        EditorConfigCodeStyleSettings copy = new EditorConfigCodeStyleSettings(getLanguageId());
        copy.setTabSize(getTabSize());
        copy.setIndent(getIndent());
        copy.setContinuationIndent(getContinuationIndent());
        copy.setUseTabCharacter(isUseTabCharacter());
        copy.setProperties(getAllProperties());
        return copy;
    }

    public boolean isSpacesAroundSeparator() {
        return getBoolean(SPACES_AROUND_SEPARATOR, true);
    }

    public void setSpacesAroundSeparator(boolean v) {
        setBoolean(SPACES_AROUND_SEPARATOR, v);
    }

    public boolean isSpacesBeforeColon() {
        return getBoolean(SPACES_BEFORE_COLON, false);
    }

    public void setSpacesBeforeColon(boolean v) {
        setBoolean(SPACES_BEFORE_COLON, v);
    }

    public boolean isSpacesAfterColon() {
        return getBoolean(SPACES_AFTER_COLON, false);
    }

    public void setSpacesAfterColon(boolean v) {
        setBoolean(SPACES_AFTER_COLON, v);
    }

    public boolean isSpacesBeforeComma() {
        return getBoolean(SPACES_BEFORE_COMMA, false);
    }

    public void setSpacesBeforeComma(boolean v) {
        setBoolean(SPACES_BEFORE_COMMA, v);
    }

    public boolean isSpacesAfterComma() {
        return getBoolean(SPACES_AFTER_COMMA, true);
    }

    public void setSpacesAfterComma(boolean v) {
        setBoolean(SPACES_AFTER_COMMA, v);
    }

    public String getVisualGuides() {
        return getString(WRAP_VISUAL_GUIDES, "Default: None");
    }

    public void setVisualGuides(String v) {
        setString(WRAP_VISUAL_GUIDES, v);
    }

    public boolean isAlignFieldsInColumns() {
        return getBoolean(WRAP_ALIGN_FIELDS_IN_COLUMNS, false);
    }

    public void setAlignFieldsInColumns(boolean v) {
        setBoolean(WRAP_ALIGN_FIELDS_IN_COLUMNS, v);
    }

    // ==========================================
    // Sample Code matching media_1790574580900.png & media_1790574588939.png verbatim
    // ==========================================
    public static final String SAMPLE_EDITORCONFIG = """
root = true

[foo]
charset = utf-8
key = value1, value2, value3
key2 = value4:value5
; another comment
""";

    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "EditorConfig";
            }

            @Override
            public String getDisplayName() {
                return "EditorConfig";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Spaces", "Wrapping and Braces");
            }

            @Override
            public boolean hasPreview(String tabName) {
                return true;
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new EditorConfigCodeStyleSettings("EditorConfig");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_EDITORCONFIG;
            }

            @Override
            public String getSampleCode(String tabName) {
                return SAMPLE_EDITORCONFIG;
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Spaces".equals(tabName)) {
                    CodeStyleGroup aroundOps = CodeStyleGroup.collapsible("Around operators");
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACES_AROUND_SEPARATOR, "Around separator", true));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACES_BEFORE_COLON, "Before ':'", false));
                    aroundOps.addOption(CodeStyleOption.checkbox(SPACES_AFTER_COLON, "After ':'", false));
                    customizer.addGroup(aroundOps);

                    CodeStyleGroup other = CodeStyleGroup.collapsible("Other");
                    other.addOption(CodeStyleOption.checkbox(SPACES_BEFORE_COMMA, "Before comma", false));
                    other.addOption(CodeStyleOption.checkbox(SPACES_AFTER_COMMA, "After comma", true));
                    customizer.addGroup(other);

                } else if ("Wrapping and Braces".equals(tabName)) {
                    CodeStyleGroup general = CodeStyleGroup.flat("General");
                    general.addOption(CodeStyleOption.combo(
                            WRAP_VISUAL_GUIDES,
                            "Visual guides",
                            List.of("Default: None", "None", "80", "120"),
                            "Default: None"
                    ));
                    customizer.addGroup(general);

                    CodeStyleGroup groupDecl = CodeStyleGroup.collapsible("Group declarations");
                    groupDecl.addOption(CodeStyleOption.rightAlignedCheckbox(
                            WRAP_ALIGN_FIELDS_IN_COLUMNS,
                            "Align fields in columns",
                            false
                    ));
                    customizer.addGroup(groupDecl);
                }
            }
        };
    }
}
