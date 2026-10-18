package dev.lumina.database;

import java.sql.*;
import java.util.*;

/**
 * Discovers database catalogs, schemas, tables, views, and columns.
 */
public class DbMetadataService {

    public record TestResult(boolean success, String message, long latencyMs, String serverVersion) {}

    public record ColumnInfo(String name, String typeName, int columnSize, boolean nullable, boolean primaryKey) {}

    public record TableInfo(String name, String type, List<ColumnInfo> columns) {}

    public record SchemaInfo(String name, List<TableInfo> tables, List<TableInfo> views) {}

    public record CatalogInfo(String name, List<SchemaInfo> schemas) {}

    public static TestResult testConnection(DbConnectionProfile profile) {
        long start = System.currentTimeMillis();
        if (profile.getEngine() == DbEngineType.MONGODB) {
            try {
                // Test MongoDB connection via reflection or driver
                Class<?> mongoClientClass = Class.forName("com.mongodb.client.MongoClients");
                java.lang.reflect.Method createMethod = mongoClientClass.getMethod("create", String.class);
                AutoCloseable client = (AutoCloseable) createMethod.invoke(null, profile.getJdbcUrl());
                try {
                    java.lang.reflect.Method listDbNames = client.getClass().getMethod("listDatabaseNames");
                    Object iterable = listDbNames.invoke(client);
                    java.lang.reflect.Method firstMethod = iterable.getClass().getMethod("first");
                    firstMethod.invoke(iterable);
                    long elapsed = System.currentTimeMillis() - start;
                    return new TestResult(true, "Connected successfully", elapsed, "MongoDB");
                } finally {
                    client.close();
                }
            } catch (Throwable ex) {
                long elapsed = System.currentTimeMillis() - start;
                String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                return new TestResult(false, msg != null ? msg : ex.toString(), elapsed, null);
            }
        }

        // Relational JDBC
        String url = profile.getJdbcUrl();
        String user = profile.getUser();
        String pass = profile.getPassword();

        try {
            ensureDriverLoaded(profile.getEngine());
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                long elapsed = System.currentTimeMillis() - start;
                DatabaseMetaData meta = conn.getMetaData();
                String product = meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion();
                return new TestResult(true, "Connected successfully", elapsed, product);
            }
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - start;
            String msg = ex.getMessage();
            return new TestResult(false, msg != null ? msg : ex.toString(), elapsed, null);
        }
    }

    public static Connection createConnection(DbConnectionProfile profile) throws SQLException {
        ensureDriverLoaded(profile.getEngine());
        return DriverManager.getConnection(profile.getJdbcUrl(), profile.getUser(), profile.getPassword());
    }

    public static List<SchemaInfo> loadSchemasAndTables(Connection conn, DbEngineType engine) throws SQLException {
        List<SchemaInfo> result = new ArrayList<>();
        DatabaseMetaData meta = conn.getMetaData();
        String currentCatalog = null;
        try {
            currentCatalog = conn.getCatalog();
        } catch (Throwable ignored) {}

        // 1. Gather schemas
        List<String> schemaNames = new ArrayList<>();
        try (ResultSet rs = meta.getSchemas()) {
            while (rs.next()) {
                String s = rs.getString("TABLE_SCHEM");
                if (s != null && !s.isBlank() && !isSystemSchema(s, engine)) {
                    schemaNames.add(s);
                }
            }
        } catch (Throwable ignored) {}

        if (schemaNames.isEmpty()) {
            if (currentCatalog != null && !currentCatalog.isBlank()) {
                schemaNames.add(currentCatalog);
            } else {
                schemaNames.add("default");
            }
        }

        for (String schema : schemaNames) {
            String schemaPattern = "default".equals(schema) ? null : schema;
            List<TableInfo> tables = new ArrayList<>();
            List<TableInfo> views = new ArrayList<>();

            try (ResultSet rs = meta.getTables(currentCatalog, schemaPattern, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    String tableType = rs.getString("TABLE_TYPE");
                    if ("VIEW".equalsIgnoreCase(tableType)) {
                        views.add(new TableInfo(tableName, "VIEW", Collections.emptyList()));
                    } else {
                        tables.add(new TableInfo(tableName, "TABLE", Collections.emptyList()));
                    }
                }
            } catch (Throwable ignored) {}

            result.add(new SchemaInfo(schema, tables, views));
        }

        return result;
    }

    public static List<ColumnInfo> loadColumns(Connection conn, String schema, String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        try {
            DatabaseMetaData meta = conn.getMetaData();
            String catalog = conn.getCatalog();
            String schemaPattern = "default".equals(schema) ? null : schema;

            Set<String> pks = new HashSet<>();
            try (ResultSet pkRs = meta.getPrimaryKeys(catalog, schemaPattern, tableName)) {
                while (pkRs.next()) {
                    String pkCol = pkRs.getString("COLUMN_NAME");
                    if (pkCol != null) pks.add(pkCol.toLowerCase());
                }
            } catch (Throwable ignored) {}

            try (ResultSet rs = meta.getColumns(catalog, schemaPattern, tableName, "%")) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    String typeName = rs.getString("TYPE_NAME");
                    int size = rs.getInt("COLUMN_SIZE");
                    boolean nullable = rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                    boolean isPk = pks.contains(colName.toLowerCase());
                    columns.add(new ColumnInfo(colName, typeName, size, nullable, isPk));
                }
            }
        } catch (Throwable ignored) {}
        return columns;
    }

    private static boolean isSystemSchema(String schema, DbEngineType engine) {
        String s = schema.toLowerCase();
        if (engine == DbEngineType.POSTGRESQL || engine == DbEngineType.STRATOSDB) {
            return s.startsWith("pg_") || s.equals("information_schema");
        }
        if (engine == DbEngineType.MYSQL || engine == DbEngineType.MARIADB) {
            return s.equals("information_schema") || s.equals("performance_schema") || s.equals("sys") || s.equals("mysql");
        }
        if (engine == DbEngineType.ORACLE) {
            return s.equals("sys") || s.equals("system") || s.equals("outln") || s.equals("xdb") || s.equals("ctxsys");
        }
        if (engine == DbEngineType.SQLSERVER) {
            return s.equals("sys") || s.equals("information_schema");
        }
        return false;
    }

    public static void ensureDriverLoaded(DbEngineType engine) {
        if (engine == null || engine.getDriverClassName() == null) return;
        try {
            Class.forName(engine.getDriverClassName());
        } catch (Throwable ignored) {}
    }
}
