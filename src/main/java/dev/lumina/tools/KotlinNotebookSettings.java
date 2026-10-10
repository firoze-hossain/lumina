package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Kotlin Notebook configuration settings in Lumina IDE.
 * Fully configurable and observable without hardcoded values.
 */
public class KotlinNotebookSettings implements Cloneable {

    public static final String DEFAULT_KERNEL_VERSION = "0.15.1-761-1 (bundled)";
    public static final String DEFAULT_JDK_PATH = "Project SDK 25";
    public static final String DEFAULT_JVM_TARGET = "Selected JDK default";
    public static final int DEFAULT_MAX_HEAP_SIZE = 3256;

    // JVM and Build
    private String kernelVersion = DEFAULT_KERNEL_VERSION;
    private String jdkPath = DEFAULT_JDK_PATH;
    private String jvmTargetForSnippets = DEFAULT_JVM_TARGET;
    private int maxHeapSize = DEFAULT_MAX_HEAP_SIZE;
    private String jvmExtraArguments = "";
    private String environmentVariables = "";

    // Debug Options
    private boolean showNotebookSessionVariables = false;
    private boolean openVariablesTabAfterCellExecution = false;

    // Kernel Session
    private boolean stopExecutionOnFailure = true;
    private boolean resolveSources = true;
    private boolean resolveMultiplatformDependencies = false;

    // Outputs
    private boolean renderKandyPlotsNatively = true;
    private boolean renderDataFrameTablesNatively = true;

    // Type Hints
    private boolean showTypeHintsOnlyInActiveCell = false;

    // Appearance
    private boolean displayExecutionCount = true;
    private boolean showFoldableRegions = true;

    public KotlinNotebookSettings() {
    }

    public String getKernelVersion() {
        return kernelVersion;
    }

    public void setKernelVersion(String kernelVersion) {
        this.kernelVersion = kernelVersion != null ? kernelVersion : DEFAULT_KERNEL_VERSION;
    }

    public String getJdkPath() {
        return jdkPath;
    }

    public void setJdkPath(String jdkPath) {
        this.jdkPath = jdkPath != null ? jdkPath : DEFAULT_JDK_PATH;
    }

    public String getJvmTargetForSnippets() {
        return jvmTargetForSnippets;
    }

    public void setJvmTargetForSnippets(String jvmTargetForSnippets) {
        this.jvmTargetForSnippets = jvmTargetForSnippets != null ? jvmTargetForSnippets : DEFAULT_JVM_TARGET;
    }

    public int getMaxHeapSize() {
        return maxHeapSize;
    }

    public void setMaxHeapSize(int maxHeapSize) {
        this.maxHeapSize = maxHeapSize > 0 ? maxHeapSize : DEFAULT_MAX_HEAP_SIZE;
    }

    public String getJvmExtraArguments() {
        return jvmExtraArguments;
    }

    public void setJvmExtraArguments(String jvmExtraArguments) {
        this.jvmExtraArguments = jvmExtraArguments != null ? jvmExtraArguments : "";
    }

    public String getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(String environmentVariables) {
        this.environmentVariables = environmentVariables != null ? environmentVariables : "";
    }

    public boolean isShowNotebookSessionVariables() {
        return showNotebookSessionVariables;
    }

    public void setShowNotebookSessionVariables(boolean showNotebookSessionVariables) {
        this.showNotebookSessionVariables = showNotebookSessionVariables;
    }

    public boolean isOpenVariablesTabAfterCellExecution() {
        return openVariablesTabAfterCellExecution;
    }

    public void setOpenVariablesTabAfterCellExecution(boolean openVariablesTabAfterCellExecution) {
        this.openVariablesTabAfterCellExecution = openVariablesTabAfterCellExecution;
    }

    public boolean isStopExecutionOnFailure() {
        return stopExecutionOnFailure;
    }

    public void setStopExecutionOnFailure(boolean stopExecutionOnFailure) {
        this.stopExecutionOnFailure = stopExecutionOnFailure;
    }

    public boolean isResolveSources() {
        return resolveSources;
    }

    public void setResolveSources(boolean resolveSources) {
        this.resolveSources = resolveSources;
    }

    public boolean isResolveMultiplatformDependencies() {
        return resolveMultiplatformDependencies;
    }

    public void setResolveMultiplatformDependencies(boolean resolveMultiplatformDependencies) {
        this.resolveMultiplatformDependencies = resolveMultiplatformDependencies;
    }

