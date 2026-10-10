package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Jupyter > Jupyter General settings matching Image 4.
 */
public class JupyterGeneralSettings implements Cloneable {

    // Appearance
    private boolean showAddCellPopup = true;
    private boolean showRunAndDebugActions = false;
    private boolean invertImageOutputsForDarkThemes = true;
    private int maxOutputHeightInTextLines = -1;
    private boolean asciiColoringInErrorOutputs = false;

    // Markdown
    private int markdownFontScale = 100;
    private boolean renderMarkdownCellsAutomatically = true;

    // Variables
    private boolean openVariablesOnFirstCellExecution = true;
    private boolean showInlineValues = true;
    private String inlineValuesScope = "In the current line";

    // Execution
    private boolean notifyWhenCellExecutionExceeds = true;
    private int cellExecutionTimeoutSeconds = 60;
    private boolean showTimestampOnExecutionLabel = false;
    private String executionTimeDisplayMode = "Compact (recommended)";
    private boolean includeProjectSourceRootsToPythonPath = true;

    // Other
    private boolean uploadSupportLibsToJupyterServer = true;

    public JupyterGeneralSettings() {
    }

    public boolean isShowAddCellPopup() {
        return showAddCellPopup;
    }

    public void setShowAddCellPopup(boolean showAddCellPopup) {
        this.showAddCellPopup = showAddCellPopup;
    }

    public boolean isShowRunAndDebugActions() {
        return showRunAndDebugActions;
    }

    public void setShowRunAndDebugActions(boolean showRunAndDebugActions) {
        this.showRunAndDebugActions = showRunAndDebugActions;
    }

    public boolean isInvertImageOutputsForDarkThemes() {
        return invertImageOutputsForDarkThemes;
    }

    public void setInvertImageOutputsForDarkThemes(boolean invertImageOutputsForDarkThemes) {
        this.invertImageOutputsForDarkThemes = invertImageOutputsForDarkThemes;
    }

    public int getMaxOutputHeightInTextLines() {
        return maxOutputHeightInTextLines;
    }

    public void setMaxOutputHeightInTextLines(int maxOutputHeightInTextLines) {
        this.maxOutputHeightInTextLines = maxOutputHeightInTextLines;
    }

    public boolean isAsciiColoringInErrorOutputs() {
        return asciiColoringInErrorOutputs;
    }

    public void setAsciiColoringInErrorOutputs(boolean asciiColoringInErrorOutputs) {
        this.asciiColoringInErrorOutputs = asciiColoringInErrorOutputs;
    }

    public int getMarkdownFontScale() {
        return markdownFontScale;
    }

    public void setMarkdownFontScale(int markdownFontScale) {
        this.markdownFontScale = markdownFontScale;
    }

    public boolean isRenderMarkdownCellsAutomatically() {
        return renderMarkdownCellsAutomatically;
    }

    public void setRenderMarkdownCellsAutomatically(boolean renderMarkdownCellsAutomatically) {
        this.renderMarkdownCellsAutomatically = renderMarkdownCellsAutomatically;
    }

    public boolean isOpenVariablesOnFirstCellExecution() {
        return openVariablesOnFirstCellExecution;
    }

    public void setOpenVariablesOnFirstCellExecution(boolean openVariablesOnFirstCellExecution) {
        this.openVariablesOnFirstCellExecution = openVariablesOnFirstCellExecution;
    }

    public boolean isShowInlineValues() {
        return showInlineValues;
    }

    public void setShowInlineValues(boolean showInlineValues) {
        this.showInlineValues = showInlineValues;
    }

    public String getInlineValuesScope() {
        return inlineValuesScope;
    }

    public void setInlineValuesScope(String inlineValuesScope) {
        this.inlineValuesScope = inlineValuesScope != null ? inlineValuesScope : "In the current line";
    }

    public boolean isNotifyWhenCellExecutionExceeds() {
        return notifyWhenCellExecutionExceeds;
    }

    public void setNotifyWhenCellExecutionExceeds(boolean notifyWhenCellExecutionExceeds) {
        this.notifyWhenCellExecutionExceeds = notifyWhenCellExecutionExceeds;
    }

    public int getCellExecutionTimeoutSeconds() {
        return cellExecutionTimeoutSeconds;
    }

    public void setCellExecutionTimeoutSeconds(int cellExecutionTimeoutSeconds) {
        this.cellExecutionTimeoutSeconds = cellExecutionTimeoutSeconds;
    }

    public boolean isShowTimestampOnExecutionLabel() {
        return showTimestampOnExecutionLabel;
    }

    public void setShowTimestampOnExecutionLabel(boolean showTimestampOnExecutionLabel) {
        this.showTimestampOnExecutionLabel = showTimestampOnExecutionLabel;
    }

