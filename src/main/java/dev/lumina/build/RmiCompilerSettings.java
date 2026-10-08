package dev.lumina.build;

import java.util.Objects;

/**
 * Configuration model for RMI Compiler settings in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791449893875_0016cee0.png:
 *  - Enable RMI stubs generation
 *  - Generate IIOP stubs
 *  - Generate debugging info
 *  - Generate no warnings
 *  - Additional command line parameters
 */
public class RmiCompilerSettings implements Cloneable {

    private boolean enableRmiStubsGeneration = false;
    private boolean generateIiopStubs = false;
    private boolean generateDebuggingInfo = true;
    private boolean generateNoWarnings = false;
    private String additionalCommandLineParameters = "";

    public RmiCompilerSettings() {
    }

    public RmiCompilerSettings(RmiCompilerSettings other) {
        if (other != null) {
            this.enableRmiStubsGeneration = other.enableRmiStubsGeneration;
            this.generateIiopStubs = other.generateIiopStubs;
            this.generateDebuggingInfo = other.generateDebuggingInfo;
            this.generateNoWarnings = other.generateNoWarnings;
            this.additionalCommandLineParameters = other.additionalCommandLineParameters;
        }
    }

    public boolean isEnableRmiStubsGeneration() { return enableRmiStubsGeneration; }
    public void setEnableRmiStubsGeneration(boolean enableRmiStubsGeneration) {
        this.enableRmiStubsGeneration = enableRmiStubsGeneration;
    }

    public boolean isGenerateIiopStubs() { return generateIiopStubs; }
    public void setGenerateIiopStubs(boolean generateIiopStubs) {
        this.generateIiopStubs = generateIiopStubs;
    }

    public boolean isGenerateDebuggingInfo() { return generateDebuggingInfo; }
    public void setGenerateDebuggingInfo(boolean generateDebuggingInfo) {
        this.generateDebuggingInfo = generateDebuggingInfo;
    }

    public boolean isGenerateNoWarnings() { return generateNoWarnings; }
    public void setGenerateNoWarnings(boolean generateNoWarnings) {
        this.generateNoWarnings = generateNoWarnings;
    }

    public String getAdditionalCommandLineParameters() { return additionalCommandLineParameters; }
    public void setAdditionalCommandLineParameters(String additionalCommandLineParameters) {
        this.additionalCommandLineParameters = additionalCommandLineParameters != null ? additionalCommandLineParameters : "";
    }

    @Override
    public RmiCompilerSettings clone() {
        return new RmiCompilerSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RmiCompilerSettings that = (RmiCompilerSettings) o;
        return enableRmiStubsGeneration == that.enableRmiStubsGeneration &&
                generateIiopStubs == that.generateIiopStubs &&
                generateDebuggingInfo == that.generateDebuggingInfo &&
                generateNoWarnings == that.generateNoWarnings &&
                Objects.equals(additionalCommandLineParameters, that.additionalCommandLineParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableRmiStubsGeneration, generateIiopStubs, generateDebuggingInfo,
                generateNoWarnings, additionalCommandLineParameters);
    }
}
