package dev.lumina.database;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Database Query Execution settings in Lumina IDE.
 */
public class DatabaseQueryExecutionSettings implements Cloneable {

    private List<String> profiles = new ArrayList<>(List.of("Execute", "Execute (2)", "Execute (3)"));
    private String selectedProfile = "Execute";

    private String whenCaretInside = "Ask what to execute";
    private String whenCaretOutside = "Nothing";
    private String forSelection = "Exactly as separate statements";
    private boolean openResultsInNewTab = false;

    private String splitScript = "Into valid ANSI SQL statements or by separator";
    private boolean reviewParametersBeforeExecution = true;
    private boolean showWarningUnsafeQueries = true;

    public DatabaseQueryExecutionSettings() {
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<String> profiles) {
        this.profiles = profiles != null ? new ArrayList<>(profiles) : new ArrayList<>();
    }

    public String getSelectedProfile() {
        return selectedProfile;
    }

    public void setSelectedProfile(String selectedProfile) {
        this.selectedProfile = selectedProfile != null ? selectedProfile : "Execute";
    }

    public String getWhenCaretInside() {
        return whenCaretInside;
    }

    public void setWhenCaretInside(String whenCaretInside) {
        this.whenCaretInside = whenCaretInside != null ? whenCaretInside : "Ask what to execute";
    }

    public String getWhenCaretOutside() {
        return whenCaretOutside;
    }

    public void setWhenCaretOutside(String whenCaretOutside) {
        this.whenCaretOutside = whenCaretOutside != null ? whenCaretOutside : "Nothing";
    }

    public String getForSelection() {
        return forSelection;
    }

    public void setForSelection(String forSelection) {
        this.forSelection = forSelection != null ? forSelection : "Exactly as separate statements";
    }

    public boolean isOpenResultsInNewTab() {
        return openResultsInNewTab;
    }

    public void setOpenResultsInNewTab(boolean openResultsInNewTab) {
        this.openResultsInNewTab = openResultsInNewTab;
    }

    public String getSplitScript() {
        return splitScript;
    }

    public void setSplitScript(String splitScript) {
        this.splitScript = splitScript != null ? splitScript : "Into valid ANSI SQL statements or by separator";
    }

    public boolean isReviewParametersBeforeExecution() {
        return reviewParametersBeforeExecution;
    }

    public void setReviewParametersBeforeExecution(boolean reviewParametersBeforeExecution) {
        this.reviewParametersBeforeExecution = reviewParametersBeforeExecution;
    }

    public boolean isShowWarningUnsafeQueries() {
        return showWarningUnsafeQueries;
    }

    public void setShowWarningUnsafeQueries(boolean showWarningUnsafeQueries) {
        this.showWarningUnsafeQueries = showWarningUnsafeQueries;
    }

    @Override
    public DatabaseQueryExecutionSettings clone() {
        try {
            DatabaseQueryExecutionSettings copy = (DatabaseQueryExecutionSettings) super.clone();
            copy.profiles = new ArrayList<>(this.profiles);
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseQueryExecutionSettings that = (DatabaseQueryExecutionSettings) o;
        return openResultsInNewTab == that.openResultsInNewTab &&
                reviewParametersBeforeExecution == that.reviewParametersBeforeExecution &&
                showWarningUnsafeQueries == that.showWarningUnsafeQueries &&
                Objects.equals(profiles, that.profiles) &&
                Objects.equals(selectedProfile, that.selectedProfile) &&
                Objects.equals(whenCaretInside, that.whenCaretInside) &&
                Objects.equals(whenCaretOutside, that.whenCaretOutside) &&
                Objects.equals(forSelection, that.forSelection) &&
                Objects.equals(splitScript, that.splitScript);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profiles, selectedProfile, whenCaretInside, whenCaretOutside,
                forSelection, openResultsInNewTab, splitScript, reviewParametersBeforeExecution, showWarningUnsafeQueries);
    }
}
