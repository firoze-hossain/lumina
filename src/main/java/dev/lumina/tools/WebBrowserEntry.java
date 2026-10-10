package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Model representing a single browser entry in Tools > Web Browsers and Preview.
 */
public class WebBrowserEntry implements Cloneable {

    private String id = UUID.randomUUID().toString();
    private boolean active = true;
    private String name = "";
    private String family = "Chrome"; // Chrome, Firefox, Safari, Opera, Internet Explorer
    private String path = "";

    public WebBrowserEntry() {
    }

    public WebBrowserEntry(boolean active, String name, String family, String path) {
        this.active = active;
        this.name = name != null ? name : "";
        this.family = family != null ? family : "Chrome";
        this.path = path != null ? path : "";
    }

    public static List<WebBrowserEntry> defaultBrowsers() {
        List<WebBrowserEntry> list = new ArrayList<>();
        list.add(new WebBrowserEntry(true, "Chrome", "Chrome", "google-chrome"));
        list.add(new WebBrowserEntry(true, "Firefox", "Firefox", "firefox"));
        list.add(new WebBrowserEntry(false, "Safari", "Safari", ""));
        list.add(new WebBrowserEntry(false, "Opera", "Chrome", "opera"));
        list.add(new WebBrowserEntry(false, "Internet Explorer", "Internet Explorer", ""));
        list.add(new WebBrowserEntry(false, "Edge", "Chrome", "microsoft-edge"));
        return list;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id != null ? id : UUID.randomUUID().toString();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getFamily() {
        return family;
    }

    public void setFamily(String family) {
        this.family = family != null ? family : "Chrome";
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path : "";
    }

    @Override
    public WebBrowserEntry clone() {
        try {
            return (WebBrowserEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            WebBrowserEntry copy = new WebBrowserEntry();
            copy.id = this.id;
            copy.active = this.active;
            copy.name = this.name;
            copy.family = this.family;
            copy.path = this.path;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebBrowserEntry that = (WebBrowserEntry) o;
        return active == that.active &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(family, that.family) &&
                Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, active, name, family, path);
    }

    @Override
    public String toString() {
        return name + " (" + family + "): " + path;
    }
}
