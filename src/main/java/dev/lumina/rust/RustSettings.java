package dev.lumina.rust;

import java.util.Objects;

/**
 * Model representing Rust configuration in Lumina IDE.
 * Covers Rust toolchain, External Linters, and Rustfmt.
 * Matches Images 3, 4, and 5.
 */
public class RustSettings {

    private String toolchainLocation = "";
    private String toolchainVersion = "";
    private String standardLibrary = "";
    private String environmentVariables = "";
    private boolean expandMacros = true;
    private boolean injectRustIntoDocComments = true;

    private ExternalLintersConfig externalLinters = new ExternalLintersConfig();
    private RustfmtConfig rustfmt = new RustfmtConfig();

    public RustSettings() {}

    public RustSettings(RustSettings other) {
        if (other == null) return;
        this.toolchainLocation = other.toolchainLocation;
        this.toolchainVersion = other.toolchainVersion;
        this.standardLibrary = other.standardLibrary;
        this.environmentVariables = other.environmentVariables;
        this.expandMacros = other.expandMacros;
        this.injectRustIntoDocComments = other.injectRustIntoDocComments;
        this.externalLinters = other.externalLinters != null ? other.externalLinters.copy() : new ExternalLintersConfig();
        this.rustfmt = other.rustfmt != null ? other.rustfmt.copy() : new RustfmtConfig();
    }

    public RustSettings copy() {
        return new RustSettings(this);
    }

    public static class ExternalLintersConfig {
        private boolean runOnTheFly = true;
        private String externalTool = "Cargo Check"; // "Cargo Check", "Clippy"
        private String additionalArguments = "";
        private String channel = "[default]"; // "[default]", "stable", "beta", "nightly"
        private String environmentVariables = "";

        public ExternalLintersConfig() {}

        public ExternalLintersConfig(ExternalLintersConfig other) {
            if (other == null) return;
            this.runOnTheFly = other.runOnTheFly;
            this.externalTool = other.externalTool;
            this.additionalArguments = other.additionalArguments;
            this.channel = other.channel;
            this.environmentVariables = other.environmentVariables;
        }

        public ExternalLintersConfig copy() {
            return new ExternalLintersConfig(this);
        }

        public boolean isRunOnTheFly() {
            return runOnTheFly;
        }

        public void setRunOnTheFly(boolean runOnTheFly) {
            this.runOnTheFly = runOnTheFly;
        }

        public String getExternalTool() {
            return externalTool;
        }

        public void setExternalTool(String externalTool) {
            this.externalTool = externalTool != null ? externalTool : "Cargo Check";
        }

        public String getAdditionalArguments() {
            return additionalArguments;
        }

        public void setAdditionalArguments(String additionalArguments) {
            this.additionalArguments = additionalArguments != null ? additionalArguments : "";
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel != null ? channel : "[default]";
        }

        public String getEnvironmentVariables() {
            return environmentVariables;
        }

