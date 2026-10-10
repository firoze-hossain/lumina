package dev.lumina.schemas;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > Schemas and DTDs > Remote JSON Schemas settings in Lumina IDE.
 * Matches reference screenshot:
 *  - Allow downloading JSON Schemas from remote sources (default: true)
 *  - Use schemastore.org JSON Schema catalog (default: true)
 *  - Always download the most recent version of schemas (default: false)
 *  - Configurable schema catalog URL (default: https://www.schemastore.org/api/json/catalog.json)
 */
public class RemoteJsonSchemasSettings implements Cloneable {

    public static final String DEFAULT_CATALOG_URL = "https://www.schemastore.org/api/json/catalog.json";

    private boolean allowDownloadRemoteSchemas = true;
    private boolean useSchemaStoreCatalog = true;
    private boolean alwaysDownloadMostRecentVersion = false;
    private String schemaStoreCatalogUrl = DEFAULT_CATALOG_URL;

    public RemoteJsonSchemasSettings() {
    }

    public RemoteJsonSchemasSettings(boolean allowDownloadRemoteSchemas,
                                      boolean useSchemaStoreCatalog,
                                      boolean alwaysDownloadMostRecentVersion) {
        this.allowDownloadRemoteSchemas = allowDownloadRemoteSchemas;
        this.useSchemaStoreCatalog = useSchemaStoreCatalog;
        this.alwaysDownloadMostRecentVersion = alwaysDownloadMostRecentVersion;
    }

    public RemoteJsonSchemasSettings(boolean allowDownloadRemoteSchemas,
                                      boolean useSchemaStoreCatalog,
                                      boolean alwaysDownloadMostRecentVersion,
                                      String schemaStoreCatalogUrl) {
        this.allowDownloadRemoteSchemas = allowDownloadRemoteSchemas;
        this.useSchemaStoreCatalog = useSchemaStoreCatalog;
        this.alwaysDownloadMostRecentVersion = alwaysDownloadMostRecentVersion;
        this.schemaStoreCatalogUrl = schemaStoreCatalogUrl != null ? schemaStoreCatalogUrl : DEFAULT_CATALOG_URL;
    }

    public RemoteJsonSchemasSettings(RemoteJsonSchemasSettings other) {
        if (other != null) {
            this.allowDownloadRemoteSchemas = other.allowDownloadRemoteSchemas;
            this.useSchemaStoreCatalog = other.useSchemaStoreCatalog;
            this.alwaysDownloadMostRecentVersion = other.alwaysDownloadMostRecentVersion;
            this.schemaStoreCatalogUrl = other.schemaStoreCatalogUrl != null ? other.schemaStoreCatalogUrl : DEFAULT_CATALOG_URL;
        }
    }

    public boolean isAllowDownloadRemoteSchemas() {
        return allowDownloadRemoteSchemas;
    }

    public void setAllowDownloadRemoteSchemas(boolean allowDownloadRemoteSchemas) {
        this.allowDownloadRemoteSchemas = allowDownloadRemoteSchemas;
    }

    public boolean isUseSchemaStoreCatalog() {
        return useSchemaStoreCatalog;
    }

    public void setUseSchemaStoreCatalog(boolean useSchemaStoreCatalog) {
        this.useSchemaStoreCatalog = useSchemaStoreCatalog;
    }

    public boolean isAlwaysDownloadMostRecentVersion() {
        return alwaysDownloadMostRecentVersion;
    }

    public void setAlwaysDownloadMostRecentVersion(boolean alwaysDownloadMostRecentVersion) {
        this.alwaysDownloadMostRecentVersion = alwaysDownloadMostRecentVersion;
    }

    public String getSchemaStoreCatalogUrl() {
        return schemaStoreCatalogUrl;
    }

    public void setSchemaStoreCatalogUrl(String schemaStoreCatalogUrl) {
        this.schemaStoreCatalogUrl = schemaStoreCatalogUrl != null ? schemaStoreCatalogUrl : DEFAULT_CATALOG_URL;
    }

    @Override
    public RemoteJsonSchemasSettings clone() {
        return new RemoteJsonSchemasSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteJsonSchemasSettings that = (RemoteJsonSchemasSettings) o;
        return allowDownloadRemoteSchemas == that.allowDownloadRemoteSchemas &&
                useSchemaStoreCatalog == that.useSchemaStoreCatalog &&
                alwaysDownloadMostRecentVersion == that.alwaysDownloadMostRecentVersion &&
                Objects.equals(schemaStoreCatalogUrl, that.schemaStoreCatalogUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowDownloadRemoteSchemas, useSchemaStoreCatalog,
                alwaysDownloadMostRecentVersion, schemaStoreCatalogUrl);
    }

    @Override
    public String toString() {
        return "RemoteJsonSchemasSettings{" +
                "allowDownloadRemoteSchemas=" + allowDownloadRemoteSchemas +
                ", useSchemaStoreCatalog=" + useSchemaStoreCatalog +
                ", alwaysDownloadMostRecentVersion=" + alwaysDownloadMostRecentVersion +
                ", schemaStoreCatalogUrl='" + schemaStoreCatalogUrl + '\'' +
                '}';
    }
}
