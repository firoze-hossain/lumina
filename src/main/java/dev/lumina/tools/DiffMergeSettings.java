package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Diff & Merge settings in Lumina IDE.
 */
public class DiffMergeSettings implements Cloneable {

    private int contextLines = 4;
    private boolean goToNextFileAfterLastChange = true;
    private String navigationHistoryPolicy = "Until the diff is closed";

    private boolean autoApplyNonConflictingChanges = false;
    private boolean autoResolveConflictsInImports = false;
    private boolean highlightModifiedLinesInGutter = true;

    public DiffMergeSettings() {
    }

    public int getContextLines() {
        return contextLines;
    }

    public void setContextLines(int contextLines) {
        this.contextLines = contextLines;
    }

    public boolean isGoToNextFileAfterLastChange() {
        return goToNextFileAfterLastChange;
    }

    public void setGoToNextFileAfterLastChange(boolean goToNextFileAfterLastChange) {
        this.goToNextFileAfterLastChange = goToNextFileAfterLastChange;
    }

    public String getNavigationHistoryPolicy() {
        return navigationHistoryPolicy;
    }

    public void setNavigationHistoryPolicy(String navigationHistoryPolicy) {
        this.navigationHistoryPolicy = navigationHistoryPolicy != null ? navigationHistoryPolicy : "Until the diff is closed";
    }

    public boolean isAutoApplyNonConflictingChanges() {
        return autoApplyNonConflictingChanges;
    }

    public void setAutoApplyNonConflictingChanges(boolean autoApplyNonConflictingChanges) {
        this.autoApplyNonConflictingChanges = autoApplyNonConflictingChanges;
    }

    public boolean isAutoResolveConflictsInImports() {
        return autoResolveConflictsInImports;
    }

    public void setAutoResolveConflictsInImports(boolean autoResolveConflictsInImports) {
        this.autoResolveConflictsInImports = autoResolveConflictsInImports;
    }

    public boolean isHighlightModifiedLinesInGutter() {
        return highlightModifiedLinesInGutter;
    }

    public void setHighlightModifiedLinesInGutter(boolean highlightModifiedLinesInGutter) {
        this.highlightModifiedLinesInGutter = highlightModifiedLinesInGutter;
    }

    @Override
    public DiffMergeSettings clone() {
        try {
            return (DiffMergeSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiffMergeSettings that = (DiffMergeSettings) o;
        return contextLines == that.contextLines &&
                goToNextFileAfterLastChange == that.goToNextFileAfterLastChange &&
                autoApplyNonConflictingChanges == that.autoApplyNonConflictingChanges &&
                autoResolveConflictsInImports == that.autoResolveConflictsInImports &&
                highlightModifiedLinesInGutter == that.highlightModifiedLinesInGutter &&
                Objects.equals(navigationHistoryPolicy, that.navigationHistoryPolicy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextLines, goToNextFileAfterLastChange, navigationHistoryPolicy,
                autoApplyNonConflictingChanges, autoResolveConflictsInImports, highlightModifiedLinesInGutter);
    }
}
