package dev.lumina.scala;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > Scala > Base Package settings.
 * Matches Image 4:
 *  - Radio: Inherit from Package Prefix of a Source Folder (default: true)
 *  - Radio: Use custom (default: false)
 *  - Table of custom base packages (Module, Base Package)
 */
public class ScalaBasePackageSettings {

    private boolean inheritFromPackagePrefix = true;
    private List<ScalaBasePackageEntry> customBasePackages = new ArrayList<>();

    public ScalaBasePackageSettings() {
    }

    public ScalaBasePackageSettings(boolean inheritFromPackagePrefix, List<ScalaBasePackageEntry> customBasePackages) {
        this.inheritFromPackagePrefix = inheritFromPackagePrefix;
        if (customBasePackages != null) {
            for (ScalaBasePackageEntry e : customBasePackages) {
                this.customBasePackages.add(e != null ? e.copy() : new ScalaBasePackageEntry());
            }
        }
    }

    public boolean isInheritFromPackagePrefix() {
        return inheritFromPackagePrefix;
    }

    public void setInheritFromPackagePrefix(boolean inheritFromPackagePrefix) {
        this.inheritFromPackagePrefix = inheritFromPackagePrefix;
    }

    public List<ScalaBasePackageEntry> getCustomBasePackages() {
        return customBasePackages;
    }

    public void setCustomBasePackages(List<ScalaBasePackageEntry> customBasePackages) {
        this.customBasePackages = new ArrayList<>();
        if (customBasePackages != null) {
            for (ScalaBasePackageEntry e : customBasePackages) {
                this.customBasePackages.add(e != null ? e.copy() : new ScalaBasePackageEntry());
            }
        }
    }

    public ScalaBasePackageSettings copy() {
        return new ScalaBasePackageSettings(inheritFromPackagePrefix, customBasePackages);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaBasePackageSettings that = (ScalaBasePackageSettings) o;
        return inheritFromPackagePrefix == that.inheritFromPackagePrefix &&
                Objects.equals(customBasePackages, that.customBasePackages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inheritFromPackagePrefix, customBasePackages);
    }

    @Override
    public String toString() {
        return "ScalaBasePackageSettings{" +
                "inheritFromPackagePrefix=" + inheritFromPackagePrefix +
                ", customBasePackages=" + customBasePackages +
                '}';
    }
}
