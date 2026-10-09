package dev.lumina.javascript;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a JavaScript library in Lumina IDE.
 */
public class JavaScriptLibrary {

    private String name;
    private boolean enabled;
    private String type; // "Predefined", "Project", "Global"
    private List<String> sourceFiles;
    private List<String> documentationUrls;

    public JavaScriptLibrary() {
        this("", false, "Project", new ArrayList<>(), new ArrayList<>());
    }

    public JavaScriptLibrary(String name, boolean enabled, String type) {
        this(name, enabled, type, new ArrayList<>(), new ArrayList<>());
    }

    public JavaScriptLibrary(String name, boolean enabled, String type, List<String> sourceFiles, List<String> documentationUrls) {
        this.name = name != null ? name : "";
        this.enabled = enabled;
        this.type = type != null ? type : "Project";
        this.sourceFiles = sourceFiles != null ? new ArrayList<>(sourceFiles) : new ArrayList<>();
        this.documentationUrls = documentationUrls != null ? new ArrayList<>(documentationUrls) : new ArrayList<>();
    }

    public JavaScriptLibrary copy() {
        return new JavaScriptLibrary(name, enabled, type, new ArrayList<>(sourceFiles), new ArrayList<>(documentationUrls));
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type != null ? type : "Project";
    }

    public List<String> getSourceFiles() {
        return sourceFiles;
    }

    public void setSourceFiles(List<String> sourceFiles) {
        this.sourceFiles = sourceFiles != null ? new ArrayList<>(sourceFiles) : new ArrayList<>();
    }

    public List<String> getDocumentationUrls() {
        return documentationUrls;
    }

    public void setDocumentationUrls(List<String> documentationUrls) {
        this.documentationUrls = documentationUrls != null ? new ArrayList<>(documentationUrls) : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JavaScriptLibrary that)) return false;
        return enabled == that.enabled &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&
                Objects.equals(sourceFiles, that.sourceFiles) &&
                Objects.equals(documentationUrls, that.documentationUrls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, enabled, type, sourceFiles, documentationUrls);
    }
}
