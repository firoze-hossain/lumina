package dev.lumina.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > Web Contexts settings in Lumina IDE.
 * Matches reference screenshot media_1791604029588_6d4f3b81.png:
 *  - List of Path to Web Context mappings
 */
public class WebContextsSettings implements Cloneable {

    private List<WebContextMapping> mappings = new ArrayList<>();

    public WebContextsSettings() {
    }

    public WebContextsSettings(WebContextsSettings other) {
        if (other != null && other.mappings != null) {
            this.mappings = new ArrayList<>();
            for (WebContextMapping m : other.mappings) {
                this.mappings.add(m.clone());
            }
        }
    }

    public List<WebContextMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<WebContextMapping> mappings) {
        this.mappings = mappings != null ? new ArrayList<>(mappings) : new ArrayList<>();
    }

    public void addMapping(WebContextMapping mapping) {
        if (mapping != null) {
            mappings.removeIf(m -> Objects.equals(m.getPath(), mapping.getPath()));
            mappings.add(mapping);
        }
    }

    public void removeMapping(String path) {
        mappings.removeIf(m -> Objects.equals(m.getPath(), path));
    }

    @Override
    public WebContextsSettings clone() {
        return new WebContextsSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebContextsSettings that = (WebContextsSettings) o;
        return Objects.equals(mappings, that.mappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappings);
    }
}
