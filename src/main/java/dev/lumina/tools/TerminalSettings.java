package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Terminal settings in Lumina IDE.
 * 1:1 specification match with IDE reference layout.
 */
public class TerminalSettings implements Cloneable {

    private String terminalEngine = "Reworked 2025";

    // Command Completion
    private boolean showCompletionPopup = true;
    private String completionPopupMode = "Only for parameters"; // "Always" or "Only for parameters"
    private String showCompletionPopupShortcut = "Ctrl+Space";
    private String insertSuggestionShortcut = "Enter";

    // Project Settings
    private String startDirectory = "";
    private String environmentVariables = "";

    // Font Settings
    private String fontFamily = "JetBrains Mono";
    private String fallbackFontFamily = "JetBrains Mono";
    private double fontSize = 13.0;
    private double lineHeight = 1.0;
    private double columnWidth = 1.0;

    // Application Settings
    private String shellPath = "/bin/bash";
    private String defaultTabName = "Local";
    private boolean enforceMinimumContrastRatio = true;
    private double contrastRatio = 4.5;
    private boolean showSeparatorsBetweenExecutedCommands = true;
    private boolean audibleBell = true;
    private boolean closeSessionWhenItEnds = true;
    private boolean mouseReporting = true;
    private boolean moveFocusToEditorWithEscape = true;
    private boolean pasteOnMiddleMouseButtonClick = true;
    private boolean overrideIdeShortcuts = true;
    private boolean shellIntegration = true;
    private boolean highlightHyperlinks = true;
    private boolean activateVirtualenv = true;
    private boolean addDefaultPhpInterpreterToPath = true;
    private String cursorShape = "Block"; // "Block", "Underline", "Vertical Bar"

    public TerminalSettings() {
    }

    public String getTerminalEngine() {
        return terminalEngine != null ? terminalEngine : "Reworked 2025";
    }

    public void setTerminalEngine(String terminalEngine) {
        this.terminalEngine = terminalEngine != null ? terminalEngine : "Reworked 2025";
    }

    public boolean isShowCompletionPopup() {
        return showCompletionPopup;
    }

    public void setShowCompletionPopup(boolean showCompletionPopup) {
        this.showCompletionPopup = showCompletionPopup;
    }

    public String getCompletionPopupMode() {
        return completionPopupMode != null ? completionPopupMode : "Only for parameters";
    }

    public void setCompletionPopupMode(String completionPopupMode) {
        this.completionPopupMode = completionPopupMode != null ? completionPopupMode : "Only for parameters";
    }

    public String getShowCompletionPopupShortcut() {
        return showCompletionPopupShortcut != null ? showCompletionPopupShortcut : "Ctrl+Space";
    }

    public void setShowCompletionPopupShortcut(String showCompletionPopupShortcut) {
        this.showCompletionPopupShortcut = showCompletionPopupShortcut != null ? showCompletionPopupShortcut : "Ctrl+Space";
    }

    public String getInsertSuggestionShortcut() {
        return insertSuggestionShortcut != null ? insertSuggestionShortcut : "Enter";
    }

    public void setInsertSuggestionShortcut(String insertSuggestionShortcut) {
        this.insertSuggestionShortcut = insertSuggestionShortcut != null ? insertSuggestionShortcut : "Enter";
    }

    public String getStartDirectory() {
        return startDirectory != null ? startDirectory : "";
    }

    public void setStartDirectory(String startDirectory) {
        this.startDirectory = startDirectory != null ? startDirectory : "";
    }

    public String getEnvironmentVariables() {
        return environmentVariables != null ? environmentVariables : "";
    }

    public void setEnvironmentVariables(String environmentVariables) {
        this.environmentVariables = environmentVariables != null ? environmentVariables : "";
    }

    public String getFontFamily() {
        return fontFamily != null ? fontFamily : "JetBrains Mono";
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily != null ? fontFamily : "JetBrains Mono";
    }

    public String getFallbackFontFamily() {
        return fallbackFontFamily != null ? fallbackFontFamily : "JetBrains Mono";
    }

    public void setFallbackFontFamily(String fallbackFontFamily) {
        this.fallbackFontFamily = fallbackFontFamily != null ? fallbackFontFamily : "JetBrains Mono";
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize > 0 ? fontSize : 13.0;
    }

    public double getLineHeight() {
        return lineHeight;
    }

    public void setLineHeight(double lineHeight) {
        this.lineHeight = lineHeight > 0 ? lineHeight : 1.0;
    }

    public double getColumnWidth() {
        return columnWidth;
    }

