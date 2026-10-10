package dev.lumina.database;

import java.util.Objects;

/**
 * Model representing Output and Results settings for Database Query Execution in Lumina IDE.
 */
public class DatabaseOutputResultsSettings implements Cloneable {

    private boolean showTimestampForQueryOutput = false;
    private boolean enableDbmsOutput = false;

    private boolean showResultsInEditor = false;
    private boolean createTitleFromComment = true;
    private String treatTextAsTitleAfter = "";

    private String showServicesToolWindow = "For all output";
    private boolean focusOnServicesWindow = false;
    private boolean openNewServicesTab = false;
    private boolean activateServicesOutputPane = false;

    public DatabaseOutputResultsSettings() {
    }

    public boolean isShowTimestampForQueryOutput() {
        return showTimestampForQueryOutput;
    }

    public void setShowTimestampForQueryOutput(boolean showTimestampForQueryOutput) {
        this.showTimestampForQueryOutput = showTimestampForQueryOutput;
    }

    public boolean isEnableDbmsOutput() {
        return enableDbmsOutput;
    }

    public void setEnableDbmsOutput(boolean enableDbmsOutput) {
        this.enableDbmsOutput = enableDbmsOutput;
    }

    public boolean isShowResultsInEditor() {
        return showResultsInEditor;
    }

    public void setShowResultsInEditor(boolean showResultsInEditor) {
        this.showResultsInEditor = showResultsInEditor;
    }

    public boolean isCreateTitleFromComment() {
        return createTitleFromComment;
    }

    public void setCreateTitleFromComment(boolean createTitleFromComment) {
        this.createTitleFromComment = createTitleFromComment;
    }

    public String getTreatTextAsTitleAfter() {
        return treatTextAsTitleAfter;
    }

    public void setTreatTextAsTitleAfter(String treatTextAsTitleAfter) {
        this.treatTextAsTitleAfter = treatTextAsTitleAfter != null ? treatTextAsTitleAfter : "";
    }

    public String getShowServicesToolWindow() {
        return showServicesToolWindow;
    }

    public void setShowServicesToolWindow(String showServicesToolWindow) {
        this.showServicesToolWindow = showServicesToolWindow != null ? showServicesToolWindow : "For all output";
    }

    public boolean isFocusOnServicesWindow() {
        return focusOnServicesWindow;
    }

    public void setFocusOnServicesWindow(boolean focusOnServicesWindow) {
        this.focusOnServicesWindow = focusOnServicesWindow;
    }

    public boolean isOpenNewServicesTab() {
        return openNewServicesTab;
    }

    public void setOpenNewServicesTab(boolean openNewServicesTab) {
        this.openNewServicesTab = openNewServicesTab;
    }

    public boolean isActivateServicesOutputPane() {
        return activateServicesOutputPane;
    }

    public void setActivateServicesOutputPane(boolean activateServicesOutputPane) {
        this.activateServicesOutputPane = activateServicesOutputPane;
    }

    @Override
    public DatabaseOutputResultsSettings clone() {
        try {
            return (DatabaseOutputResultsSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseOutputResultsSettings that = (DatabaseOutputResultsSettings) o;
        return showTimestampForQueryOutput == that.showTimestampForQueryOutput &&
                enableDbmsOutput == that.enableDbmsOutput &&
                showResultsInEditor == that.showResultsInEditor &&
                createTitleFromComment == that.createTitleFromComment &&
                focusOnServicesWindow == that.focusOnServicesWindow &&
                openNewServicesTab == that.openNewServicesTab &&
                activateServicesOutputPane == that.activateServicesOutputPane &&
                Objects.equals(treatTextAsTitleAfter, that.treatTextAsTitleAfter) &&
                Objects.equals(showServicesToolWindow, that.showServicesToolWindow);
    }

    @Override
    public int hashCode() {
        return Objects.hash(showTimestampForQueryOutput, enableDbmsOutput, showResultsInEditor,
                createTitleFromComment, treatTextAsTitleAfter, showServicesToolWindow,
                focusOnServicesWindow, openNewServicesTab, activateServicesOutputPane);
    }
}
