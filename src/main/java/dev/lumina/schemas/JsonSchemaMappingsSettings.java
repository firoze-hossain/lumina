package dev.lumina.schemas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > Schemas and DTDs > JSON Schema Mappings settings.
 * Matches Image 5:
 *  - List of schema mappings (defaults to 1 "New Schema")
 */
public class JsonSchemaMappingsSettings {

    private List<JsonSchemaMapping> mappings = createDefaultMappings();

    public static List<JsonSchemaMapping> createDefaultMappings() {
        List<JsonSchemaMapping> list = new ArrayList<>();
        list.add(new JsonSchemaMapping("New Schema", "", JsonSchemaMapping.VERSION_V4, new ArrayList<>()));
        return list;
    }

    public JsonSchemaMappingsSettings() {
    }

    public JsonSchemaMappingsSettings(List<JsonSchemaMapping> mappings) {
        if (mappings != null) {
            this.mappings = new ArrayList<>();
            for (JsonSchemaMapping m : mappings) {
                this.mappings.add(m != null ? m.copy() : new JsonSchemaMapping());
            }
        }
    }

    public List<JsonSchemaMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<JsonSchemaMapping> mappings) {
        this.mappings = new ArrayList<>();
        if (mappings != null) {
            for (JsonSchemaMapping m : mappings) {
                this.mappings.add(m != null ? m.copy() : new JsonSchemaMapping());
            }
        }
    }

    public JsonSchemaMappingsSettings copy() {
        return new JsonSchemaMappingsSettings(mappings);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonSchemaMappingsSettings that = (JsonSchemaMappingsSettings) o;
        return Objects.equals(mappings, that.mappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mappings);
    }

    @Override
    public String toString() {
        return "JsonSchemaMappingsSettings{" +
                "mappings=" + mappings +
                '}';
    }
}
