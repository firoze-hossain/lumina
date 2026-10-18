package dev.lumina.database;

/**
 * All database engines supported by Lumina, matching DBNavigator's full engine suite.
 */
public enum DbEngineType {
    POSTGRESQL("PostgreSQL", 5432, "org.postgresql.Driver", "postgres", true),
    MYSQL("MySQL", 3306, "com.mysql.cj.jdbc.Driver", "root", true),
    MARIADB("MariaDB", 3306, "org.mariadb.jdbc.Driver", "root", true),
    SQLITE("SQLite", 0, "org.sqlite.JDBC", "", true),
    SQLSERVER("SQL Server", 1433, "com.microsoft.sqlserver.jdbc.SQLServerDriver", "sa", true),
    ORACLE("Oracle", 1521, "oracle.jdbc.OracleDriver", "system", true),
    STRATOSDB("StratosDB", 6582, "com.stratosdb.jdbc.StratosDriver", "stratos", true),
    MONGODB("MongoDB", 27017, null, "", false);

    private final String displayName;
    private final int defaultPort;
    private final String driverClassName;
    private final String defaultUser;
    private final boolean relational;

    DbEngineType(String displayName, int defaultPort, String driverClassName, String defaultUser, boolean relational) {
        this.displayName = displayName;
        this.defaultPort = defaultPort;
        this.driverClassName = driverClassName;
        this.defaultUser = defaultUser;
        this.relational = relational;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultPort() {
        return defaultPort;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getDefaultUser() {
        return defaultUser;
    }

    public boolean isRelational() {
        return relational;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
