package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Shared Indexes configuration settings in Lumina IDE.
 */
public class SharedIndexesSettings implements Cloneable {

    public static final String MODE_MANUAL = "MANUAL";
    public static final String MODE_AUTO_DOWNLOAD = "AUTO_DOWNLOAD";
    public static final String MODE_CUSTOM_SERVER = "CUSTOM_SERVER";

    private String downloadMode = MODE_MANUAL;
    private String customServerUrl = "";
    private String localCacheDirectory = "";
    private boolean downloadJdkIndexes = false;
    private boolean downloadMavenIndexes = false;

    public SharedIndexesSettings() {
    }

    public String getDownloadMode() {
        return downloadMode != null ? downloadMode : MODE_MANUAL;
    }

    public void setDownloadMode(String downloadMode) {
        this.downloadMode = downloadMode != null ? downloadMode : MODE_MANUAL;
    }

    public String getCustomServerUrl() {
        return customServerUrl != null ? customServerUrl : "";
    }

    public void setCustomServerUrl(String customServerUrl) {
        this.customServerUrl = customServerUrl != null ? customServerUrl.trim() : "";
    }

    public String getLocalCacheDirectory() {
        return localCacheDirectory != null ? localCacheDirectory : "";
    }

    public void setLocalCacheDirectory(String localCacheDirectory) {
        this.localCacheDirectory = localCacheDirectory != null ? localCacheDirectory.trim() : "";
    }

    public boolean isDownloadJdkIndexes() {
        return downloadJdkIndexes;
    }

    public void setDownloadJdkIndexes(boolean downloadJdkIndexes) {
        this.downloadJdkIndexes = downloadJdkIndexes;
    }

    public boolean isDownloadMavenIndexes() {
        return downloadMavenIndexes;
    }

    public void setDownloadMavenIndexes(boolean downloadMavenIndexes) {
        this.downloadMavenIndexes = downloadMavenIndexes;
    }

    @Override
    public SharedIndexesSettings clone() {
        try {
            return (SharedIndexesSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            SharedIndexesSettings copy = new SharedIndexesSettings();
            copy.downloadMode = this.downloadMode;
            copy.customServerUrl = this.customServerUrl;
            copy.localCacheDirectory = this.localCacheDirectory;
            copy.downloadJdkIndexes = this.downloadJdkIndexes;
            copy.downloadMavenIndexes = this.downloadMavenIndexes;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SharedIndexesSettings that = (SharedIndexesSettings) o;
        return downloadJdkIndexes == that.downloadJdkIndexes &&
                downloadMavenIndexes == that.downloadMavenIndexes &&
                Objects.equals(downloadMode, that.downloadMode) &&
                Objects.equals(customServerUrl, that.customServerUrl) &&
                Objects.equals(localCacheDirectory, that.localCacheDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(downloadMode, customServerUrl, localCacheDirectory, downloadJdkIndexes, downloadMavenIndexes);
    }
}
