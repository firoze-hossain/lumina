package dev.lumina.stylesheets;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > Style Sheets > Dialects settings in Lumina IDE.
 * Matches reference screenshot media_1791600448941_316b34fc.png:
 *  - Project CSS Dialect (default: "<None>")
 *  - Path to Dialect mappings list
 */
public class CssDialectsSettings implements Cloneable {

    public static final List<String> STANDARD_DIALECTS = List.of(
            "<None>",
            "CSS",
            "Sass",
            "SCSS",
            "Less",
            "Stylus",
            "PostCSS"
    );

    private String projectDialect = "<None>";
    private List<CssDialectMapping> mappings = new ArrayList<>();

    public CssDialectsSettings() {
    }

    public CssDialectsSettings(CssDialectsSettings other) {
        if (other != null) {
            this.projectDialect = other.projectDialect != null ? other.projectDialect : "<None>";
            this.mappings = new ArrayList<>();
            if (other.mappings != null) {
                for (CssDialectMapping m : other.mappings) {
                    this.mappings.add(m.clone());
                }
            }
        }
    }

    public String getProjectDialect() {
        return projectDialect;
    }

    public void setProjectDialect(String projectDialect) {
        this.projectDialect = projectDialect != null ? projectDialect : "<None>";
    }

    public List<CssDialectMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<CssDialectMapping> mappings) {
        this.mappings = mappings != null ? new ArrayList<>(mappings) : new ArrayList<>();
    }

    public void addMapping(CssDialectMapping mapping) {
        if (mapping != null) {
            this.mappings.add(mapping);
        }
    }

    public void removeMapping(CssDialectMapping mapping) {
        this.mappings.remove(mapping);
    }

    @Override
    public CssDialectsSettings clone() {
        return new CssDialectsSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CssDialectsSettings that = (CssDialectsSettings) o;
        return Objects.equals(projectDialect, that.projectDialect) &&
                Objects.equals(mappings, that.mappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectDialect, mappings);
    }

    @Override
    public String toString() {
        return "CssDialectsSettings{" +
                "projectDialect='" + projectDialect + '\'' +
                ", mappings=" + mappings +
                '}';
    }
}
