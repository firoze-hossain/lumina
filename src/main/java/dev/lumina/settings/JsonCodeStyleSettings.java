package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.List;

/**
 * Dedicated code style settings and dynamic provider for JSON.
 * Strictly decoupled, dynamically configured without hardcoding, and brand-isolated.
 * Supports all 4 tabs matching reference screenshots:
 * 1. Tabs and Indents (media_1790604939250.png)
 * 2. Spaces (media_1790604950902.png)
 * 3. Blank Lines (media_1790604967970.png)
 * 4. Wrapping and Braces (media_1790604979247.png)
 */
public class JsonCodeStyleSettings extends LanguageCodeStyleSettings {

    // ==========================================
    // Option Choice Lists
    // ==========================================
    public static final List<String> WRAP_OPTIONS = List.of(
            "Do not wrap",
            "Wrap if long",
            "Chop down if long",
            "Wrap always"
    );

    public static final List<String> ALIGN_OPTIONS = List.of(
            "Do not align",
            "On colon",
            "On value"
    );

    public static final List<String> WRAP_ON_TYPING_OPTIONS = List.of(
            "Default: No",
            "Yes",
            "No"
    );

    public static final List<String> VISUAL_GUIDES_OPTIONS = List.of(
            "Default: None",
            "None",
            "80",
            "120"
    );

    // ==========================================
    // Property Keys - Spaces (media_1790604950902.png)
    // ==========================================
    public static final String SPACE_WITHIN_BRACES = "json_space_within_braces";
    public static final String SPACE_WITHIN_BRACKETS = "json_space_within_brackets";
    public static final String SPACE_BEFORE_COMMA = "json_space_before_comma";
    public static final String SPACE_AFTER_COMMA = "json_space_after_comma";
    public static final String SPACE_BEFORE_COLON = "json_space_before_colon";
    public static final String SPACE_AFTER_COLON = "json_space_after_colon";

    // ==========================================
    // Property Keys - Blank Lines (media_1790604967970.png)
    // ==========================================
    public static final String BLANK_LINES_KEEP_IN_CODE = "json_blank_lines_keep_in_code";

    // ==========================================
    // Property Keys - Wrapping and Braces (media_1790604979247.png)
    // ==========================================
    public static final String HARD_WRAP_AT = "json_hard_wrap_at";
    public static final String WRAP_ON_TYPING = "json_wrap_on_typing";
    public static final String VISUAL_GUIDES = "json_visual_guides";
    public static final String KEEP_LINE_BREAKS = "json_keep_line_breaks";
    public static final String KEEP_TRAILING_COMMA = "json_keep_trailing_comma";
    public static final String WRAP_LONG_LINES = "json_wrap_long_lines";
    public static final String ARRAY_WRAPPING = "json_array_wrapping";
    public static final String OBJECT_WRAPPING = "json_object_wrapping";
    public static final String OBJECT_ALIGN = "json_object_align";

    public JsonCodeStyleSettings() {
        this("JSON");
    }

    public JsonCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        // Tab 1: Tabs and Indents (media_1790604939250.png)
        setTabSize(4);
        setIndent(2);
        setContinuationIndent(8);
        setUseTabCharacter(false);
        setSmartTabs(false);
        setKeepIndentsOnEmptyLines(false);

        // Tab 2: Spaces (media_1790604950902.png)
        setBoolean(SPACE_WITHIN_BRACES, false);
        setBoolean(SPACE_WITHIN_BRACKETS, false);
        setBoolean(SPACE_BEFORE_COMMA, false);
        setBoolean(SPACE_AFTER_COMMA, true);
        setBoolean(SPACE_BEFORE_COLON, false);
        setBoolean(SPACE_AFTER_COLON, true);

        // Tab 3: Blank Lines (media_1790604967970.png)
        setInt(BLANK_LINES_KEEP_IN_CODE, 0);