    public boolean isRenderKandyPlotsNatively() {
        return renderKandyPlotsNatively;
    }

    public void setRenderKandyPlotsNatively(boolean renderKandyPlotsNatively) {
        this.renderKandyPlotsNatively = renderKandyPlotsNatively;
    }

    public boolean isRenderDataFrameTablesNatively() {
        return renderDataFrameTablesNatively;
    }

    public void setRenderDataFrameTablesNatively(boolean renderDataFrameTablesNatively) {
        this.renderDataFrameTablesNatively = renderDataFrameTablesNatively;
    }

    public boolean isShowTypeHintsOnlyInActiveCell() {
        return showTypeHintsOnlyInActiveCell;
    }

    public void setShowTypeHintsOnlyInActiveCell(boolean showTypeHintsOnlyInActiveCell) {
        this.showTypeHintsOnlyInActiveCell = showTypeHintsOnlyInActiveCell;
    }

    public boolean isDisplayExecutionCount() {
        return displayExecutionCount;
    }

    public void setDisplayExecutionCount(boolean displayExecutionCount) {
        this.displayExecutionCount = displayExecutionCount;
    }

    public boolean isShowFoldableRegions() {
        return showFoldableRegions;
    }

    public void setShowFoldableRegions(boolean showFoldableRegions) {
        this.showFoldableRegions = showFoldableRegions;
    }

    public KotlinNotebookSettings copy() {
        return clone();
    }

    @Override
    public KotlinNotebookSettings clone() {
        try {
            return (KotlinNotebookSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            KotlinNotebookSettings copy = new KotlinNotebookSettings();
            copy.kernelVersion = this.kernelVersion;
            copy.jdkPath = this.jdkPath;
            copy.jvmTargetForSnippets = this.jvmTargetForSnippets;
            copy.maxHeapSize = this.maxHeapSize;
            copy.jvmExtraArguments = this.jvmExtraArguments;
            copy.environmentVariables = this.environmentVariables;
            copy.showNotebookSessionVariables = this.showNotebookSessionVariables;
            copy.openVariablesTabAfterCellExecution = this.openVariablesTabAfterCellExecution;
            copy.stopExecutionOnFailure = this.stopExecutionOnFailure;
            copy.resolveSources = this.resolveSources;
            copy.resolveMultiplatformDependencies = this.resolveMultiplatformDependencies;
            copy.renderKandyPlotsNatively = this.renderKandyPlotsNatively;
            copy.renderDataFrameTablesNatively = this.renderDataFrameTablesNatively;
            copy.showTypeHintsOnlyInActiveCell = this.showTypeHintsOnlyInActiveCell;
            copy.displayExecutionCount = this.displayExecutionCount;
            copy.showFoldableRegions = this.showFoldableRegions;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KotlinNotebookSettings that = (KotlinNotebookSettings) o;
        return maxHeapSize == that.maxHeapSize &&
                showNotebookSessionVariables == that.showNotebookSessionVariables &&
                openVariablesTabAfterCellExecution == that.openVariablesTabAfterCellExecution &&
                stopExecutionOnFailure == that.stopExecutionOnFailure &&
                resolveSources == that.resolveSources &&
                resolveMultiplatformDependencies == that.resolveMultiplatformDependencies &&
                renderKandyPlotsNatively == that.renderKandyPlotsNatively &&
                renderDataFrameTablesNatively == that.renderDataFrameTablesNatively &&
                showTypeHintsOnlyInActiveCell == that.showTypeHintsOnlyInActiveCell &&
                displayExecutionCount == that.displayExecutionCount &&
                showFoldableRegions == that.showFoldableRegions &&
                Objects.equals(kernelVersion, that.kernelVersion) &&
                Objects.equals(jdkPath, that.jdkPath) &&
                Objects.equals(jvmTargetForSnippets, that.jvmTargetForSnippets) &&
                Objects.equals(jvmExtraArguments, that.jvmExtraArguments) &&
                Objects.equals(environmentVariables, that.environmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kernelVersion, jdkPath, jvmTargetForSnippets, maxHeapSize,
                jvmExtraArguments, environmentVariables, showNotebookSessionVariables,
                openVariablesTabAfterCellExecution, stopExecutionOnFailure, resolveSources,
                resolveMultiplatformDependencies, renderKandyPlotsNatively, renderDataFrameTablesNatively,
                showTypeHintsOnlyInActiveCell, displayExecutionCount, showFoldableRegions);
    }
}
