package dev.lumina.scala;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model for Languages & Frameworks > Scala > Extensions settings.
 * Matches Image 2:
 *  - Enable loading external extensions (default: true)
 *  - Known extension libraries (list of libraries)
 */
public class ScalaExtensionsSettings {

    private boolean enableLoadingExternalExtensions = true;
    private List<ScalaExtensionLibrary> knownLibraries = new ArrayList<>();

    public ScalaExtensionsSettings() {
    }

    public ScalaExtensionsSettings(boolean enableLoadingExternalExtensions, List<ScalaExtensionLibrary> knownLibraries) {
        this.enableLoadingExternalExtensions = enableLoadingExternalExtensions;
        if (knownLibraries != null) {
            for (ScalaExtensionLibrary lib : knownLibraries) {
                this.knownLibraries.add(lib != null ? lib.copy() : new ScalaExtensionLibrary());
            }
        }
    }

    public boolean isEnableLoadingExternalExtensions() {
        return enableLoadingExternalExtensions;
    }

    public void setEnableLoadingExternalExtensions(boolean enableLoadingExternalExtensions) {
        this.enableLoadingExternalExtensions = enableLoadingExternalExtensions;
    }

    public List<ScalaExtensionLibrary> getKnownLibraries() {
        return knownLibraries;
    }

    public void setKnownLibraries(List<ScalaExtensionLibrary> knownLibraries) {
        this.knownLibraries = new ArrayList<>();
        if (knownLibraries != null) {
            for (ScalaExtensionLibrary lib : knownLibraries) {
                this.knownLibraries.add(lib != null ? lib.copy() : new ScalaExtensionLibrary());
            }
        }
    }

    public ScalaExtensionsSettings copy() {
        return new ScalaExtensionsSettings(enableLoadingExternalExtensions, knownLibraries);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaExtensionsSettings that = (ScalaExtensionsSettings) o;
        return enableLoadingExternalExtensions == that.enableLoadingExternalExtensions &&
                Objects.equals(knownLibraries, that.knownLibraries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableLoadingExternalExtensions, knownLibraries);
    }

    @Override
    public String toString() {
        return "ScalaExtensionsSettings{" +
                "enableLoadingExternalExtensions=" + enableLoadingExternalExtensions +
                ", knownLibraries=" + knownLibraries +
                '}';
    }
}
