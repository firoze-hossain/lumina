package dev.lumina.templates;

import java.util.Objects;

/**
 * Represents a path-to-template-data-language mapping.
 */
public class TemplateDataLanguageMapping implements Cloneable {

    private String path = "";
    private String language = "<None>";

    public TemplateDataLanguageMapping() {
    }

    public TemplateDataLanguageMapping(String path, String language) {
        this.path = path != null ? path : "";
        this.language = language != null ? language : "<None>";
    }

    public TemplateDataLanguageMapping(TemplateDataLanguageMapping other) {
        if (other != null) {
            this.path = other.path;
            this.language = other.language;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path : "";
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language != null ? language : "<None>";
    }

    @Override
    public TemplateDataLanguageMapping clone() {
        return new TemplateDataLanguageMapping(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateDataLanguageMapping that = (TemplateDataLanguageMapping) o;
        return Objects.equals(path, that.path) && Objects.equals(language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, language);
    }
}
