package dev.lumina.schemas;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > Schemas and DTDs > XML Catalog settings in Lumina IDE.
 * Matches reference screenshot media_1791600352140_3eefd9d9.png:
 *  - Catalog property file: [ path field + browse button ]
 */
public class XmlCatalogSettings implements Cloneable {

    private String catalogPropertyFile = "";

    public XmlCatalogSettings() {
    }

    public XmlCatalogSettings(String catalogPropertyFile) {
        this.catalogPropertyFile = catalogPropertyFile != null ? catalogPropertyFile : "";
    }

    public XmlCatalogSettings(XmlCatalogSettings other) {
        if (other != null) {
            this.catalogPropertyFile = other.catalogPropertyFile != null ? other.catalogPropertyFile : "";
        }
    }

    public String getCatalogPropertyFile() {
        return catalogPropertyFile;
    }

    public void setCatalogPropertyFile(String catalogPropertyFile) {
        this.catalogPropertyFile = catalogPropertyFile != null ? catalogPropertyFile : "";
    }

    @Override
    public XmlCatalogSettings clone() {
        return new XmlCatalogSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        XmlCatalogSettings that = (XmlCatalogSettings) o;
        return Objects.equals(catalogPropertyFile, that.catalogPropertyFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(catalogPropertyFile);
    }

    @Override
    public String toString() {
        return "XmlCatalogSettings{" +
                "catalogPropertyFile='" + catalogPropertyFile + '\'' +
                '}';
    }
}
