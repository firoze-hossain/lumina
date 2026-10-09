package dev.lumina.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Python Template Languages settings in Lumina IDE.
 * Faithfully matches Image 1.
 */
public class PythonTemplateLanguagesSettings {

    private String templateLanguage = "None"; // "None", "Jinja2", "Django", "Mako", "Web2Py"
    private List<String> templateFileTypes = new ArrayList<>(List.of("XHTML", "XML", "HTML"));

    public PythonTemplateLanguagesSettings() {}

    public PythonTemplateLanguagesSettings(PythonTemplateLanguagesSettings other) {
        if (other == null) return;
        this.templateLanguage = other.templateLanguage;
        this.templateFileTypes = new ArrayList<>(other.templateFileTypes);
    }

    public PythonTemplateLanguagesSettings copy() {
        return new PythonTemplateLanguagesSettings(this);
    }

    public String getTemplateLanguage() {
        return templateLanguage;
    }

    public void setTemplateLanguage(String templateLanguage) {
        this.templateLanguage = templateLanguage != null ? templateLanguage : "None";
    }

    public List<String> getTemplateFileTypes() {
        return templateFileTypes;
    }

    public void setTemplateFileTypes(List<String> templateFileTypes) {
        this.templateFileTypes = templateFileTypes != null ? new ArrayList<>(templateFileTypes) : new ArrayList<>();
    }

    public void addFileType(String type) {
        if (type != null && !type.isBlank() && !templateFileTypes.contains(type)) {
            templateFileTypes.add(type);
        }
    }

    public void removeFileType(String type) {
        templateFileTypes.remove(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PythonTemplateLanguagesSettings that)) return false;
        return Objects.equals(templateLanguage, that.templateLanguage) &&
                Objects.equals(templateFileTypes, that.templateFileTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(templateLanguage, templateFileTypes);
    }
}
