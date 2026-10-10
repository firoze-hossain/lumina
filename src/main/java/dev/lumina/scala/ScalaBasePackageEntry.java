package dev.lumina.scala;

import java.util.Objects;

/**
 * Entry representing a module to base package mapping in Scala settings.
 */
public class ScalaBasePackageEntry {

    private String module;
    private String basePackage;

    public ScalaBasePackageEntry() {
        this("", "");
    }

    public ScalaBasePackageEntry(String module, String basePackage) {
        this.module = module != null ? module : "";
        this.basePackage = basePackage != null ? basePackage : "";
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module != null ? module : "";
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage != null ? basePackage : "";
    }

    public ScalaBasePackageEntry copy() {
        return new ScalaBasePackageEntry(module, basePackage);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaBasePackageEntry that = (ScalaBasePackageEntry) o;
        return Objects.equals(module, that.module) &&
                Objects.equals(basePackage, that.basePackage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module, basePackage);
    }

    @Override
    public String toString() {
        return "ScalaBasePackageEntry{" +
                "module='" + module + '\'' +
                ", basePackage='" + basePackage + '\'' +
                '}';
    }
}
