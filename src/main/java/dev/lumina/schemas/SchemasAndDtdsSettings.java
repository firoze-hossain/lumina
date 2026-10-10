package dev.lumina.schemas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > Schemas and DTDs settings.
 * Matches Image 3:
 *  - External schemas and DTDs (URI, Location)
 *  - Ignored schemas and DTDs (list of 8 default URIs)
 */
public class SchemasAndDtdsSettings {

    private List<ExternalResourceEntry> externalResources = new ArrayList<>();
    private List<String> ignoredSchemas = createDefaultIgnoredSchemas();

    public static List<String> createDefaultIgnoredSchemas() {
        List<String> list = new ArrayList<>();
        list.add("http://exslt.org/common");
        list.add("http://exslt.org/dates-and-times");
        list.add("http://exslt.org/dynamic");
        list.add("http://exslt.org/math");
        list.add("http://exslt.org/sets");
        list.add("http://exslt.org/strings");
        list.add("http://relaxng.org/ns/compatibility/annotations/1.0");
        list.add("urn:lumina:xslt-plugin#extensions");
        return list;
    }

    public SchemasAndDtdsSettings() {
    }

    public SchemasAndDtdsSettings(List<ExternalResourceEntry> externalResources, List<String> ignoredSchemas) {
        if (externalResources != null) {
            for (ExternalResourceEntry r : externalResources) {
                this.externalResources.add(r != null ? r.copy() : new ExternalResourceEntry());
            }
        }
        if (ignoredSchemas != null) {
            this.ignoredSchemas = new ArrayList<>(ignoredSchemas);
        }
    }

    public List<ExternalResourceEntry> getExternalResources() {
        return externalResources;
    }

    public void setExternalResources(List<ExternalResourceEntry> externalResources) {
        this.externalResources = new ArrayList<>();
        if (externalResources != null) {
            for (ExternalResourceEntry r : externalResources) {
                this.externalResources.add(r != null ? r.copy() : new ExternalResourceEntry());
            }
        }
    }

    public List<String> getIgnoredSchemas() {
        return ignoredSchemas;
    }

    public void setIgnoredSchemas(List<String> ignoredSchemas) {
        this.ignoredSchemas = new ArrayList<>();
        if (ignoredSchemas != null) {
            this.ignoredSchemas.addAll(ignoredSchemas);
        }
    }

    public SchemasAndDtdsSettings copy() {
        return new SchemasAndDtdsSettings(externalResources, ignoredSchemas);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemasAndDtdsSettings that = (SchemasAndDtdsSettings) o;
        return Objects.equals(externalResources, that.externalResources) &&
                Objects.equals(ignoredSchemas, that.ignoredSchemas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(externalResources, ignoredSchemas);
    }

    @Override
    public String toString() {
        return "SchemasAndDtdsSettings{" +
                "externalResources=" + externalResources +
                ", ignoredSchemas=" + ignoredSchemas +
                '}';
    }
}