    public String getExecutionTimeDisplayMode() {
        return executionTimeDisplayMode;
    }

    public void setExecutionTimeDisplayMode(String executionTimeDisplayMode) {
        this.executionTimeDisplayMode = executionTimeDisplayMode != null ? executionTimeDisplayMode : "Compact (recommended)";
    }

    public boolean isIncludeProjectSourceRootsToPythonPath() {
        return includeProjectSourceRootsToPythonPath;
    }

    public void setIncludeProjectSourceRootsToPythonPath(boolean includeProjectSourceRootsToPythonPath) {
        this.includeProjectSourceRootsToPythonPath = includeProjectSourceRootsToPythonPath;
    }

    public boolean isUploadSupportLibsToJupyterServer() {
        return uploadSupportLibsToJupyterServer;
    }

    public void setUploadSupportLibsToJupyterServer(boolean uploadSupportLibsToJupyterServer) {
        this.uploadSupportLibsToJupyterServer = uploadSupportLibsToJupyterServer;
    }

    public JupyterGeneralSettings copy() {
        return clone();
    }

    @Override
    public JupyterGeneralSettings clone() {
        JupyterGeneralSettings c = new JupyterGeneralSettings();
        c.showAddCellPopup = this.showAddCellPopup;
        c.showRunAndDebugActions = this.showRunAndDebugActions;
        c.invertImageOutputsForDarkThemes = this.invertImageOutputsForDarkThemes;
        c.maxOutputHeightInTextLines = this.maxOutputHeightInTextLines;
        c.asciiColoringInErrorOutputs = this.asciiColoringInErrorOutputs;
        c.markdownFontScale = this.markdownFontScale;
        c.renderMarkdownCellsAutomatically = this.renderMarkdownCellsAutomatically;
        c.openVariablesOnFirstCellExecution = this.openVariablesOnFirstCellExecution;
        c.showInlineValues = this.showInlineValues;
        c.inlineValuesScope = this.inlineValuesScope;
        c.notifyWhenCellExecutionExceeds = this.notifyWhenCellExecutionExceeds;
        c.cellExecutionTimeoutSeconds = this.cellExecutionTimeoutSeconds;
        c.showTimestampOnExecutionLabel = this.showTimestampOnExecutionLabel;
        c.executionTimeDisplayMode = this.executionTimeDisplayMode;
        c.includeProjectSourceRootsToPythonPath = this.includeProjectSourceRootsToPythonPath;
        c.uploadSupportLibsToJupyterServer = this.uploadSupportLibsToJupyterServer;
        return c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JupyterGeneralSettings that = (JupyterGeneralSettings) o;
        return showAddCellPopup == that.showAddCellPopup &&
                showRunAndDebugActions == that.showRunAndDebugActions &&
                invertImageOutputsForDarkThemes == that.invertImageOutputsForDarkThemes &&
                maxOutputHeightInTextLines == that.maxOutputHeightInTextLines &&
                asciiColoringInErrorOutputs == that.asciiColoringInErrorOutputs &&
                markdownFontScale == that.markdownFontScale &&
                renderMarkdownCellsAutomatically == that.renderMarkdownCellsAutomatically &&
                openVariablesOnFirstCellExecution == that.openVariablesOnFirstCellExecution &&
                showInlineValues == that.showInlineValues &&
                notifyWhenCellExecutionExceeds == that.notifyWhenCellExecutionExceeds &&
                cellExecutionTimeoutSeconds == that.cellExecutionTimeoutSeconds &&
                showTimestampOnExecutionLabel == that.showTimestampOnExecutionLabel &&
                includeProjectSourceRootsToPythonPath == that.includeProjectSourceRootsToPythonPath &&
                uploadSupportLibsToJupyterServer == that.uploadSupportLibsToJupyterServer &&
                Objects.equals(inlineValuesScope, that.inlineValuesScope) &&
                Objects.equals(executionTimeDisplayMode, that.executionTimeDisplayMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showAddCellPopup, showRunAndDebugActions, invertImageOutputsForDarkThemes,
                maxOutputHeightInTextLines, asciiColoringInErrorOutputs, markdownFontScale,
                renderMarkdownCellsAutomatically, openVariablesOnFirstCellExecution, showInlineValues,
                inlineValuesScope, notifyWhenCellExecutionExceeds, cellExecutionTimeoutSeconds,
                showTimestampOnExecutionLabel, executionTimeDisplayMode, includeProjectSourceRootsToPythonPath,
                uploadSupportLibsToJupyterServer);
    }

    @Override
    public String toString() {
        return "JupyterGeneralSettings{" +
                "fontScale=" + markdownFontScale +
                ", executionMode='" + executionTimeDisplayMode + '\'' +
                '}';
    }
}
