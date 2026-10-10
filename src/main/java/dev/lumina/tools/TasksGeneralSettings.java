package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Tasks general configuration settings in Lumina IDE.
 */
public class TasksGeneralSettings implements Cloneable {

    public static final String DEFAULT_CHANGELIST_FORMAT = "${id} ${summary}";
    public static final String DEFAULT_BRANCH_FORMAT = "${id}";

    private String changelistNameFormat = DEFAULT_CHANGELIST_FORMAT;
    private String featureBranchNameFormat = DEFAULT_BRANCH_FORMAT;
    private boolean lowercased = false;
    private String replaceSpacesWith = "-";
    private int taskHistoryLength = 50;
    private int connectionTimeoutMs = 5000;
    private boolean showTaskWidgetIfNoActiveTasks = false;
    private boolean saveContextOnCommit = true;

    // Issue Cache
    private boolean enableCache = true;
    private int updateIssuesCount = 100;
    private int cacheIntervalMinutes = 20;

    public TasksGeneralSettings() {
    }

    public String getChangelistNameFormat() {
        return changelistNameFormat != null ? changelistNameFormat : DEFAULT_CHANGELIST_FORMAT;
    }

    public void setChangelistNameFormat(String changelistNameFormat) {
        this.changelistNameFormat = changelistNameFormat != null ? changelistNameFormat.trim() : DEFAULT_CHANGELIST_FORMAT;
    }

    public String getFeatureBranchNameFormat() {
        return featureBranchNameFormat != null ? featureBranchNameFormat : DEFAULT_BRANCH_FORMAT;
    }

    public void setFeatureBranchNameFormat(String featureBranchNameFormat) {
        this.featureBranchNameFormat = featureBranchNameFormat != null ? featureBranchNameFormat.trim() : DEFAULT_BRANCH_FORMAT;
    }

    public boolean isLowercased() {
        return lowercased;
    }

    public void setLowercased(boolean lowercased) {
        this.lowercased = lowercased;
    }

    public String getReplaceSpacesWith() {
        return replaceSpacesWith != null ? replaceSpacesWith : "-";
    }

    public void setReplaceSpacesWith(String replaceSpacesWith) {
        this.replaceSpacesWith = replaceSpacesWith != null ? replaceSpacesWith : "-";
    }

    public int getTaskHistoryLength() {
        return taskHistoryLength;
    }

    public void setTaskHistoryLength(int taskHistoryLength) {
        this.taskHistoryLength = taskHistoryLength > 0 ? taskHistoryLength : 50;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs > 0 ? connectionTimeoutMs : 5000;
    }

    public boolean isShowTaskWidgetIfNoActiveTasks() {
        return showTaskWidgetIfNoActiveTasks;
    }

    public void setShowTaskWidgetIfNoActiveTasks(boolean showTaskWidgetIfNoActiveTasks) {
        this.showTaskWidgetIfNoActiveTasks = showTaskWidgetIfNoActiveTasks;
    }

    public boolean isSaveContextOnCommit() {
        return saveContextOnCommit;
    }

    public void setSaveContextOnCommit(boolean saveContextOnCommit) {
        this.saveContextOnCommit = saveContextOnCommit;
    }

    public boolean isEnableCache() {
        return enableCache;
    }

    public void setEnableCache(boolean enableCache) {
        this.enableCache = enableCache;
    }

    public int getUpdateIssuesCount() {
        return updateIssuesCount;
    }

    public void setUpdateIssuesCount(int updateIssuesCount) {
        this.updateIssuesCount = updateIssuesCount > 0 ? updateIssuesCount : 100;
    }

    public int getCacheIntervalMinutes() {
        return cacheIntervalMinutes;
    }

    public void setCacheIntervalMinutes(int cacheIntervalMinutes) {
        this.cacheIntervalMinutes = cacheIntervalMinutes > 0 ? cacheIntervalMinutes : 20;
    }

    @Override
    public TasksGeneralSettings clone() {
        try {
            return (TasksGeneralSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            TasksGeneralSettings copy = new TasksGeneralSettings();
            copy.changelistNameFormat = this.changelistNameFormat;
            copy.featureBranchNameFormat = this.featureBranchNameFormat;
            copy.lowercased = this.lowercased;
            copy.replaceSpacesWith = this.replaceSpacesWith;
            copy.taskHistoryLength = this.taskHistoryLength;
            copy.connectionTimeoutMs = this.connectionTimeoutMs;
            copy.showTaskWidgetIfNoActiveTasks = this.showTaskWidgetIfNoActiveTasks;
            copy.saveContextOnCommit = this.saveContextOnCommit;
            copy.enableCache = this.enableCache;
            copy.updateIssuesCount = this.updateIssuesCount;
            copy.cacheIntervalMinutes = this.cacheIntervalMinutes;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TasksGeneralSettings that = (TasksGeneralSettings) o;
        return lowercased == that.lowercased &&
                taskHistoryLength == that.taskHistoryLength &&
                connectionTimeoutMs == that.connectionTimeoutMs &&
                showTaskWidgetIfNoActiveTasks == that.showTaskWidgetIfNoActiveTasks &&
                saveContextOnCommit == that.saveContextOnCommit &&
                enableCache == that.enableCache &&
                updateIssuesCount == that.updateIssuesCount &&
                cacheIntervalMinutes == that.cacheIntervalMinutes &&
                Objects.equals(changelistNameFormat, that.changelistNameFormat) &&
                Objects.equals(featureBranchNameFormat, that.featureBranchNameFormat) &&
                Objects.equals(replaceSpacesWith, that.replaceSpacesWith);
    }

    @Override
    public int hashCode() {
        return Objects.hash(changelistNameFormat, featureBranchNameFormat, lowercased,
                replaceSpacesWith, taskHistoryLength, connectionTimeoutMs,
                showTaskWidgetIfNoActiveTasks, saveContextOnCommit, enableCache,
                updateIssuesCount, cacheIntervalMinutes);
    }
}
