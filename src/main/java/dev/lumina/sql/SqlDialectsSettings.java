package dev.lumina.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > SQL Dialects settings in Lumina IDE.
 * Matches reference screenshot media_1791600383044_0ff4ac92.png:
 *  - Global SQL Dialect (default: "<None>")
 *  - Project SQL Dialect (default: "<None>")
 *  - Path to Dialect mappings list
 */
public class SqlDialectsSettings implements Cloneable {

    public static final List<String> STANDARD_DIALECTS = List.of(
            "<None>",
            "Generic SQL",
            "Amazon Redshift",
            "Apache Derby",
            "Cassandra CQL",
            "ClickHouse",
            "Couchbase Query (N1QL)",
            "DuckDB",
            "Exasol",
            "Google BigQuery",
            "H2",
            "Hive",
            "HSQLDB",
            "IBM DB2",
            "MariaDB",
            "Microsoft SQL Server",
            "MongoDB",
            "MySQL",
            "Oracle",
            "Oracle SQL*Plus",
            "PostgreSQL",
            "Presto",
            "Snowflake",
            "SQLite",
            "Sybase",
            "Trino",
            "Vertica"
    );

    private String globalDialect = "<None>";
    private String projectDialect = "<None>";
    private List<SqlDialectMapping> mappings = new ArrayList<>();

    public SqlDialectsSettings() {
    }

    public SqlDialectsSettings(SqlDialectsSettings other) {
        if (other != null) {
            this.globalDialect = other.globalDialect != null ? other.globalDialect : "<None>";
            this.projectDialect = other.projectDialect != null ? other.projectDialect : "<None>";
            this.mappings = new ArrayList<>();
            if (other.mappings != null) {
                for (SqlDialectMapping m : other.mappings) {
                    this.mappings.add(m.clone());
                }
            }
        }
    }

    public String getGlobalDialect() {
        return globalDialect;
    }

    public void setGlobalDialect(String globalDialect) {
        this.globalDialect = globalDialect != null ? globalDialect : "<None>";
    }

    public String getProjectDialect() {
        return projectDialect;
    }

    public void setProjectDialect(String projectDialect) {
        this.projectDialect = projectDialect != null ? projectDialect : "<None>";
    }

    public List<SqlDialectMapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<SqlDialectMapping> mappings) {
        this.mappings = mappings != null ? new ArrayList<>(mappings) : new ArrayList<>();
    }

    public void addMapping(SqlDialectMapping mapping) {
        if (mapping != null) {
            this.mappings.add(mapping);
        }
    }

    public void removeMapping(SqlDialectMapping mapping) {
        this.mappings.remove(mapping);
    }

    @Override
    public SqlDialectsSettings clone() {
        return new SqlDialectsSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SqlDialectsSettings that = (SqlDialectsSettings) o;
        return Objects.equals(globalDialect, that.globalDialect) &&
                Objects.equals(projectDialect, that.projectDialect) &&
                Objects.equals(mappings, that.mappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(globalDialect, projectDialect, mappings);
    }

    @Override
    public String toString() {
        return "SqlDialectsSettings{" +
                "globalDialect='" + globalDialect + '\'' +
                ", projectDialect='" + projectDialect + '\'' +
                ", mappings=" + mappings +
                '}';
    }
}
