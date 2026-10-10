package dev.lumina.templates;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > Template Data Languages settings in Lumina IDE.
 * Matches reference screenshot media_1791602845046_bc1f39dd.png:
 *  - Project Language: [ <None> v ]
 *  - Path to Language mappings list
 */
public class TemplateDataLanguagesSettings implements Cloneable {

    public static final List<String> STANDARD_LANGUAGES = List.of(
            "<None>",
            "HTML",
            "XML",
            "JSON",
            "CSS",
            "JavaScript",
            "TypeScript",
            "SQL",
            "YAML",
            "JSP",
            "FreeMarker",
            "Velocity",
            "Vue",
            "PHP"
    );

    private String projectLanguage = "<None>";
    private List<TemplateDataLanguageMapping> mappings = new ArrayList<>();

    public TemplateDataLanguagesSettings() {
    }

    public TemplateDataLanguagesSettings(TemplateDataLanguagesSettings other) {
        if (other != null) {
            this.projectLanguage = other.projectLanguage != null ? other.projectLanguage : "<None>";
            this.mappings = new ArrayList<>();
            if (other.mappings != null) {
                for (TemplateDataLanguageMapping m : other.mappings) {
                    this.mappings.add(m.clone());
                }
            }
        }
    }

    public String getProjectLanguage() {
        return projectLanguage;
    }

    public void setProjectLanguage(String projectLanguage) {
        this.projectLanguage = projectLanguage != null ? projectLanguage : "<None>";
    }

    public List<TemplateDataLanguageMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<TemplateDataLanguageMapping> mappings) {
        this.mappings = mappings != null ? new ArrayList<>(mappings) : new ArrayList<>();
    }

    public void addMapping(TemplateDataLanguageMapping mapping) {
        if (mapping != null) {
            mappings.removeIf(m -> Objects.equals(m.getPath(), mapping.getPath()));
            mappings.add(mapping);
        }
    }

    public void removeMapping(String path) {
        mappings.removeIf(m -> Objects.equals(m.getPath(), path));
    }

    @Override
    public TemplateDataLanguagesSettings clone() {
        return new TemplateDataLanguagesSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateDataLanguagesSettings that = (TemplateDataLanguagesSettings) o;
        return Objects.equals(projectLanguage, that.projectLanguage) &&
                Objects.equals(mappings, that.mappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectLanguage, mappings);
    }
}
