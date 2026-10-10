package dev.lumina.database.versioning;

import java.util.*;

/**
 * Settings model for Tools > Database Versioning > Type Mappings in Lumina IDE.
 */
public class DatabaseTypeMappingsSettings implements Cloneable {

    public static final List<String> SUPPORTED_DATABASES = List.of(
            "mysql",
            "mariadb",
            "postgresql",
            "mssql",
            "oracle",
            "h2",
            "db2",
            "hsqldb"
    );

    private String selectedDatabase = "mysql";
    private Map<String, List<DatabaseTypeMappingItem>> databaseMappings = new LinkedHashMap<>();

    public DatabaseTypeMappingsSettings() {
        for (String db : SUPPORTED_DATABASES) {
            databaseMappings.put(db, new ArrayList<>());
        }
    }

    public String getSelectedDatabase() {
        return selectedDatabase;
    }

    public void setSelectedDatabase(String selectedDatabase) {
        this.selectedDatabase = selectedDatabase != null ? selectedDatabase : "mysql";
    }

    public Map<String, List<DatabaseTypeMappingItem>> getDatabaseMappings() {
        return databaseMappings;
    }

    public void setDatabaseMappings(Map<String, List<DatabaseTypeMappingItem>> databaseMappings) {
        if (databaseMappings != null) {
            this.databaseMappings = new LinkedHashMap<>();
            for (String db : SUPPORTED_DATABASES) {
                List<DatabaseTypeMappingItem> list = databaseMappings.get(db);
                this.databaseMappings.put(db, list != null ? new ArrayList<>(list) : new ArrayList<>());
            }
        }
    }

    public List<DatabaseTypeMappingItem> getMappingsForDatabase(String db) {
        return databaseMappings.computeIfAbsent(db, k -> new ArrayList<>());
    }

    @Override
    public DatabaseTypeMappingsSettings clone() {
        try {
            DatabaseTypeMappingsSettings copy = (DatabaseTypeMappingsSettings) super.clone();
            copy.databaseMappings = new LinkedHashMap<>();
            for (Map.Entry<String, List<DatabaseTypeMappingItem>> entry : this.databaseMappings.entrySet()) {
                List<DatabaseTypeMappingItem> listCopy = new ArrayList<>();
                for (DatabaseTypeMappingItem item : entry.getValue()) {
                    listCopy.add(item.clone());
                }
                copy.databaseMappings.put(entry.getKey(), listCopy);
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseTypeMappingsSettings that = (DatabaseTypeMappingsSettings) o;
        return Objects.equals(selectedDatabase, that.selectedDatabase) &&
                Objects.equals(databaseMappings, that.databaseMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(selectedDatabase, databaseMappings);
    }
}