    public void setColumnWidth(double columnWidth) {
        this.columnWidth = columnWidth > 0 ? columnWidth : 1.0;
    }

    public String getShellPath() {
        return shellPath != null ? shellPath : "/bin/bash";
    }

    public void setShellPath(String shellPath) {
        this.shellPath = shellPath != null ? shellPath : "/bin/bash";
    }

    public String getDefaultTabName() {
        return defaultTabName != null ? defaultTabName : "Local";
    }

    public void setDefaultTabName(String defaultTabName) {
        this.defaultTabName = defaultTabName != null ? defaultTabName : "Local";
    }

    public boolean isEnforceMinimumContrastRatio() {
        return enforceMinimumContrastRatio;
    }

    public void setEnforceMinimumContrastRatio(boolean enforceMinimumContrastRatio) {
        this.enforceMinimumContrastRatio = enforceMinimumContrastRatio;
    }

    public double getContrastRatio() {
        return contrastRatio;
    }

    public void setContrastRatio(double contrastRatio) {
        this.contrastRatio = contrastRatio > 0 ? contrastRatio : 4.5;
    }

    public boolean isShowSeparatorsBetweenExecutedCommands() {
        return showSeparatorsBetweenExecutedCommands;
    }

    public void setShowSeparatorsBetweenExecutedCommands(boolean showSeparatorsBetweenExecutedCommands) {
        this.showSeparatorsBetweenExecutedCommands = showSeparatorsBetweenExecutedCommands;
    }

    public boolean isAudibleBell() {
        return audibleBell;
    }

    public void setAudibleBell(boolean audibleBell) {
        this.audibleBell = audibleBell;
    }

    public boolean isCloseSessionWhenItEnds() {
        return closeSessionWhenItEnds;
    }

    public void setCloseSessionWhenItEnds(boolean closeSessionWhenItEnds) {
        this.closeSessionWhenItEnds = closeSessionWhenItEnds;
    }

    public boolean isMouseReporting() {
        return mouseReporting;
    }

    public void setMouseReporting(boolean mouseReporting) {
        this.mouseReporting = mouseReporting;
    }

    public boolean isMoveFocusToEditorWithEscape() {
        return moveFocusToEditorWithEscape;
    }

    public void setMoveFocusToEditorWithEscape(boolean moveFocusToEditorWithEscape) {
        this.moveFocusToEditorWithEscape = moveFocusToEditorWithEscape;
    }

    public boolean isPasteOnMiddleMouseButtonClick() {
        return pasteOnMiddleMouseButtonClick;
    }

    public void setPasteOnMiddleMouseButtonClick(boolean pasteOnMiddleMouseButtonClick) {
        this.pasteOnMiddleMouseButtonClick = pasteOnMiddleMouseButtonClick;
    }

    public boolean isOverrideIdeShortcuts() {
        return overrideIdeShortcuts;
    }

    public void setOverrideIdeShortcuts(boolean overrideIdeShortcuts) {
        this.overrideIdeShortcuts = overrideIdeShortcuts;
    }

    public boolean isShellIntegration() {
        return shellIntegration;
    }

    public void setShellIntegration(boolean shellIntegration) {
        this.shellIntegration = shellIntegration;
    }

    public boolean isHighlightHyperlinks() {
        return highlightHyperlinks;
    }

    public void setHighlightHyperlinks(boolean highlightHyperlinks) {
        this.highlightHyperlinks = highlightHyperlinks;
    }

    public boolean isActivateVirtualenv() {
        return activateVirtualenv;
    }

    public void setActivateVirtualenv(boolean activateVirtualenv) {
        this.activateVirtualenv = activateVirtualenv;
    }

    public boolean isAddDefaultPhpInterpreterToPath() {
        return addDefaultPhpInterpreterToPath;
    }

    public void setAddDefaultPhpInterpreterToPath(boolean addDefaultPhpInterpreterToPath) {
        this.addDefaultPhpInterpreterToPath = addDefaultPhpInterpreterToPath;
    }

    public String getCursorShape() {
        return cursorShape != null ? cursorShape : "Block";
    }

    public void setCursorShape(String cursorShape) {
        this.cursorShape = cursorShape != null ? cursorShape : "Block";
    }

