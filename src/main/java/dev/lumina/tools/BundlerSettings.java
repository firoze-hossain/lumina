package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Bundler package manager configuration settings in Lumina IDE.
 */
public class BundlerSettings implements Cloneable {

    private boolean alwaysInstallRequiredVersion = true;
    private boolean useDefaultArguments = true;
    private String defaultArguments = "";

    public BundlerSettings() {
    }

    public boolean isAlwaysInstallRequiredVersion() {
        return alwaysInstallRequiredVersion;
    }

    public void setAlwaysInstallRequiredVersion(boolean alwaysInstallRequiredVersion) {
        this.alwaysInstallRequiredVersion = alwaysInstallRequiredVersion;
    }

    public boolean isUseDefaultArguments() {
        return useDefaultArguments;
    }

    public void setUseDefaultArguments(boolean useDefaultArguments) {
        this.useDefaultArguments = useDefaultArguments;
    }

    public String getDefaultArguments() {
        return defaultArguments;
    }

    public void setDefaultArguments(String defaultArguments) {
        this.defaultArguments = defaultArguments != null ? defaultArguments : "";
    }

    @Override
    public BundlerSettings clone() {
        try {
            return (BundlerSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BundlerSettings that = (BundlerSettings) o;
        return alwaysInstallRequiredVersion == that.alwaysInstallRequiredVersion &&
                useDefaultArguments == that.useDefaultArguments &&
                Objects.equals(defaultArguments, that.defaultArguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alwaysInstallRequiredVersion, useDefaultArguments, defaultArguments);
    }
}
