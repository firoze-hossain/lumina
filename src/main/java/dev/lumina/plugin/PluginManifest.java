package dev.lumina.plugin;

import java.util.List;

/**
 * Plugin metadata for language/generator plugins.
 */
public class PluginManifest {
    private String id;
    private String name;
    private String version;
    private String description;
    private String vendor;
    private String category;
    private String generatorClass;
    private List<String> tags;
    private String homepage;
    private String icon;
    private String rating;
    private int downloads;
    private boolean installed;
    private boolean builtIn;
    private String color;

    public PluginManifest() {}

    public PluginManifest(String id, String name, String version, String description,
                          String vendor, String category, String generatorClass,
                          List<String> tags, String homepage, String icon,
                          String rating, int downloads, boolean installed, 
                          boolean builtIn, String color) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.description = description;
        this.vendor = vendor;
        this.category = category;
        this.generatorClass = generatorClass;
        this.tags = tags;
        this.homepage = homepage;
        this.icon = icon;
        this.rating = rating;
        this.downloads = downloads;
        this.installed = installed;
        this.builtIn = builtIn;
        this.color = color;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getGeneratorClass() { return generatorClass; }
    public void setGeneratorClass(String generatorClass) { this.generatorClass = generatorClass; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }
    public int getDownloads() { return downloads; }
    public void setDownloads(int downloads) { this.downloads = downloads; }
    public boolean isInstalled() { return installed; }
    public void setInstalled(boolean installed) { this.installed = installed; }
    public boolean isBuiltIn() { return builtIn; }
    public void setBuiltIn(boolean builtIn) { this.builtIn = builtIn; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}