    @Override
    public TerminalSettings clone() {
        try {
            return (TerminalSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            TerminalSettings copy = new TerminalSettings();
            copy.terminalEngine = this.terminalEngine;
            copy.showCompletionPopup = this.showCompletionPopup;
            copy.completionPopupMode = this.completionPopupMode;
            copy.showCompletionPopupShortcut = this.showCompletionPopupShortcut;
            copy.insertSuggestionShortcut = this.insertSuggestionShortcut;
            copy.startDirectory = this.startDirectory;
            copy.environmentVariables = this.environmentVariables;
            copy.fontFamily = this.fontFamily;
            copy.fallbackFontFamily = this.fallbackFontFamily;
            copy.fontSize = this.fontSize;
            copy.lineHeight = this.lineHeight;
            copy.columnWidth = this.columnWidth;
            copy.shellPath = this.shellPath;
            copy.defaultTabName = this.defaultTabName;
            copy.enforceMinimumContrastRatio = this.enforceMinimumContrastRatio;
            copy.contrastRatio = this.contrastRatio;
            copy.showSeparatorsBetweenExecutedCommands = this.showSeparatorsBetweenExecutedCommands;
            copy.audibleBell = this.audibleBell;
            copy.closeSessionWhenItEnds = this.closeSessionWhenItEnds;
            copy.mouseReporting = this.mouseReporting;
            copy.moveFocusToEditorWithEscape = this.moveFocusToEditorWithEscape;
            copy.pasteOnMiddleMouseButtonClick = this.pasteOnMiddleMouseButtonClick;
            copy.overrideIdeShortcuts = this.overrideIdeShortcuts;
            copy.shellIntegration = this.shellIntegration;
            copy.highlightHyperlinks = this.highlightHyperlinks;
            copy.activateVirtualenv = this.activateVirtualenv;
            copy.addDefaultPhpInterpreterToPath = this.addDefaultPhpInterpreterToPath;
            copy.cursorShape = this.cursorShape;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerminalSettings that = (TerminalSettings) o;
        return showCompletionPopup == that.showCompletionPopup &&
                Double.compare(that.fontSize, fontSize) == 0 &&
                Double.compare(that.lineHeight, lineHeight) == 0 &&
                Double.compare(that.columnWidth, columnWidth) == 0 &&
                enforceMinimumContrastRatio == that.enforceMinimumContrastRatio &&
                Double.compare(that.contrastRatio, contrastRatio) == 0 &&
                showSeparatorsBetweenExecutedCommands == that.showSeparatorsBetweenExecutedCommands &&
                audibleBell == that.audibleBell &&
                closeSessionWhenItEnds == that.closeSessionWhenItEnds &&
                mouseReporting == that.mouseReporting &&
                moveFocusToEditorWithEscape == that.moveFocusToEditorWithEscape &&
                pasteOnMiddleMouseButtonClick == that.pasteOnMiddleMouseButtonClick &&
                overrideIdeShortcuts == that.overrideIdeShortcuts &&
                shellIntegration == that.shellIntegration &&
                highlightHyperlinks == that.highlightHyperlinks &&
                activateVirtualenv == that.activateVirtualenv &&
                addDefaultPhpInterpreterToPath == that.addDefaultPhpInterpreterToPath &&
                Objects.equals(terminalEngine, that.terminalEngine) &&
                Objects.equals(completionPopupMode, that.completionPopupMode) &&
                Objects.equals(showCompletionPopupShortcut, that.showCompletionPopupShortcut) &&
                Objects.equals(insertSuggestionShortcut, that.insertSuggestionShortcut) &&
                Objects.equals(startDirectory, that.startDirectory) &&
                Objects.equals(environmentVariables, that.environmentVariables) &&
                Objects.equals(fontFamily, that.fontFamily) &&
                Objects.equals(fallbackFontFamily, that.fallbackFontFamily) &&
                Objects.equals(shellPath, that.shellPath) &&
                Objects.equals(defaultTabName, that.defaultTabName) &&
                Objects.equals(cursorShape, that.cursorShape);
    }

    @Override
    public int hashCode() {
        return Objects.hash(terminalEngine, showCompletionPopup, completionPopupMode, showCompletionPopupShortcut,
                insertSuggestionShortcut, startDirectory, environmentVariables, fontFamily, fallbackFontFamily,
                fontSize, lineHeight, columnWidth, shellPath, defaultTabName, enforceMinimumContrastRatio,
                contrastRatio, showSeparatorsBetweenExecutedCommands, audibleBell, closeSessionWhenItEnds,
                mouseReporting, moveFocusToEditorWithEscape, pasteOnMiddleMouseButtonClick, overrideIdeShortcuts,
                shellIntegration, highlightHyperlinks, activateVirtualenv, addDefaultPhpInterpreterToPath, cursorShape);
    }
}
