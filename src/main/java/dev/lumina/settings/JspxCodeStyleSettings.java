package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.List;

/**
 * Dedicated code style settings and dynamic provider for JSPX.
 * Strictly decoupled, dynamically configured without hardcoding, and brand-isolated.
 * Supports all 3 tabs matching reference screenshots:
 * 1. Tabs and Indents (media_1790606895909.png)
 * 2. Wrapping (media_1790606906091.png)
 * 3. Imports
 */
public class JspxCodeStyleSettings extends JspCodeStyleSettings {

    public JspxCodeStyleSettings() {
        this("JSPX");
    }

    public JspxCodeStyleSettings(String languageId) {
        super(languageId);
    }

    @Override
    public JspxCodeStyleSettings copy() {
        JspxCodeStyleSettings copy = new JspxCodeStyleSettings(getLanguageId());
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
    // Sample Code matching media_1790606895909.png
    // ==========================================
    public static final String SAMPLE_CODE = """
<jsp:root
        xmlns:c="http://java.sun.com/jsp/jstl/core"
        xmlns:jsp="http://java.sun.com/JSP/Page"
        version="2.0">
    <jsp:declaration>
        Collection myData;
    </jsp:declaration>
    <HTML>
    <HEAD>
        <TITLE>Another Tag File Example</TITLE>
    </HEAD>
    <BODY>
    <H2>News Portal: Another Tag File Example</H2>
    <TABLE border="0">
        <jsp:scriptlet>
            for (Iterator each = myData.iterator(); each.hasNext(); ) {
                Element eachElem = each.next();
        </jsp:scriptlet>
        <TR valign="top">
            <TD>
                <jsp:scriptlet>
                    output.write(eachElem.getName());
                </jsp:scriptlet>
            </TD>
            <TD>
                <jsp:scriptlet>
                    output.write(eachElem.getPrice());
                </jsp:scriptlet>
            </TD>
            <TD>
                <jsp:scriptlet>
                    output.write(eachElem.getDiscount());
                </jsp:scriptlet>
            </TD>
        </TR>
""";

    /**
     * Creates and registers the dynamic LanguageCodeStyleProvider for JSPX.
     */
    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "JSPX";
            }

            @Override
            public String getDisplayName() {
                return "JSPX";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents", "Wrapping", "Imports");
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new JspxCodeStyleSettings("JSPX");
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
