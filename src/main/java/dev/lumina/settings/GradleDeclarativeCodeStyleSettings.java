package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.List;

/**
 * Dedicated code style settings and dynamic provider for Gradle Declarative Configuration.
 * Strictly decoupled and brand-isolated.
 * Supports exactly 1 tab matching reference screenshot media_1790578358987.png:
 * - Tabs and Indents
 */
public class GradleDeclarativeCodeStyleSettings extends LanguageCodeStyleSettings {

    public GradleDeclarativeCodeStyleSettings() {
        this("Gradle Declarative Configuration");
    }

    public GradleDeclarativeCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        setTabSize(4);
        setIndent(4);
        setContinuationIndent(8);
        setUseTabCharacter(false);
        setSmartTabs(false);
        setKeepIndentsOnEmptyLines(false);
    }

    @Override
    public GradleDeclarativeCodeStyleSettings copy() {
        GradleDeclarativeCodeStyleSettings copy = new GradleDeclarativeCodeStyleSettings(getLanguageId());
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
    // Sample Code matching media_1790578358987.png verbatim
    // ==========================================
    public static final String SAMPLE_GRADLE_DECLARATIVE = """
block {
    key = "value"
    anotherKey = factory("parameter")
}
""";

    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "Gradle Declarative Configuration";
            }

            @Override
            public String getDisplayName() {
                return "Gradle Declarative Configuration";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents");
            }

            @Override
            public boolean hasPreview(String tabName) {
                return true;
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new GradleDeclarativeCodeStyleSettings("Gradle Declarative Configuration");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_GRADLE_DECLARATIVE;
            }

            @Override
            public String getSampleCode(String tabName) {
                return SAMPLE_GRADLE_DECLARATIVE;
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Tabs and Indents".equals(tabName)) {
                    CodeStyleGroup g = CodeStyleGroup.flat("Tabs and Indents");
                    g.addOption(CodeStyleOption.checkbox("use_tab_character", "Use tab character", false));
                    g.addOption(CodeStyleOption.indentedCheckbox("smart_tabs", "Smart tabs", false));
                    g.addOption(CodeStyleOption.number("tab_size", "Tab size:", 4));
                    g.addOption(CodeStyleOption.number("indent", "Indent:", 4));
                    g.addOption(CodeStyleOption.number("continuation_indent", "Continuation indent:", 8));
                    g.addOption(CodeStyleOption.checkbox("keep_indents_on_empty_lines", "Keep indents on empty lines", false));
                    customizer.addGroup(g);
                }
            }
        };
    }
}
