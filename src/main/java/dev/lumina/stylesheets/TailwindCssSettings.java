package dev.lumina.stylesheets;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > Style Sheets > Tailwind CSS settings in Lumina IDE.
 * Matches reference screenshots media_1791600486101_c3595d17.png & media_1791600502988_86d834fe.png:
 *  - Language Server: "@tailwindcss/language-server (Default)" (version: "0.14.22")
 *  - Configuration: Formatted JSON text matching the LSP configuration
 */
public class TailwindCssSettings implements Cloneable {

    public static final String DEFAULT_SERVER = "@tailwindcss/language-server (Default)";
    public static final String DEFAULT_VERSION = "0.14.22";

    public static final String DEFAULT_CONFIG_JSON = """
            {
              "includeLanguages": {
                "ftl": "html",
                "jinja": "html",
                "jinja2": "html",
                "smarty": "html",
                "tmpl": "gohtml",
                "cshtml": "html",
                "vbhtml": "html",
                "razor": "html"
              },
              "files": {
                "exclude": [
                  "**/.git/**",
                  "**/.hg/**",
                  "**/.svn/**",
                  "**/node_modules/**",
                  "**/.yarn/**",
                  "**/.venv/**",
                  "**/venv/**",
                  "**/.next/**",
                  "**/.parcel-cache/**",
                  "**/.svelte-kit/**",
                  "**/.turbo/**",
                  "**/__pycache__/**"
                ]
              },
              "emmetCompletions": false,
              "classAttributes": [
                "class",
                "className",
                "ngClass"
              ],
              "colorDecorators": true,
              "showPixelEquivalents": true,
              "rootFontSize": 16,
              "hovers": true,
              "suggestions": true,
              "codeActions": true,
              "validate": true,
              "lint": {
                "invalidScreen": "error",
                "invalidVariant": "error",
                "invalidTailwindDirective": "error",
                "invalidApply": "error",
                "invalidConfigPath": "error",
                "cssConflict": "warning",
                "recommendedVariantOrder": "warning"
              },
              "experimental": {
                "configFile": null,
                "classRegex": []
              }
            }""";

    private String languageServer = DEFAULT_SERVER;
    private String languageServerVersion = DEFAULT_VERSION;
    private String customServerPath = "";
    private String configurationJson = DEFAULT_CONFIG_JSON;

    public TailwindCssSettings() {
    }

    public TailwindCssSettings(TailwindCssSettings other) {
        if (other != null) {
            this.languageServer = other.languageServer != null ? other.languageServer : DEFAULT_SERVER;
            this.languageServerVersion = other.languageServerVersion != null ? other.languageServerVersion : DEFAULT_VERSION;
            this.customServerPath = other.customServerPath != null ? other.customServerPath : "";
            this.configurationJson = other.configurationJson != null ? other.configurationJson : DEFAULT_CONFIG_JSON;
        }
    }

    public String getLanguageServer() {
        return languageServer;
    }

    public void setLanguageServer(String languageServer) {
        this.languageServer = languageServer != null ? languageServer : DEFAULT_SERVER;
    }

    public String getLanguageServerVersion() {
        return languageServerVersion;
    }

    public void setLanguageServerVersion(String languageServerVersion) {
        this.languageServerVersion = languageServerVersion != null ? languageServerVersion : DEFAULT_VERSION;
    }

    public String getCustomServerPath() {
        return customServerPath;
    }

    public void setCustomServerPath(String customServerPath) {
        this.customServerPath = customServerPath != null ? customServerPath : "";
    }

    public String getConfigurationJson() {
        return configurationJson;
    }

    public void setConfigurationJson(String configurationJson) {
        this.configurationJson = configurationJson != null ? configurationJson : DEFAULT_CONFIG_JSON;
    }

    @Override
    public TailwindCssSettings clone() {
        return new TailwindCssSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TailwindCssSettings that = (TailwindCssSettings) o;
        return Objects.equals(languageServer, that.languageServer) &&
                Objects.equals(languageServerVersion, that.languageServerVersion) &&
                Objects.equals(customServerPath, that.customServerPath) &&
                Objects.equals(configurationJson.trim(), that.configurationJson.trim());
    }

    @Override
    public int hashCode() {
        return Objects.hash(languageServer, languageServerVersion, customServerPath, configurationJson.trim());
    }

    @Override
    public String toString() {
        return "TailwindCssSettings{" +
                "languageServer='" + languageServer + '\'' +
                ", languageServerVersion='" + languageServerVersion + '\'' +
                ", customServerPath='" + customServerPath + '\'' +
                '}';
    }
}
