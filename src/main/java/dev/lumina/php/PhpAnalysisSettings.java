package dev.lumina.php;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Settings for PHP static analysis and exception inspection in Lumina IDE.
 */
public class PhpAnalysisSettings {

    private String callTreeAnalysisDepth = "1";
    private boolean skipCallsWithConstantParams = true;
    private List<String> uncheckedExceptions = new ArrayList<>(List.of(
            "\\RuntimeException",
            "\\LogicException",
            "\\Error"
    ));
    private String documentRoot = "$_SERVER['DOCUMENT_ROOT']";

    public PhpAnalysisSettings() {
    }

    public PhpAnalysisSettings(String callTreeAnalysisDepth, boolean skipCallsWithConstantParams,
                               List<String> uncheckedExceptions, String documentRoot) {
        this.callTreeAnalysisDepth = callTreeAnalysisDepth != null ? callTreeAnalysisDepth : "1";
        this.skipCallsWithConstantParams = skipCallsWithConstantParams;
        this.uncheckedExceptions = uncheckedExceptions != null ? new ArrayList<>(uncheckedExceptions) : new ArrayList<>();
        this.documentRoot = documentRoot != null ? documentRoot : "$_SERVER['DOCUMENT_ROOT']";
    }

    public PhpAnalysisSettings copy() {
        return new PhpAnalysisSettings(callTreeAnalysisDepth, skipCallsWithConstantParams,
                new ArrayList<>(uncheckedExceptions), documentRoot);
    }

    public String getCallTreeAnalysisDepth() {
        return callTreeAnalysisDepth;
    }

    public void setCallTreeAnalysisDepth(String callTreeAnalysisDepth) {
        this.callTreeAnalysisDepth = callTreeAnalysisDepth;
    }

    public boolean isSkipCallsWithConstantParams() {
        return skipCallsWithConstantParams;
    }

    public void setSkipCallsWithConstantParams(boolean skipCallsWithConstantParams) {
        this.skipCallsWithConstantParams = skipCallsWithConstantParams;
    }

    public List<String> getUncheckedExceptions() {
        return uncheckedExceptions;
    }

    public void setUncheckedExceptions(List<String> uncheckedExceptions) {
        this.uncheckedExceptions = uncheckedExceptions != null ? new ArrayList<>(uncheckedExceptions) : new ArrayList<>();
    }

    public String getDocumentRoot() {
        return documentRoot;
    }

    public void setDocumentRoot(String documentRoot) {
        this.documentRoot = documentRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhpAnalysisSettings that)) return false;
        return skipCallsWithConstantParams == that.skipCallsWithConstantParams &&
                Objects.equals(callTreeAnalysisDepth, that.callTreeAnalysisDepth) &&
                Objects.equals(uncheckedExceptions, that.uncheckedExceptions) &&
                Objects.equals(documentRoot, that.documentRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(callTreeAnalysisDepth, skipCallsWithConstantParams, uncheckedExceptions, documentRoot);
    }
}
