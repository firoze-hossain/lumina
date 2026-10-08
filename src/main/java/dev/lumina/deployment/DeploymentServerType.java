package dev.lumina.deployment;

/**
 * Server types supported by Deployment configurations in IntelliJ IDEA.
 */
public enum DeploymentServerType {
    SFTP("SFTP", "SSH / SFTP Server"),
    FTP("FTP", "FTP Server"),
    FTPS("FTPS", "FTP over SSL / TLS"),
    WEBDAV("WebDAV", "WebDAV Server"),
    LOCAL("Local or mounted folder", "Local or mounted folder"),
    IN_PLACE("In place", "In place"),
    SERVER_GROUP("Server Group", "Group of deployment servers");

    private final String displayName;
    private final String description;

    DeploymentServerType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
