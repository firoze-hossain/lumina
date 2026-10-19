package dev.lumina.update;

import java.util.Locale;

public final class AppUpdate {
    public String project = "lumina";
    public String currentVersion;
    public boolean available;
    public boolean mandatory;
    public String channel = "Stable";
    public String platform;
    public String architecture;
    public String latestVersion;
    public String source = "website"; // "website" or "github"
    public String htmlUrl;
    public Release release;

    public static final class Release {
        public long id;
        public String version;
        public String channel;
        public String notes;
        public boolean mandatory;
        public String htmlUrl;
        public String fileName;
        public long fileSize;
        public String sha256;
        public String downloadUrl;
        public String publishedAt;

        public String effectiveFileName() {
            return fileName != null && !fileName.isBlank() ? fileName : "lumina-update.bin";
        }

        public long effectiveFileSize() {
            return fileSize;
        }

        public String effectiveSha256() {
            return sha256;
        }

        public String effectiveDownloadUrl() {
            return downloadUrl != null && !downloadUrl.isBlank() ? downloadUrl : htmlUrl;
        }

        public String displayNotes() {
            return notes == null || notes.isBlank() ? "General improvements and bug fixes." : notes;
        }

        public String effectivePackageType() {
            String fn = effectiveFileName().toLowerCase(Locale.ROOT);
            if (fn.endsWith(".deb")) return "deb";
            if (fn.endsWith(".msi")) return "msi";
            if (fn.endsWith(".exe")) return "exe";
            if (fn.endsWith(".dmg")) return "dmg";
            if (fn.endsWith(".pkg")) return "pkg";
            if (fn.endsWith(".tar.gz") || fn.endsWith(".tgz")) return "tar.gz";
            if (fn.endsWith(".zip")) return "zip";
            return "unknown";
        }
    }
}
