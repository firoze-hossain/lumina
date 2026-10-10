package dev.lumina.schemas;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > Schemas and DTDs > Default XML Schemas settings.
 * Matches Image 4:
 *  - Default HTML language level:
 *    - HTML 4 ("http://www.w3.org/TR/html4/loose.dtd")
 *    - HTML 5 (default)
 *    - Other doctype:
 *  - XML Schema version:
 *    - XML Schema 1.0 (default)
 *    - XML Schema 1.1
 */
public class DefaultXmlSchemasSettings {

    public static final String HTML_4 = "HTML 4 (\"http://www.w3.org/TR/html4/loose.dtd\")";
    public static final String HTML_5 = "HTML 5";
    public static final String HTML_OTHER = "Other doctype:";

    public static final String XML_SCHEMA_1_0 = "XML Schema 1.0";
    public static final String XML_SCHEMA_1_1 = "XML Schema 1.1";

    private String htmlLanguageLevel = HTML_5;
    private String otherDoctype = "";
    private String xmlSchemaVersion = XML_SCHEMA_1_0;

    public DefaultXmlSchemasSettings() {
    }

    public DefaultXmlSchemasSettings(String htmlLanguageLevel, String otherDoctype, String xmlSchemaVersion) {
        this.htmlLanguageLevel = htmlLanguageLevel != null ? htmlLanguageLevel : HTML_5;
        this.otherDoctype = otherDoctype != null ? otherDoctype : "";
        this.xmlSchemaVersion = xmlSchemaVersion != null ? xmlSchemaVersion : XML_SCHEMA_1_0;
    }

    public String getHtmlLanguageLevel() {
        return htmlLanguageLevel;
    }

    public void setHtmlLanguageLevel(String htmlLanguageLevel) {
        this.htmlLanguageLevel = htmlLanguageLevel != null ? htmlLanguageLevel : HTML_5;
    }

    public String getOtherDoctype() {
        return otherDoctype;
    }

    public void setOtherDoctype(String otherDoctype) {
        this.otherDoctype = otherDoctype != null ? otherDoctype : "";
    }

    public String getXmlSchemaVersion() {
        return xmlSchemaVersion;
    }

    public void setXmlSchemaVersion(String xmlSchemaVersion) {
        this.xmlSchemaVersion = xmlSchemaVersion != null ? xmlSchemaVersion : XML_SCHEMA_1_0;
    }

    public DefaultXmlSchemasSettings copy() {
        return new DefaultXmlSchemasSettings(htmlLanguageLevel, otherDoctype, xmlSchemaVersion);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultXmlSchemasSettings that = (DefaultXmlSchemasSettings) o;
        return Objects.equals(htmlLanguageLevel, that.htmlLanguageLevel) &&
                Objects.equals(otherDoctype, that.otherDoctype) &&
                Objects.equals(xmlSchemaVersion, that.xmlSchemaVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(htmlLanguageLevel, otherDoctype, xmlSchemaVersion);
    }

    @Override
    public String toString() {
        return "DefaultXmlSchemasSettings{" +
                "htmlLanguageLevel='" + htmlLanguageLevel + '\'' +
                ", otherDoctype='" + otherDoctype + '\'' +
                ", xmlSchemaVersion='" + xmlSchemaVersion + '\'' +
                '}';
    }
}