        // Tab 4: Wrapping and Braces (media_1790604979247.png)
        setInt(HARD_WRAP_AT, 120);
        setString(WRAP_ON_TYPING, "Default: No");
        setString(VISUAL_GUIDES, "Default: None");
        setBoolean(KEEP_LINE_BREAKS, true);
        setBoolean(KEEP_TRAILING_COMMA, false);
        setBoolean(WRAP_LONG_LINES, false);
        setString(ARRAY_WRAPPING, "Wrap always");
        setString(OBJECT_WRAPPING, "Wrap always");
        setString(OBJECT_ALIGN, "Do not align");
    }

    @Override
    public JsonCodeStyleSettings copy() {
        JsonCodeStyleSettings copy = new JsonCodeStyleSettings(getLanguageId());
        copy.setTabSize(getTabSize());
        copy.setIndent(getIndent());
        copy.setContinuationIndent(getContinuationIndent());
        copy.setUseTabCharacter(isUseTabCharacter());
        copy.setSmartTabs(isSmartTabs());
        copy.setKeepIndentsOnEmptyLines(isKeepIndentsOnEmptyLines());
        copy.setProperties(getAllProperties());
        return copy;
    }

    // ==========================================
    // Getters and Setters - Spaces
    // ==========================================

    public boolean isSpaceWithinBraces() {
        return getBoolean(SPACE_WITHIN_BRACES, false);
    }

    public void setSpaceWithinBraces(boolean val) {
        setBoolean(SPACE_WITHIN_BRACES, val);
    }

    public boolean isSpaceWithinBrackets() {
        return getBoolean(SPACE_WITHIN_BRACKETS, false);
    }

    public void setSpaceWithinBrackets(boolean val) {
        setBoolean(SPACE_WITHIN_BRACKETS, val);
    }

    public boolean isSpaceBeforeComma() {
        return getBoolean(SPACE_BEFORE_COMMA, false);
    }

    public void setSpaceBeforeComma(boolean val) {
        setBoolean(SPACE_BEFORE_COMMA, val);
    }

    public boolean isSpaceAfterComma() {
        return getBoolean(SPACE_AFTER_COMMA, true);
    }

    public void setSpaceAfterComma(boolean val) {
        setBoolean(SPACE_AFTER_COMMA, val);
    }

    public boolean isSpaceBeforeColon() {
        return getBoolean(SPACE_BEFORE_COLON, false);
    }

    public void setSpaceBeforeColon(boolean val) {
        setBoolean(SPACE_BEFORE_COLON, val);
    }

    public boolean isSpaceAfterColon() {
        return getBoolean(SPACE_AFTER_COLON, true);
    }

    public void setSpaceAfterColon(boolean val) {
        setBoolean(SPACE_AFTER_COLON, val);
    }

    // ==========================================
    // Getters and Setters - Blank Lines
    // ==========================================

    public int getBlankLinesKeepInCode() {
        return getInt(BLANK_LINES_KEEP_IN_CODE, 0);
    }

    public void setBlankLinesKeepInCode(int val) {
        setInt(BLANK_LINES_KEEP_IN_CODE, val);
    }

    // ==========================================
    // Getters and Setters - Wrapping and Braces
    // ==========================================

    public int getHardWrapAt() {
        return getInt(HARD_WRAP_AT, 120);
    }

    public void setHardWrapAt(int val) {
        setInt(HARD_WRAP_AT, val);
    }

    public String getWrapOnTyping() {
        return getString(WRAP_ON_TYPING, "Default: No");
    }

    public void setWrapOnTyping(String val) {
        setString(WRAP_ON_TYPING, val);
    }

    public String getVisualGuides() {
        return getString(VISUAL_GUIDES, "Default: None");
    }

    public void setVisualGuides(String val) {
        setString(VISUAL_GUIDES, val);
    }

    public boolean isKeepLineBreaks() {
        return getBoolean(KEEP_LINE_BREAKS, true);
    }

    public void setKeepLineBreaks(boolean val) {
        setBoolean(KEEP_LINE_BREAKS, val);
    }

    public boolean isKeepTrailingComma() {
        return getBoolean(KEEP_TRAILING_COMMA, false);
    }

    public void setKeepTrailingComma(boolean val) {
        setBoolean(KEEP_TRAILING_COMMA, val);
    }

    public boolean isWrapLongLines() {
        return getBoolean(WRAP_LONG_LINES, false);
    }

    public void setWrapLongLines(boolean val) {
        setBoolean(WRAP_LONG_LINES, val);
    }

    public String getArrayWrapping() {
        return getString(ARRAY_WRAPPING, "Wrap always");
    }

    public void setArrayWrapping(String val) {
        setString(ARRAY_WRAPPING, val);
    }

    public String getObjectWrapping() {
        return getString(OBJECT_WRAPPING, "Wrap always");
    }

    public void setObjectWrapping(String val) {
        setString(OBJECT_WRAPPING, val);
    }

    public String getObjectAlign() {
        return getString(OBJECT_ALIGN, "Do not align");
    }

    public void setObjectAlign(String val) {
        setString(OBJECT_ALIGN, val);
    }

    // ==========================================
    // Sample Code matching media_1790604939250.png, media_1790604950902.png,
    // media_1790604967970.png, media_1790604979247.png
    // ==========================================
    public static final String SAMPLE_CODE = """
{
  "json literals are": {
    "strings": [
      "foo",
      "bar",
      "\\u0062\\u0061\\u0072"
    ],
    "numbers": [
      42,
      6.62606975e-34
    ],
    "boolean values": [
      true,
      false
    ],
    "objects": {
      "null": null,
      "another": null
    }
  }
}
""";

    /**
     * Creates and registers the dynamic LanguageCodeStyleProvider for JSON.
     */
    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "JSON";
            }

            @Override
            public String getDisplayName() {
                return "JSON";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents", "Spaces", "Blank Lines", "Wrapping and Braces");
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new JsonCodeStyleSettings("JSON");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_CODE;
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Tabs and Indents".equals(tabName)) {
                    CodeStyleGroup indents = CodeStyleGroup.flat("Tabs and Indents");
                    indents.addOption(CodeStyleOption.checkbox("use_tab_character", "Use tab character", false));
                    indents.addOption(CodeStyleOption.indentedCheckbox("smart_tabs", "Smart tabs", false));
                    indents.addOption(CodeStyleOption.number("tab_size", "Tab size:", 4));
                    indents.addOption(CodeStyleOption.number("indent", "Indent:", 2));
                    indents.addOption(CodeStyleOption.number("continuation_indent", "Continuation indent:", 8));
                    indents.addOption(CodeStyleOption.checkbox("keep_indents_on_empty_lines", "Keep indents on empty lines", false));
                    customizer.addGroup(indents);

                } else if ("Spaces".equals(tabName)) {
                    // Group 1: Within (media_1790604950902.png)
                    CodeStyleGroup within = CodeStyleGroup.collapsible("Within");
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_BRACES, "Braces", false));
                    within.addOption(CodeStyleOption.checkbox(SPACE_WITHIN_BRACKETS, "Brackets", false));
                    customizer.addGroup(within);

                    // Group 2: Other (media_1790604950902.png)
                    CodeStyleGroup other = CodeStyleGroup.collapsible("Other");
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_COMMA, "Before comma", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_COMMA, "After comma", true));
                    other.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_COLON, "Before ':'", false));
                    other.addOption(CodeStyleOption.checkbox(SPACE_AFTER_COLON, "After ':'", true));
                    customizer.addGroup(other);

                } else if ("Blank Lines".equals(tabName)) {
                    // Group: Keep maximum blank lines (media_1790604967970.png)
                    CodeStyleGroup blank = CodeStyleGroup.divider("Keep maximum blank lines");
                    blank.addOption(CodeStyleOption.number(BLANK_LINES_KEEP_IN_CODE, "In code:", 0));
                    customizer.addGroup(blank);

                } else if ("Wrapping and Braces".equals(tabName)) {
                    // General / Top-level options (media_1790604979247.png)
                    CodeStyleGroup general = CodeStyleGroup.flat("General");
                    general.addOption(CodeStyleOption.number(HARD_WRAP_AT, "Hard wrap at:", 120));
                    general.addOption(CodeStyleOption.combo(WRAP_ON_TYPING, "Wrap on typing", WRAP_ON_TYPING_OPTIONS, "Default: No"));
                    general.addOption(CodeStyleOption.combo(VISUAL_GUIDES, "Visual guides", VISUAL_GUIDES_OPTIONS, "Default: None"));
                    customizer.addGroup(general);

                    // Keep when reformatting (media_1790604979247.png)
                    CodeStyleGroup keep = CodeStyleGroup.collapsible("Keep when reformatting");
                    keep.addOption(CodeStyleOption.rightAlignedCheckbox(KEEP_LINE_BREAKS, "Line breaks", true));
                    keep.addOption(CodeStyleOption.rightAlignedCheckbox(KEEP_TRAILING_COMMA, "Trailing comma", false));
                    keep.addOption(CodeStyleOption.rightAlignedCheckbox(WRAP_LONG_LINES, "Ensure right margin is not exceeded", false));
                    customizer.addGroup(keep);

                    // Arrays (media_1790604979247.png)
                    CodeStyleGroup arrays = CodeStyleGroup.flat("Arrays");
                    arrays.addOption(CodeStyleOption.combo(ARRAY_WRAPPING, "Arrays", WRAP_OPTIONS, "Wrap always"));
                    customizer.addGroup(arrays);

                    // Objects (media_1790604979247.png)
                    CodeStyleGroup objects = CodeStyleGroup.collapsibleWithCombo("Objects", OBJECT_WRAPPING, WRAP_OPTIONS, "Wrap always");
                    objects.addOption(CodeStyleOption.combo(OBJECT_ALIGN, "Align", ALIGN_OPTIONS, "Do not align"));
                    customizer.addGroup(objects);
                }
            }
        };
    }
}
