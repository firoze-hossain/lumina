package dev.lumina.scala;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > Scala > Worksheet settings.
 * Matches Image 3:
 *  - Treat .sc files as: default "Always Worksheet" ("Always Worksheet", "Always Ammonite")
 *  - Run worksheet in the compiler process (Plain mode only): default true
 *  - Use "eclipse compatibility" mode: default false
 *  - Treat Scala scratch files as worksheet files: default true
 *  - Collapse long output by default: default true
 *  - Output cutoff limit: default 35 lines
 *  - Delay before auto-run: default 1400 milliseconds
 */
public class ScalaWorksheetSettings {

    private String treatScFilesAs = "Always Worksheet";
    private boolean runWorksheetInCompilerProcessPlainMode = true;
    private boolean useEclipseCompatibilityMode = false;
    private boolean treatScalaScratchFilesAsWorksheet = true;
    private boolean collapseLongOutputByDefault = true;
    private int outputCutoffLimit = 35;
    private int delayBeforeAutoRunMs = 1400;

    public ScalaWorksheetSettings() {
    }

    public ScalaWorksheetSettings(String treatScFilesAs,
                                  boolean runWorksheetInCompilerProcessPlainMode,
                                  boolean useEclipseCompatibilityMode,
                                  boolean treatScalaScratchFilesAsWorksheet,
                                  boolean collapseLongOutputByDefault,
                                  int outputCutoffLimit,
                                  int delayBeforeAutoRunMs) {
        this.treatScFilesAs = treatScFilesAs;
        this.runWorksheetInCompilerProcessPlainMode = runWorksheetInCompilerProcessPlainMode;
        this.useEclipseCompatibilityMode = useEclipseCompatibilityMode;
        this.treatScalaScratchFilesAsWorksheet = treatScalaScratchFilesAsWorksheet;
        this.collapseLongOutputByDefault = collapseLongOutputByDefault;
        this.outputCutoffLimit = outputCutoffLimit;
        this.delayBeforeAutoRunMs = delayBeforeAutoRunMs;
    }

    public String getTreatScFilesAs() {
        return treatScFilesAs;
    }

    public void setTreatScFilesAs(String treatScFilesAs) {
        this.treatScFilesAs = treatScFilesAs;
    }

    public boolean isRunWorksheetInCompilerProcessPlainMode() {
        return runWorksheetInCompilerProcessPlainMode;
    }

    public void setRunWorksheetInCompilerProcessPlainMode(boolean runWorksheetInCompilerProcessPlainMode) {
        this.runWorksheetInCompilerProcessPlainMode = runWorksheetInCompilerProcessPlainMode;
    }

    public boolean isUseEclipseCompatibilityMode() {
        return useEclipseCompatibilityMode;
    }

    public void setUseEclipseCompatibilityMode(boolean useEclipseCompatibilityMode) {
        this.useEclipseCompatibilityMode = useEclipseCompatibilityMode;
    }

    public boolean isTreatScalaScratchFilesAsWorksheet() {
        return treatScalaScratchFilesAsWorksheet;
    }

    public void setTreatScalaScratchFilesAsWorksheet(boolean treatScalaScratchFilesAsWorksheet) {
        this.treatScalaScratchFilesAsWorksheet = treatScalaScratchFilesAsWorksheet;
    }

    public boolean isCollapseLongOutputByDefault() {
        return collapseLongOutputByDefault;
    }

    public void setCollapseLongOutputByDefault(boolean collapseLongOutputByDefault) {
        this.collapseLongOutputByDefault = collapseLongOutputByDefault;
    }

    public int getOutputCutoffLimit() {
        return outputCutoffLimit;
    }

    public void setOutputCutoffLimit(int outputCutoffLimit) {
        this.outputCutoffLimit = outputCutoffLimit;
    }

    public int getDelayBeforeAutoRunMs() {
        return delayBeforeAutoRunMs;
    }

    public void setDelayBeforeAutoRunMs(int delayBeforeAutoRunMs) {
        this.delayBeforeAutoRunMs = delayBeforeAutoRunMs;
    }

    public ScalaWorksheetSettings copy() {
        return new ScalaWorksheetSettings(
                treatScFilesAs,
                runWorksheetInCompilerProcessPlainMode,
                useEclipseCompatibilityMode,
                treatScalaScratchFilesAsWorksheet,
                collapseLongOutputByDefault,
                outputCutoffLimit,
                delayBeforeAutoRunMs
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaWorksheetSettings that = (ScalaWorksheetSettings) o;
        return runWorksheetInCompilerProcessPlainMode == that.runWorksheetInCompilerProcessPlainMode &&
                useEclipseCompatibilityMode == that.useEclipseCompatibilityMode &&
                treatScalaScratchFilesAsWorksheet == that.treatScalaScratchFilesAsWorksheet &&
                collapseLongOutputByDefault == that.collapseLongOutputByDefault &&
                outputCutoffLimit == that.outputCutoffLimit &&
                delayBeforeAutoRunMs == that.delayBeforeAutoRunMs &&
                Objects.equals(treatScFilesAs, that.treatScFilesAs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(treatScFilesAs, runWorksheetInCompilerProcessPlainMode,
                useEclipseCompatibilityMode, treatScalaScratchFilesAsWorksheet,
                collapseLongOutputByDefault, outputCutoffLimit, delayBeforeAutoRunMs);
    }

    @Override
    public String toString() {
        return "ScalaWorksheetSettings{" +
                "treatScFilesAs='" + treatScFilesAs + '\'' +
                ", runWorksheetInCompilerProcessPlainMode=" + runWorksheetInCompilerProcessPlainMode +
                ", useEclipseCompatibilityMode=" + useEclipseCompatibilityMode +
                ", treatScalaScratchFilesAsWorksheet=" + treatScalaScratchFilesAsWorksheet +
                ", collapseLongOutputByDefault=" + collapseLongOutputByDefault +
                ", outputCutoffLimit=" + outputCutoffLimit +
                ", delayBeforeAutoRunMs=" + delayBeforeAutoRunMs +
                '}';
    }
}
