package dev.lumina.settings;

import dev.lumina.settings.CodeStyleSettings.CodeStyleGroup;
import dev.lumina.settings.CodeStyleSettings.CodeStyleOption;
import dev.lumina.settings.CodeStyleSettings.CodeStyleSettingsCustomizer;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleProvider;
import dev.lumina.settings.CodeStyleSettings.LanguageCodeStyleSettings;

import java.util.List;

/**
 * Dedicated code style settings and dynamic provider for HTTP Request.
 * Strictly decoupled and brand-isolated.
 * Supports the 3 tabs matching reference screenshots:
 * 1. Tabs and Indents (media_1790582880182.png)
 * 2. Wrapping and Braces (media_1790582889076.png)
 * 3. Spaces (media_1790582896367.png)
 */
public class HttpRequestCodeStyleSettings extends LanguageCodeStyleSettings {

    public static final List<String> WRAP_OPTIONS = List.of(
            "Do not wrap",
            "Wrap if long",
            "Chop down if long",
            "Wrap always"
    );

    // ==========================================
    // Property Keys - Tabs and Indents
    // ==========================================
    public static final String URL_PARTS_INDENT = "http_url_parts_indent";

    // ==========================================
    // Property Keys - Wrapping and Braces
    // ==========================================
    public static final String VISUAL_GUIDES = "http_visual_guides";
    public static final String FORM_URLENCODED_PARAMS_WRAP = "http_form_urlencoded_params_wrap";
    public static final String QUERY_PARAMS_WRAP = "http_query_params_wrap";

    // ==========================================
    // Property Keys - Spaces
    // ==========================================
    public static final String SPACES_AROUND_EQUALITY_IN_FORM = "http_spaces_around_equality_in_form";
    public static final String SPACE_BEFORE_AMPERSAND_IN_FORM = "http_space_before_ampersand_in_form";

    public HttpRequestCodeStyleSettings() {
        this("HTTP Request");
    }

    public HttpRequestCodeStyleSettings(String languageId) {
        super(languageId);
        initDefaults();
    }

    private void initDefaults() {
        // Tab 1: Tabs and Indents (media_1790582880182.png)
        setIndent(4);
        setTabSize(4);
        setContinuationIndent(4);
        setUseTabCharacter(false);
        setInt(URL_PARTS_INDENT, 4);

        // Tab 2: Wrapping and Braces (media_1790582889076.png)
        setString(VISUAL_GUIDES, "Default: None");
        setString(FORM_URLENCODED_PARAMS_WRAP, "Wrap always");
        setString(QUERY_PARAMS_WRAP, "Wrap if long");

        // Tab 3: Spaces (media_1790582896367.png)
        setBoolean(SPACES_AROUND_EQUALITY_IN_FORM, true);
        setBoolean(SPACE_BEFORE_AMPERSAND_IN_FORM, true);
    }

    @Override
    public HttpRequestCodeStyleSettings copy() {
        HttpRequestCodeStyleSettings copy = new HttpRequestCodeStyleSettings(getLanguageId());
        copy.setIndent(getIndent());
        copy.setTabSize(getTabSize());
        copy.setContinuationIndent(getContinuationIndent());
        copy.setUseTabCharacter(isUseTabCharacter());
        copy.setProperties(getAllProperties());
        return copy;
    }

    // ==========================================
    // Getters and Setters
    // ==========================================

    public int getUrlPartsIndent() {
        return getInt(URL_PARTS_INDENT, 4);
    }

    public void setUrlPartsIndent(int v) {
        setInt(URL_PARTS_INDENT, v);
    }

    public String getVisualGuides() {
        return getString(VISUAL_GUIDES, "Default: None");
    }

    public void setVisualGuides(String v) {
        setString(VISUAL_GUIDES, v);
    }

    public String getFormUrlencodedParamsWrap() {
        return getString(FORM_URLENCODED_PARAMS_WRAP, "Wrap always");
    }

    public void setFormUrlencodedParamsWrap(String v) {
        setString(FORM_URLENCODED_PARAMS_WRAP, v);
    }

    public String getQueryParamsWrap() {
        return getString(QUERY_PARAMS_WRAP, "Wrap if long");
    }

    public void setQueryParamsWrap(String v) {
        setString(QUERY_PARAMS_WRAP, v);
    }

    public boolean isSpacesAroundEqualityInForm() {
        return getBoolean(SPACES_AROUND_EQUALITY_IN_FORM, true);
    }

