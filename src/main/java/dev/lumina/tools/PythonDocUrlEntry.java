package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing a single entry in Tools > Python External Documentation in Lumina IDE.
 */
public class PythonDocUrlEntry implements Cloneable {

    private String moduleName = "";
    private String urlPattern = "";

    public PythonDocUrlEntry() {
    }

    public PythonDocUrlEntry(String moduleName, String urlPattern) {
        this.moduleName = moduleName != null ? moduleName : "";
        this.urlPattern = urlPattern != null ? urlPattern : "";
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName != null ? moduleName : "";
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern != null ? urlPattern : "";
    }

    @Override
    public PythonDocUrlEntry clone() {
        try {
            return (PythonDocUrlEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            return new PythonDocUrlEntry(this.moduleName, this.urlPattern);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PythonDocUrlEntry that = (PythonDocUrlEntry) o;
        return Objects.equals(moduleName, that.moduleName) &&
                Objects.equals(urlPattern, that.urlPattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleName, urlPattern);
    }

    @Override
    public String toString() {
        return "PythonDocUrlEntry{" +
                "moduleName='" + moduleName + '\'' +
                ", urlPattern='" + urlPattern + '\'' +
                '}';
    }
}
