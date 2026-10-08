package dev.lumina.build;

import java.util.Objects;

/**
 * Model representing Python Debugger configuration in Lumina IDE.
 * Matches Build, Execution, Deployment > Python Debugger preferences.
 */
public class PythonDebuggerSettings implements Cloneable {

    private boolean attachToSubprocess = true;
    private boolean collectRunTimeTypes = false;
    private boolean geventCompatible = false;
    private boolean dropIntoDebuggerOnFailedTests = false;
    private boolean pyQtCompatible = true;
    private String pyQtBackend = "Auto";
    private String attachProcessFilter = "python";
    private int evalResponseTimeoutMs = 60000;

    public PythonDebuggerSettings() {
    }

    public PythonDebuggerSettings(PythonDebuggerSettings other) {
        if (other != null) {
            this.attachToSubprocess = other.attachToSubprocess;
            this.collectRunTimeTypes = other.collectRunTimeTypes;
            this.geventCompatible = other.geventCompatible;
            this.dropIntoDebuggerOnFailedTests = other.dropIntoDebuggerOnFailedTests;
            this.pyQtCompatible = other.pyQtCompatible;
            this.pyQtBackend = other.pyQtBackend != null ? other.pyQtBackend : "Auto";
            this.attachProcessFilter = other.attachProcessFilter != null ? other.attachProcessFilter : "python";
            this.evalResponseTimeoutMs = other.evalResponseTimeoutMs;
        }
    }

    public boolean isAttachToSubprocess() {
        return attachToSubprocess;
    }

    public void setAttachToSubprocess(boolean attachToSubprocess) {
        this.attachToSubprocess = attachToSubprocess;
    }

    public boolean isCollectRunTimeTypes() {
        return collectRunTimeTypes;
    }

    public void setCollectRunTimeTypes(boolean collectRunTimeTypes) {
        this.collectRunTimeTypes = collectRunTimeTypes;
    }

    public boolean isGeventCompatible() {
        return geventCompatible;
    }

    public void setGeventCompatible(boolean geventCompatible) {
        this.geventCompatible = geventCompatible;
    }

    public boolean isDropIntoDebuggerOnFailedTests() {
        return dropIntoDebuggerOnFailedTests;
    }

    public void setDropIntoDebuggerOnFailedTests(boolean dropIntoDebuggerOnFailedTests) {
        this.dropIntoDebuggerOnFailedTests = dropIntoDebuggerOnFailedTests;
    }

    public boolean isPyQtCompatible() {
        return pyQtCompatible;
    }

    public void setPyQtCompatible(boolean pyQtCompatible) {
        this.pyQtCompatible = pyQtCompatible;
    }

    public String getPyQtBackend() {
        return pyQtBackend;
    }

    public void setPyQtBackend(String pyQtBackend) {
        this.pyQtBackend = pyQtBackend != null ? pyQtBackend : "Auto";
    }

    public String getAttachProcessFilter() {
        return attachProcessFilter;
    }

    public void setAttachProcessFilter(String attachProcessFilter) {
        this.attachProcessFilter = attachProcessFilter != null ? attachProcessFilter : "python";
    }

    public int getEvalResponseTimeoutMs() {
        return evalResponseTimeoutMs;
    }

    public void setEvalResponseTimeoutMs(int evalResponseTimeoutMs) {
        this.evalResponseTimeoutMs = evalResponseTimeoutMs;
    }

    @Override
    public PythonDebuggerSettings clone() {
        return new PythonDebuggerSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PythonDebuggerSettings that = (PythonDebuggerSettings) o;
        return attachToSubprocess == that.attachToSubprocess &&
                collectRunTimeTypes == that.collectRunTimeTypes &&
                geventCompatible == that.geventCompatible &&
                dropIntoDebuggerOnFailedTests == that.dropIntoDebuggerOnFailedTests &&
                pyQtCompatible == that.pyQtCompatible &&
                evalResponseTimeoutMs == that.evalResponseTimeoutMs &&
                Objects.equals(pyQtBackend, that.pyQtBackend) &&
                Objects.equals(attachProcessFilter, that.attachProcessFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attachToSubprocess, collectRunTimeTypes, geventCompatible,
                dropIntoDebuggerOnFailedTests, pyQtCompatible, pyQtBackend,
                attachProcessFilter, evalResponseTimeoutMs);
    }
}
