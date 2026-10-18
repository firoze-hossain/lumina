package dev.lumina.database;

import java.io.File;
import java.util.UUID;

/**
 * A persistent database connection profile in Lumina, matching DBNavigator's full configuration suite.
 */
public class DbConnectionProfile {

    public enum OracleConnectionType { SID, SERVICE_NAME, TNS, URL_ONLY }
    public enum MongoConnectionType { DEFAULT, SRV, URL_ONLY }

    private String id = UUID.randomUUID().toString();
    private String name = "";
    private DbEngineType engine = DbEngineType.POSTGRESQL;
    private String host = "localhost";
    private int port = 5432;
    private String database = "postgres";
    private String user = "postgres";
    private String password = "";
    private boolean savePassword = true;
    private boolean useSsl = false;

    // Oracle specific
    private OracleConnectionType oracleConnectionType = OracleConnectionType.SERVICE_NAME;
    private String oracleSid = "XE";
    private String oracleConnectString = "";

    // Mongo specific
    private MongoConnectionType mongoConnectionType = MongoConnectionType.DEFAULT;
    private String replicaSet = "";
    private String mongoUrlOverride = "";

    public DbConnectionProfile() {
    }

    public DbConnectionProfile(DbEngineType engine) {
        this.engine = engine;
        this.port = engine.getDefaultPort();
        this.user = engine.getDefaultUser();
        this.name = engine.getDisplayName() + " - localhost";
        if (engine == DbEngineType.SQLITE) {
            this.database = "mydb.db";
            this.name = "SQLite - Local";
        } else if (engine == DbEngineType.MYSQL || engine == DbEngineType.MARIADB) {
            this.database = "";
        } else if (engine == DbEngineType.STRATOSDB) {
            this.database = "stratos";
        } else if (engine == DbEngineType.MONGODB) {
            this.database = "test";
        }
    }

    public DbConnectionProfile copy() {
        DbConnectionProfile c = new DbConnectionProfile();
        c.id = this.id;
        c.name = this.name;
        c.engine = this.engine;
        c.host = this.host;
        c.port = this.port;
        c.database = this.database;
        c.user = this.user;
        c.password = this.password;
        c.savePassword = this.savePassword;
        c.useSsl = this.useSsl;
        c.oracleConnectionType = this.oracleConnectionType;
        c.oracleSid = this.oracleSid;
        c.oracleConnectString = this.oracleConnectString;
        c.mongoConnectionType = this.mongoConnectionType;
        c.replicaSet = this.replicaSet;
        c.mongoUrlOverride = this.mongoUrlOverride;
        return c;
    }

    public String getJdbcUrl() {
        if (engine == null) return "";
        switch (engine) {
            case POSTGRESQL: {
                String db = database != null && !database.isBlank() ? "/" + database.trim() : "";
                String ssl = useSsl ? "?ssl=true" : "";
                return "jdbc:postgresql://" + host.trim() + ":" + port + db + ssl;
            }
            case MYSQL: {
                String db = database != null && !database.isBlank() ? "/" + database.trim() : "";
                String ssl = useSsl ? "?useSSL=true&allowPublicKeyRetrieval=true" : "?useSSL=false&allowPublicKeyRetrieval=true";
                return "jdbc:mysql://" + host.trim() + ":" + port + db + ssl;
            }
            case MARIADB: {
                String db = database != null && !database.isBlank() ? "/" + database.trim() : "";
                String ssl = useSsl ? "?useSsl=true" : "";
                return "jdbc:mariadb://" + host.trim() + ":" + port + db + ssl;
            }
            case SQLITE: {
                String path = database != null ? database.trim() : "database.db";
                return "jdbc:sqlite:" + path;
            }
            case SQLSERVER: {
                String db = database != null && !database.isBlank() ? ";databaseName=" + database.trim() : "";
                String ssl = useSsl ? ";encrypt=true;trustServerCertificate=false" : ";encrypt=false;trustServerCertificate=true";
                return "jdbc:sqlserver://" + host.trim() + ":" + port + db + ssl;
            }
            case ORACLE: {
                if (oracleConnectionType == OracleConnectionType.URL_ONLY && !oracleConnectString.isBlank()) {
                    return oracleConnectString.trim();
                }
                if (oracleConnectionType == OracleConnectionType.SID) {
                    String sid = oracleSid != null && !oracleSid.isBlank() ? oracleSid.trim() : "XE";
                    return "jdbc:oracle:thin:@" + host.trim() + ":" + port + ":" + sid;
                }
                String service = database != null && !database.isBlank() ? database.trim() : "ORCL";
                return "jdbc:oracle:thin:@//" + host.trim() + ":" + port + "/" + service;
            }
            case STRATOSDB: {
                String db = database != null && !database.isBlank() ? "/" + database.trim() : "";
                String ssl = useSsl ? "?ssl=true" : "";
                return "jdbc:stratosdb://" + host.trim() + ":" + port + db + ssl;
            }
            case MONGODB: {
                if (mongoConnectionType == MongoConnectionType.URL_ONLY && !mongoUrlOverride.isBlank()) {
                    return mongoUrlOverride.trim();
                }
                if (mongoConnectionType == MongoConnectionType.SRV) {
                    return "mongodb+srv://" + host.trim() + "/" + (database != null ? database.trim() : "");
                }
                return "mongodb://" + host.trim() + ":" + port + "/" + (database != null ? database.trim() : "");
            }
            default:
                return "jdbc:" + engine.name().toLowerCase() + "://" + host + ":" + port + "/" + database;
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name != null && !name.isBlank() ? name : (engine != null ? engine.getDisplayName() : "Data Source"); }
    public void setName(String name) { this.name = name; }

    public DbEngineType getEngine() { return engine != null ? engine : DbEngineType.POSTGRESQL; }
    public void setEngine(DbEngineType engine) { this.engine = engine; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port > 0 ? port : (engine != null ? engine.getDefaultPort() : 5432); }
    public void setPort(int port) { this.port = port; }

    public String getDatabase() { return database != null ? database : ""; }
    public void setDatabase(String database) { this.database = database; }

    public String getUser() { return user != null ? user : ""; }
    public void setUser(String user) { this.user = user; }

    public String getPassword() { return password != null ? password : ""; }
    public void setPassword(String password) { this.password = password; }

    public boolean isSavePassword() { return savePassword; }
    public void setSavePassword(boolean savePassword) { this.savePassword = savePassword; }

    public boolean isUseSsl() { return useSsl; }
    public void setUseSsl(boolean useSsl) { this.useSsl = useSsl; }

    public OracleConnectionType getOracleConnectionType() { return oracleConnectionType; }
    public void setOracleConnectionType(OracleConnectionType oracleConnectionType) { this.oracleConnectionType = oracleConnectionType; }

    public String getOracleSid() { return oracleSid; }
    public void setOracleSid(String oracleSid) { this.oracleSid = oracleSid; }

    public String getOracleConnectString() { return oracleConnectString; }
    public void setOracleConnectString(String oracleConnectString) { this.oracleConnectString = oracleConnectString; }

    public MongoConnectionType getMongoConnectionType() { return mongoConnectionType; }
    public void setMongoConnectionType(MongoConnectionType mongoConnectionType) { this.mongoConnectionType = mongoConnectionType; }

    public String getReplicaSet() { return replicaSet; }
    public void setReplicaSet(String replicaSet) { this.replicaSet = replicaSet; }

    public String getMongoUrlOverride() { return mongoUrlOverride; }
    public void setMongoUrlOverride(String mongoUrlOverride) { this.mongoUrlOverride = mongoUrlOverride; }
}
