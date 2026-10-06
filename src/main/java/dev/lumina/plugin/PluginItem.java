package dev.lumina.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Rich model representing an IDE plugin (both marketplace and installed).
 * Contains complete metadata for rendering modern plugin cards and details.
 */
public class PluginItem {
    private String id;
    private String name;
    private String version;
    private String availableVersion; // Non-null if update is available
    private String vendor;
    private String vendorUrl;
    private String homepageUrl;
    private String downloads;
    private String rating;
    private List<String> tags = new ArrayList<>();
    private boolean freemium;
    private boolean installed;
    private boolean enabled = true;
    private boolean bundled;
    private String marketplaceSection; // "Staff Picks", "New and Updated", etc.
    private String termsNotice;
    private String shortDescription;
    private String descriptionHeading;
    private String fullDescription;
    private List<String> features = new ArrayList<>();
    private String whatsNew;
    private List<ReviewItem> reviews = new ArrayList<>();
    private String size = "12.4 MB";
    private String license = "Commercial / Proprietary";
    private String compatibleVersions = "Lumina IDE 2024.1 — 2025.3+";
    private String heroTitle;
    private String heroSubhead;
    private List<CarouselSlide> carouselSlides = new ArrayList<>();
    private String iconSymbol;
    private String iconBgColor;
    private String downloadUrl;
    private String sha256;
    private String packageType = "jar";
    private String fileName;
    private boolean fromRozeHub;
    private boolean pendingRestart;

    public PluginItem() {}

    public PluginItem(String id, String name, String version, String vendor) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.vendor = vendor;
    }

    public static class ReviewItem {
        private final String author;
        private final double rating;
        private final String date;
        private final String comment;

        public ReviewItem(String author, double rating, String date, String comment) {
            this.author = author;
            this.rating = rating;
            this.date = date;
            this.comment = comment;
        }

        public String getAuthor() { return author; }
        public double getRating() { return rating; }
        public String getDate() { return date; }
        public String getComment() { return comment; }
    }

    public static class CarouselSlide {
        private final String title;
        private final String subtitle;
        private final String visualType;

        public CarouselSlide(String title, String subtitle, String visualType) {
            this.title = title;
            this.subtitle = subtitle;
            this.visualType = visualType;
        }

        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getVisualType() { return visualType; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getAvailableVersion() { return availableVersion; }
    public void setAvailableVersion(String availableVersion) { this.availableVersion = availableVersion; }

    public boolean hasUpdate() {
        return availableVersion != null && !availableVersion.isBlank() && !availableVersion.equals(version);
    }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getVendorUrl() { return vendorUrl; }
    public void setVendorUrl(String vendorUrl) { this.vendorUrl = vendorUrl; }

    public String getHomepageUrl() {
        if (homepageUrl != null && !homepageUrl.isBlank() && !homepageUrl.contains("lumina.dev/plugins")) {
            return homepageUrl;
        }
        return RozeHubClient.getInstance().getBaseUrl() + "/marketplace/" + (id != null ? id : "");
    }
    public void setHomepageUrl(String homepageUrl) { this.homepageUrl = homepageUrl; }

    public String getDownloads() { return downloads != null ? downloads : "1.0M"; }
    public void setDownloads(String downloads) { this.downloads = downloads; }

    public String getRating() { return rating != null ? rating : "4.5"; }
    public void setRating(String rating) { this.rating = rating; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>(); }

    public boolean isFreemium() { return freemium; }
    public void setFreemium(boolean freemium) { this.freemium = freemium; }

    public boolean isInstalled() { return installed; }
    public void setInstalled(boolean installed) { this.installed = installed; }

    public boolean isPendingRestart() { return pendingRestart; }
    public void setPendingRestart(boolean pendingRestart) { this.pendingRestart = pendingRestart; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isBundled() { return bundled; }
    public void setBundled(boolean bundled) { this.bundled = bundled; }

    public String getMarketplaceSection() { return marketplaceSection; }
    public void setMarketplaceSection(String marketplaceSection) { this.marketplaceSection = marketplaceSection; }

    public String getTermsNotice() { return termsNotice; }
    public void setTermsNotice(String termsNotice) { this.termsNotice = termsNotice; }

    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }

    public String getDescriptionHeading() { return descriptionHeading; }
    public void setDescriptionHeading(String descriptionHeading) { this.descriptionHeading = descriptionHeading; }

    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }

    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features != null ? new ArrayList<>(features) : new ArrayList<>(); }

    public String getWhatsNew() { return whatsNew; }
    public void setWhatsNew(String whatsNew) { this.whatsNew = whatsNew; }

    public List<ReviewItem> getReviews() { return reviews; }
    public void setReviews(List<ReviewItem> reviews) { this.reviews = reviews != null ? new ArrayList<>(reviews) : new ArrayList<>(); }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }

    public String getCompatibleVersions() { return compatibleVersions; }
    public void setCompatibleVersions(String compatibleVersions) { this.compatibleVersions = compatibleVersions; }

    public String getHeroTitle() { return heroTitle; }
    public void setHeroTitle(String heroTitle) { this.heroTitle = heroTitle; }

    public String getHeroSubhead() { return heroSubhead; }
    public void setHeroSubhead(String heroSubhead) { this.heroSubhead = heroSubhead; }

    public List<CarouselSlide> getCarouselSlides() { return carouselSlides; }
    public void setCarouselSlides(List<CarouselSlide> carouselSlides) {
        this.carouselSlides = carouselSlides != null ? new ArrayList<>(carouselSlides) : new ArrayList<>();
    }

    public String getIconSymbol() { return iconSymbol; }
    public void setIconSymbol(String iconSymbol) { this.iconSymbol = iconSymbol; }

    public String getIconBgColor() { return iconBgColor; }
    public void setIconBgColor(String iconBgColor) { this.iconBgColor = iconBgColor; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public String getPackageType() { return packageType != null ? packageType : "jar"; }
    public void setPackageType(String packageType) { this.packageType = packageType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public boolean isFromRozeHub() { return fromRozeHub; }
    public void setFromRozeHub(boolean fromRozeHub) { this.fromRozeHub = fromRozeHub; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PluginItem that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