    public void setSpacesAroundEqualityInForm(boolean v) {
        setBoolean(SPACES_AROUND_EQUALITY_IN_FORM, v);
    }

    public boolean isSpaceBeforeAmpersandInForm() {
        return getBoolean(SPACE_BEFORE_AMPERSAND_IN_FORM, true);
    }

    public void setSpaceBeforeAmpersandInForm(boolean v) {
        setBoolean(SPACE_BEFORE_AMPERSAND_IN_FORM, v);
    }

    // ==========================================
    // Sample Codes matching media_1790582880182.png, media_1790582889076.png, media_1790582896367.png
    // Strictly brand-isolated.
    // ==========================================
    public static final String SAMPLE_TABS_AND_INDENTS = """
GET https://localhost:8080/
    my-url-path?param1=value1&param2=value2
Content-Type: application/json
Accept: application/json

{
  "my-json-field": "my-json-value"
}

> {%
    if (response.status === 200) {
        client.log("Success")
    } else {
        client.log("Other")
    }
%}
""";

    public static final String SAMPLE_WRAPPING = """
GET https://localhost:8080/my-url-path?
    param1=value1&
    param2=value2&p3=v3

###
GET https://{{var}}/u?p=v&q=1

###
POST https://localhost:8080/form
Content-Type: application/x-www-form-urlencoded

param1 = value1 &
param2 = value2 &
param3 = value3
""";

    public static final String SAMPLE_SPACES = """
###
POST https://localhost:8080/form
Content-Type: application/x-www-form-urlencoded

param1 = value1 &
param2 = value2 &
param3 = value3
""";

    public static LanguageCodeStyleProvider createProvider() {
        return new LanguageCodeStyleProvider() {
            @Override
            public String getLanguageId() {
                return "HTTP Request";
            }

            @Override
            public String getDisplayName() {
                return "HTTP Request";
            }

            @Override
            public List<String> getSupportedTabs() {
                return List.of("Tabs and Indents", "Wrapping and Braces", "Spaces");
            }

            @Override
            public boolean hasPreview(String tabName) {
                return true;
            }

            @Override
            public LanguageCodeStyleSettings createDefaultSettings() {
                return new HttpRequestCodeStyleSettings("HTTP Request");
            }

            @Override
            public String getSampleCode() {
                return SAMPLE_TABS_AND_INDENTS;
            }

            @Override
            public String getSampleCode(String tabName) {
                if ("Wrapping and Braces".equals(tabName)) {
                    return SAMPLE_WRAPPING;
                }
                if ("Spaces".equals(tabName)) {
                    return SAMPLE_SPACES;
                }
                return SAMPLE_TABS_AND_INDENTS;
            }

            @Override
            public void customizeSettings(CodeStyleSettingsCustomizer customizer, String tabName) {
                if ("Tabs and Indents".equals(tabName)) {
                    CodeStyleGroup indents = CodeStyleGroup.flat("Tabs and Indents");
                    indents.addOption(CodeStyleOption.number("indent", "Indent:", 4));
                    indents.addOption(CodeStyleOption.number(URL_PARTS_INDENT, "URL parts indent:", 4));
                    customizer.addGroup(indents);

                } else if ("Wrapping and Braces".equals(tabName)) {
                    CodeStyleGroup general = CodeStyleGroup.flat("Wrapping and Braces");
                    general.addOption(CodeStyleOption.combo(
                            VISUAL_GUIDES,
                            "Visual guides",
                            List.of("Default: None", "None", "80", "120"),
                            "Default: None"
                    ));
                    general.addOption(CodeStyleOption.combo(
                            FORM_URLENCODED_PARAMS_WRAP,
                            "Form-urlencoded parameters wrap:",
                            WRAP_OPTIONS,
                            "Wrap always"
                    ));
                    general.addOption(CodeStyleOption.combo(
                            QUERY_PARAMS_WRAP,
                            "Query parameters wrap:",
                            WRAP_OPTIONS,
                            "Wrap if long"
                    ));
                    customizer.addGroup(general);

                } else if ("Spaces".equals(tabName)) {
                    CodeStyleGroup formParams = CodeStyleGroup.collapsible("'x-www-form-urlencoded' parameters");
                    formParams.addOption(CodeStyleOption.checkbox(SPACES_AROUND_EQUALITY_IN_FORM, "Spaces around '='", true));
                    formParams.addOption(CodeStyleOption.checkbox(SPACE_BEFORE_AMPERSAND_IN_FORM, "Space before '&'", true));
                    customizer.addGroup(formParams);
                }
            }
        };
    }
}