        public void setEnvironmentVariables(String environmentVariables) {
            this.environmentVariables = environmentVariables != null ? environmentVariables : "";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ExternalLintersConfig that)) return false;
            return runOnTheFly == that.runOnTheFly &&
                    Objects.equals(externalTool, that.externalTool) &&
                    Objects.equals(additionalArguments, that.additionalArguments) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(environmentVariables, that.environmentVariables);
        }

        @Override
        public int hashCode() {
            return Objects.hash(runOnTheFly, externalTool, additionalArguments, channel, environmentVariables);
        }
    }

    public static class RustfmtConfig {
        private String additionalArguments = "";
        private String channel = "[default]";
        private String environmentVariables = "";
        private boolean useRustfmtInsteadOfBuiltIn = true;

        public RustfmtConfig() {}

        public RustfmtConfig(RustfmtConfig other) {
            if (other == null) return;
            this.additionalArguments = other.additionalArguments;
            this.channel = other.channel;
            this.environmentVariables = other.environmentVariables;
            this.useRustfmtInsteadOfBuiltIn = other.useRustfmtInsteadOfBuiltIn;
        }

        public RustfmtConfig copy() {
            return new RustfmtConfig(this);
        }

        public String getAdditionalArguments() {
            return additionalArguments;
        }

        public void setAdditionalArguments(String additionalArguments) {
            this.additionalArguments = additionalArguments != null ? additionalArguments : "";
        }

        public String getChannel() {
            return channel;
        }

        public void setChannel(String channel) {
            this.channel = channel != null ? channel : "[default]";
        }

        public String getEnvironmentVariables() {
            return environmentVariables;
        }

        public void setEnvironmentVariables(String environmentVariables) {
            this.environmentVariables = environmentVariables != null ? environmentVariables : "";
        }

        public boolean isUseRustfmtInsteadOfBuiltIn() {
            return useRustfmtInsteadOfBuiltIn;
        }

        public void setUseRustfmtInsteadOfBuiltIn(boolean useRustfmtInsteadOfBuiltIn) {
            this.useRustfmtInsteadOfBuiltIn = useRustfmtInsteadOfBuiltIn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RustfmtConfig that)) return false;
            return useRustfmtInsteadOfBuiltIn == that.useRustfmtInsteadOfBuiltIn &&
                    Objects.equals(additionalArguments, that.additionalArguments) &&
                    Objects.equals(channel, that.channel) &&
                    Objects.equals(environmentVariables, that.environmentVariables);
        }

        @Override
        public int hashCode() {
            return Objects.hash(additionalArguments, channel, environmentVariables, useRustfmtInsteadOfBuiltIn);
        }
    }

    public String getToolchainLocation() {
        return toolchainLocation;
    }

    public void setToolchainLocation(String toolchainLocation) {
        this.toolchainLocation = toolchainLocation != null ? toolchainLocation : "";
    }

    public String getToolchainVersion() {
        return toolchainVersion;
    }

    public void setToolchainVersion(String toolchainVersion) {
        this.toolchainVersion = toolchainVersion != null ? toolchainVersion : "";
    }

    public String getStandardLibrary() {
        return standardLibrary;
    }

    public void setStandardLibrary(String standardLibrary) {
        this.standardLibrary = standardLibrary != null ? standardLibrary : "";
    }

    public String getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(String environmentVariables) {
        this.environmentVariables = environmentVariables != null ? environmentVariables : "";
    }

    public boolean isExpandMacros() {
        return expandMacros;
    }

    public void setExpandMacros(boolean expandMacros) {
        this.expandMacros = expandMacros;
    }

    public boolean isInjectRustIntoDocComments() {
        return injectRustIntoDocComments;
    }

    public void setInjectRustIntoDocComments(boolean injectRustIntoDocComments) {
        this.injectRustIntoDocComments = injectRustIntoDocComments;
    }

    public ExternalLintersConfig getExternalLinters() {
        return externalLinters;
    }

    public void setExternalLinters(ExternalLintersConfig externalLinters) {
        this.externalLinters = externalLinters != null ? externalLinters : new ExternalLintersConfig();
    }

    public RustfmtConfig getRustfmt() {
        return rustfmt;
    }

    public void setRustfmt(RustfmtConfig rustfmt) {
        this.rustfmt = rustfmt != null ? rustfmt : new RustfmtConfig();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RustSettings that)) return false;
        return expandMacros == that.expandMacros &&
                injectRustIntoDocComments == that.injectRustIntoDocComments &&
                Objects.equals(toolchainLocation, that.toolchainLocation) &&
                Objects.equals(toolchainVersion, that.toolchainVersion) &&
                Objects.equals(standardLibrary, that.standardLibrary) &&
                Objects.equals(environmentVariables, that.environmentVariables) &&
                Objects.equals(externalLinters, that.externalLinters) &&
                Objects.equals(rustfmt, that.rustfmt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toolchainLocation, toolchainVersion, standardLibrary,
                environmentVariables, expandMacros, injectRustIntoDocComments,
                externalLinters, rustfmt);
    }
}
