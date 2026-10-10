package dev.lumina.schemas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing a JSON Schema mapping configuration.
 * Matches Image 5:
 *  - Name: "New Schema"
 *  - Schema file or URL
 *  - Schema version: "JSON Schema v4", "JSON Schema v7", "JSON Schema 2019-09", "JSON Schema 2020-12"
 *  - List of patterns (file, pattern, directory)
 */
public class JsonSchemaMapping {

    public static final String VERSION_V4 = "JSON Schema v4";
    public static final String VERSION_V7 = "JSON Schema v7";
    public static final String VERSION_2019_09 = "JSON Schema 2019-09";
    public static final String VERSION_2020_12 = "JSON Schema 2020-12";

    private String name = "New Schema";
    private String schemaFileOrUrl = "";
    private String schemaVersion = VERSION_V4;
    private List<JsonSchemaPattern> patterns = new ArrayList<>();

    public JsonSchemaMapping() {
    }

    public JsonSchemaMapping(String name, String schemaFileOrUrl, String schemaVersion) {
        this(name, schemaFileOrUrl, schemaVersion, new ArrayList<>());
    }

    public JsonSchemaMapping(String name, String schemaFileOrUrl, String schemaVersion, List<JsonSchemaPattern> patterns) {
        this.name = name != null ? name : "New Schema";
        this.schemaFileOrUrl = schemaFileOrUrl != null ? schemaFileOrUrl : "";
        this.schemaVersion = schemaVersion != null ? schemaVersion : VERSION_V4;
        if (patterns != null) {
            for (JsonSchemaPattern p : patterns) {
                this.patterns.add(p != null ? p.copy() : new JsonSchemaPattern());
            }
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "New Schema";
    }

    public String getSchemaFileOrUrl() {
        return schemaFileOrUrl;
    }

    public void setSchemaFileOrUrl(String schemaFileOrUrl) {
        this.schemaFileOrUrl = schemaFileOrUrl != null ? schemaFileOrUrl : "";
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion != null ? schemaVersion : VERSION_V4;
    }

    public List<JsonSchemaPattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<JsonSchemaPattern> patterns) {
        this.patterns = new ArrayList<>();
        if (patterns != null) {
            for (JsonSchemaPattern p : patterns) {
                this.patterns.add(p != null ? p.copy() : new JsonSchemaPattern());
            }
        }
    }

    public JsonSchemaMapping copy() {
        return new JsonSchemaMapping(name, schemaFileOrUrl, schemaVersion, patterns);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonSchemaMapping that = (JsonSchemaMapping) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(schemaFileOrUrl, that.schemaFileOrUrl) &&
                Objects.equals(schemaVersion, that.schemaVersion) &&
                Objects.equals(patterns, that.patterns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, schemaFileOrUrl, schemaVersion, patterns);
    }

    @Override
    public String toString() {
        return "JsonSchemaMapping{" +
                "name='" + name + '\'' +
                ", schemaFileOrUrl='" + schemaFileOrUrl + '\'' +
                ", schemaVersion='" + schemaVersion + '\'' +
                ", patterns=" + patterns +
                '}';
    }
}
