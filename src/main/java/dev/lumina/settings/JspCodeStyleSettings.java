package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.List;

/**
 * Dedicated code style settings and dynamic provider for JSP.
 * Strictly decoupled, dynamically configured without hardcoding, and brand-isolated.
 * Supports all 3 tabs matching reference screenshots:
 * 1. Tabs and Indents (media_1790606858671.png)
 * 2. Wrapping (media_1790606869354.png)
 * 3. Imports (media_1790606880229.png)
 */
public class JspCodeStyleSettings extends LanguageCodeStyleSettings {

    // ==========================================
    // Option Choice Lists
    // ==========================================
    public static final List<String> WRAP_ON_TYPING_OPTIONS = List.of(
            "Default: No",
            "Yes",
            "No"
    );

    // ==========================================
    // Property Keys - Wrapping (media_1790606869354.png)
    // ==========================================
    public static final String HARD_WRAP_AT = "jsp_hard_wrap_at";
    public static final String WRAP_ON_TYPING = "jsp_wrap_on_typing";
    public static final String VISUAL_GUIDES = "jsp_visual_guides";

    // ==========================================
    // Property Keys - Imports (media_1790606880229.png)
    // ==========================================
    public static final String JSP_PREFER_COMMA_SEPARATED_IMPORT_LIST = "jsp_prefer_comma_separated_import_list";

    public JspCodeStyleSettings() {
        this("JSP");
    }

    public JspCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    protected void initDefaults() {
        // Tab 1: Tabs and Indents (media_1790606858671.png)
        setTabSize(4);
        setIndent(4);
        setContinuationIndent(8);
        setUseTabCharacter(false);
        setSmartTabs(false);
        setKeepIndentsOnEmptyLines(false);

        // Tab 2: Wrapping (media_1790606869354.png)
        setInt(HARD_WRAP_AT, 120);
        setString(WRAP_ON_TYPING, "Default: No");
        setString(VISUAL_GUIDES, "");

        // Tab 3: Imports (media_1790606880229.png)
        setBoolean(JSP_PREFER_COMMA_SEPARATED_IMPORT_LIST, false);
    }

    @Override
    public JspCodeStyleSettings copy() {
        JspCodeStyleSettings copy = new JspCodeStyleSettings(getLanguageId());
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
    // Getters and Setters - Wrapping
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
        return getString(VISUAL_GUIDES, "");
    }

    public void setVisualGuides(String val) {
        setString(VISUAL_GUIDES, val);
    }

    // ==========================================
    // Getters and Setters - Imports
    // ==========================================

    public boolean isPreferCommaSeparatedImportList() {
        return getBoolean(JSP_PREFER_COMMA_SEPARATED_IMPORT_LIST, false);
    }

    public void setPreferCommaSeparatedImportList(boolean val) {
        setBoolean(JSP_PREFER_COMMA_SEPARATED_IMPORT_LIST, val);
    }

    // ==========================================
    // Sample Code matching media_1790606858671.png and media_1790606880229.png
    // ==========================================
    public static final String SAMPLE_CODE = """
<%-- Sample comment --%>
<%@ page import="com.company.*" %>
<jsp:useBean id="info" class="com.company.Info"/>
<%!
    int i = 0;
%>
<html>
<body>
Welcome to Lumina
${ (info['version'] le 12 )? "Evaluate latest version":"" }
Release timestamp: ${ info.getReleaseDate(majorVersion, minorVersion).time }
<% for (i = 1; i < 5; i++) { %>
<h<%= i %>>Try it, it's cool!</h<%= i %>>
<% } %>
</body>
</html>
<!-- HTML comments are seen by clients, JSP aren't -->
""";

    public static final String SAMPLE_IMPORTS_ONE_PER_DIRECTIVE = """
<%@ page import="com.company.Boo"%>
<%@ page import="com.company.Far"%>
""";

    public static final String SAMPLE_IMPORTS_COMMA_SEPARATED = """
<%@ page import="com.company.Boo,
                 com.company.Far"%>
""";

    /**
     * Creates and registers the dynamic LanguageCodeStyleProvider for JSP.
     */
    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "JSP";
            }

            @Override
            public String getDisplayName() {
                return "JSP";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents", "Wrapping", "Imports");
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new JspCodeStyleSettings("JSP");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_CODE;
            }

            @Override
            public boolean hasPreview(String tabName) {
                return "Tabs and Indents".equals(tabName);
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Tabs and Indents".equals(tabName)) {
                    CodeStyleGroup indents = CodeStyleGroup.flat("Tabs and Indents");
                    indents.addOption(CodeStyleOption.checkbox("use_tab_character", "Use tab character", false));
                    indents.addOption(CodeStyleOption.indentedCheckbox("smart_tabs", "Smart tabs", false));
                    indents.addOption(CodeStyleOption.number("tab_size", "Tab size:", 4));
                    indents.addOption(CodeStyleOption.number("indent", "Indent:", 4));
                    indents.addOption(CodeStyleOption.number("continuation_indent", "Continuation indent:", 8));
                    indents.addOption(CodeStyleOption.checkbox("keep_indents_on_empty_lines", "Keep indents on empty lines", false));
                    customizer.addGroup(indents);

                } else if ("Wrapping".equals(tabName)) {
                    CodeStyleGroup wrap = CodeStyleGroup.flat("Wrapping");
                    wrap.addOption(CodeStyleOption.number(HARD_WRAP_AT, "Hard wrap at:", 120));
                    wrap.addOption(CodeStyleOption.combo(WRAP_ON_TYPING, "Wrap on typing", WRAP_ON_TYPING_OPTIONS, "Default: No"));
                    wrap.addOption(CodeStyleOption.text(VISUAL_GUIDES, "Visual guides:", ""));
                    customizer.addGroup(wrap);

                } else if ("Imports".equals(tabName)) {
                    CodeStyleGroup imports = CodeStyleGroup.divider("JSP Imports Layout");
                    imports.addOption(CodeStyleOption.checkbox(JSP_PREFER_COMMA_SEPARATED_IMPORT_LIST, "Prefer comma-separated import list", false));
                    customizer.addGroup(imports);
                }
            }
        };
    }
}
