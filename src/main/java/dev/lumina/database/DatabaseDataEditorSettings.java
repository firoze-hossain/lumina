package dev.lumina.database;

import java.util.Objects;

/**
 * Model representing Tools > Database > Data Editor and Viewer settings in Lumina IDE.
 */
public class DatabaseDataEditorSettings implements Cloneable {

    // Limitations
    private boolean limitPageSize = true;
    private int pageSize = 500;
    private int resultSetPrefetchSize = 100;
    private int filterHistorySize = 10;
    private int maxBytesPerValue = 204800;
    private boolean showFirstDataRowsInPreview = true;
    private int previewRowCount = 10;

    // Controls Customization
    private boolean enablePagingInEditorResults = false;
    private String gridPaginationControlPosition = "Grid bottom (floating)";
    private boolean showQuickActionsPopupToolbar = true;
    private boolean enableQuickActionsCustomization = true;

    // Data Presentation
    private String automaticallyTransposeTables = "Never";
    private boolean detectBinaryText = true;
    private boolean detectBinaryUuid = true;
    private boolean enableLocalFilterByDefault = true;
    private boolean enableImmediateCompletion = true;
    private String displayTemporalDataTimeZone = "";

    // Custom Number Formats
    private String decimalSeparator = ".";
    private boolean useGroupingSeparator = false;
    private String groupingSeparator = "";
    private String infinityText = "Infinity";
    private String nanText = "NaN";
    private boolean useNumberPattern = false;
    private String numberPattern = "";

    // Custom Date/Time Formats
    private boolean useDatetimeTimestamp = false;
    private String datetimeTimestampPattern = "yyyy-MM-dd HH:mm:ss";
    private boolean useDatetimeWithTz = false;
    private String datetimeWithTzPattern = "yyyy-MM-dd HH:mm:ss Z";
    private boolean useTime = false;
    private String timePattern = "HH:mm:ss";
    private boolean useTimeWithTz = false;
    private String timeWithTzPattern = "HH:mm:ss Z";
    private boolean useDate = false;
    private String datePattern = "yyyy-MM-dd";

    // Data Sorting
    private boolean sortViaOrderBy = true;
    private boolean sortTablesByNumericPrimaryKey = false;
    private String numericPrimaryKeyDirection = "Ascending";
    private String addColumnsToSorting = "⌥Click";

    // Data Modification
    private boolean submitChangesImmediately = false;
    private boolean enableEditingForJoinQueries = true;
    private boolean showDmlPreviewForJoinQueries = true;

    // URL Click Settings
    private boolean allowOpeningSecureLinks = false;
    private boolean allowOpeningStandardLinks = false;
    private boolean allowOpeningLocalFileLinks = false;
    private boolean assumeHttpIfNoProtocol = false;

    public DatabaseDataEditorSettings() {
    }

    // Getters and Setters

    public boolean isLimitPageSize() {
        return limitPageSize;
    }

    public void setLimitPageSize(boolean limitPageSize) {
        this.limitPageSize = limitPageSize;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getResultSetPrefetchSize() {
        return resultSetPrefetchSize;
    }

    public void setResultSetPrefetchSize(int resultSetPrefetchSize) {
        this.resultSetPrefetchSize = resultSetPrefetchSize;
    }

    public int getFilterHistorySize() {
        return filterHistorySize;
    }

    public void setFilterHistorySize(int filterHistorySize) {
        this.filterHistorySize = filterHistorySize;
    }

    public int getMaxBytesPerValue() {
        return maxBytesPerValue;
    }

    public void setMaxBytesPerValue(int maxBytesPerValue) {
        this.maxBytesPerValue = maxBytesPerValue;
    }

    public boolean isShowFirstDataRowsInPreview() {
        return showFirstDataRowsInPreview;
    }

    public void setShowFirstDataRowsInPreview(boolean showFirstDataRowsInPreview) {
        this.showFirstDataRowsInPreview = showFirstDataRowsInPreview;
    }

    public int getPreviewRowCount() {
        return previewRowCount;
    }

    public void setPreviewRowCount(int previewRowCount) {
        this.previewRowCount = previewRowCount;
    }

    public boolean isEnablePagingInEditorResults() {
        return enablePagingInEditorResults;
    }

    public void setEnablePagingInEditorResults(boolean enablePagingInEditorResults) {
        this.enablePagingInEditorResults = enablePagingInEditorResults;
    }

    public String getGridPaginationControlPosition() {
        return gridPaginationControlPosition;
    }

    public void setGridPaginationControlPosition(String gridPaginationControlPosition) {
        this.gridPaginationControlPosition = gridPaginationControlPosition != null ? gridPaginationControlPosition : "Grid bottom (floating)";
    }

    public boolean isShowQuickActionsPopupToolbar() {
        return showQuickActionsPopupToolbar;
    }

    public void setShowQuickActionsPopupToolbar(boolean showQuickActionsPopupToolbar) {
        this.showQuickActionsPopupToolbar = showQuickActionsPopupToolbar;
    }

    public boolean isEnableQuickActionsCustomization() {
        return enableQuickActionsCustomization;
    }

    public void setEnableQuickActionsCustomization(boolean enableQuickActionsCustomization) {
        this.enableQuickActionsCustomization = enableQuickActionsCustomization;
    }

    public String getAutomaticallyTransposeTables() {
        return automaticallyTransposeTables;
    }

    public void setAutomaticallyTransposeTables(String automaticallyTransposeTables) {
        this.automaticallyTransposeTables = automaticallyTransposeTables != null ? automaticallyTransposeTables : "Never";
    }

    public boolean isDetectBinaryText() {
        return detectBinaryText;
    }

    public void setDetectBinaryText(boolean detectBinaryText) {
        this.detectBinaryText = detectBinaryText;
    }

    public boolean isDetectBinaryUuid() {
        return detectBinaryUuid;
    }

    public void setDetectBinaryUuid(boolean detectBinaryUuid) {
        this.detectBinaryUuid = detectBinaryUuid;
    }

    public boolean isEnableLocalFilterByDefault() {
        return enableLocalFilterByDefault;
    }

    public void setEnableLocalFilterByDefault(boolean enableLocalFilterByDefault) {
        this.enableLocalFilterByDefault = enableLocalFilterByDefault;
    }

    public boolean isEnableImmediateCompletion() {
        return enableImmediateCompletion;
    }

    public void setEnableImmediateCompletion(boolean enableImmediateCompletion) {
        this.enableImmediateCompletion = enableImmediateCompletion;
    }

    public String getDisplayTemporalDataTimeZone() {
        return displayTemporalDataTimeZone;
    }

    public void setDisplayTemporalDataTimeZone(String displayTemporalDataTimeZone) {
        this.displayTemporalDataTimeZone = displayTemporalDataTimeZone != null ? displayTemporalDataTimeZone : "";
    }

    public String getDecimalSeparator() {
        return decimalSeparator;
    }

    public void setDecimalSeparator(String decimalSeparator) {
        this.decimalSeparator = decimalSeparator != null ? decimalSeparator : ".";
    }

    public boolean isUseGroupingSeparator() {
        return useGroupingSeparator;
    }

    public void setUseGroupingSeparator(boolean useGroupingSeparator) {
        this.useGroupingSeparator = useGroupingSeparator;
    }

    public String getGroupingSeparator() {
        return groupingSeparator;
    }

    public void setGroupingSeparator(String groupingSeparator) {
        this.groupingSeparator = groupingSeparator != null ? groupingSeparator : "";
    }

    public String getInfinityText() {
        return infinityText;
    }

    public void setInfinityText(String infinityText) {
        this.infinityText = infinityText != null ? infinityText : "Infinity";
    }

    public String getNanText() {
        return nanText;
    }

    public void setNanText(String nanText) {
        this.nanText = nanText != null ? nanText : "NaN";
    }

    public boolean isUseNumberPattern() {
        return useNumberPattern;
    }

    public void setUseNumberPattern(boolean useNumberPattern) {
        this.useNumberPattern = useNumberPattern;
    }

    public String getNumberPattern() {
        return numberPattern;
    }

    public void setNumberPattern(String numberPattern) {
        this.numberPattern = numberPattern != null ? numberPattern : "";
    }

    public boolean isUseDatetimeTimestamp() {
        return useDatetimeTimestamp;
    }

    public void setUseDatetimeTimestamp(boolean useDatetimeTimestamp) {
        this.useDatetimeTimestamp = useDatetimeTimestamp;
    }

    public String getDatetimeTimestampPattern() {
        return datetimeTimestampPattern;
    }

    public void setDatetimeTimestampPattern(String datetimeTimestampPattern) {
        this.datetimeTimestampPattern = datetimeTimestampPattern != null ? datetimeTimestampPattern : "yyyy-MM-dd HH:mm:ss";
    }

    public boolean isUseDatetimeWithTz() {
        return useDatetimeWithTz;
    }

    public void setUseDatetimeWithTz(boolean useDatetimeWithTz) {
        this.useDatetimeWithTz = useDatetimeWithTz;
    }

    public String getDatetimeWithTzPattern() {
        return datetimeWithTzPattern;
    }

    public void setDatetimeWithTzPattern(String datetimeWithTzPattern) {
        this.datetimeWithTzPattern = datetimeWithTzPattern != null ? datetimeWithTzPattern : "yyyy-MM-dd HH:mm:ss Z";
    }

    public boolean isUseTime() {
        return useTime;
    }

    public void setUseTime(boolean useTime) {
        this.useTime = useTime;
    }

    public String getTimePattern() {
        return timePattern;
    }

    public void setTimePattern(String timePattern) {
        this.timePattern = timePattern != null ? timePattern : "HH:mm:ss";
    }

    public boolean isUseTimeWithTz() {
        return useTimeWithTz;
    }

    public void setUseTimeWithTz(boolean useTimeWithTz) {
        this.useTimeWithTz = useTimeWithTz;
    }

    public String getTimeWithTzPattern() {
        return timeWithTzPattern;
    }

    public void setTimeWithTzPattern(String timeWithTzPattern) {
        this.timeWithTzPattern = timeWithTzPattern != null ? timeWithTzPattern : "HH:mm:ss Z";
    }

    public boolean isUseDate() {
        return useDate;
    }

    public void setUseDate(boolean useDate) {
        this.useDate = useDate;
    }

    public String getDatePattern() {
        return datePattern;
    }

    public void setDatePattern(String datePattern) {
        this.datePattern = datePattern != null ? datePattern : "yyyy-MM-dd";
    }

    public boolean isSortViaOrderBy() {
        return sortViaOrderBy;
    }

    public void setSortViaOrderBy(boolean sortViaOrderBy) {
        this.sortViaOrderBy = sortViaOrderBy;
    }

    public boolean isSortTablesByNumericPrimaryKey() {
        return sortTablesByNumericPrimaryKey;
    }

    public void setSortTablesByNumericPrimaryKey(boolean sortTablesByNumericPrimaryKey) {
        this.sortTablesByNumericPrimaryKey = sortTablesByNumericPrimaryKey;
    }

    public String getNumericPrimaryKeyDirection() {
        return numericPrimaryKeyDirection;
    }

    public void setNumericPrimaryKeyDirection(String numericPrimaryKeyDirection) {
        this.numericPrimaryKeyDirection = numericPrimaryKeyDirection != null ? numericPrimaryKeyDirection : "Ascending";
    }

    public String getAddColumnsToSorting() {
        return addColumnsToSorting;
    }

    public void setAddColumnsToSorting(String addColumnsToSorting) {
        this.addColumnsToSorting = addColumnsToSorting != null ? addColumnsToSorting : "⌥Click";
    }

    public boolean isSubmitChangesImmediately() {
        return submitChangesImmediately;
    }

    public void setSubmitChangesImmediately(boolean submitChangesImmediately) {
        this.submitChangesImmediately = submitChangesImmediately;
    }

    public boolean isEnableEditingForJoinQueries() {
        return enableEditingForJoinQueries;
    }

    public void setEnableEditingForJoinQueries(boolean enableEditingForJoinQueries) {
        this.enableEditingForJoinQueries = enableEditingForJoinQueries;
    }

    public boolean isShowDmlPreviewForJoinQueries() {
        return showDmlPreviewForJoinQueries;
    }

    public void setShowDmlPreviewForJoinQueries(boolean showDmlPreviewForJoinQueries) {
        this.showDmlPreviewForJoinQueries = showDmlPreviewForJoinQueries;
    }

    public boolean isAllowOpeningSecureLinks() {
        return allowOpeningSecureLinks;
    }

    public void setAllowOpeningSecureLinks(boolean allowOpeningSecureLinks) {
        this.allowOpeningSecureLinks = allowOpeningSecureLinks;
    }

    public boolean isAllowOpeningStandardLinks() {
        return allowOpeningStandardLinks;
    }

    public void setAllowOpeningStandardLinks(boolean allowOpeningStandardLinks) {
        this.allowOpeningStandardLinks = allowOpeningStandardLinks;
    }

    public boolean isAllowOpeningLocalFileLinks() {
        return allowOpeningLocalFileLinks;
    }

    public void setAllowOpeningLocalFileLinks(boolean allowOpeningLocalFileLinks) {
        this.allowOpeningLocalFileLinks = allowOpeningLocalFileLinks;
    }

    public boolean isAssumeHttpIfNoProtocol() {
        return assumeHttpIfNoProtocol;
    }

    public void setAssumeHttpIfNoProtocol(boolean assumeHttpIfNoProtocol) {
        this.assumeHttpIfNoProtocol = assumeHttpIfNoProtocol;
    }

    @Override
    public DatabaseDataEditorSettings clone() {
        try {
            return (DatabaseDataEditorSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatabaseDataEditorSettings that = (DatabaseDataEditorSettings) o;
        return limitPageSize == that.limitPageSize &&
                pageSize == that.pageSize &&
                resultSetPrefetchSize == that.resultSetPrefetchSize &&
                filterHistorySize == that.filterHistorySize &&
                maxBytesPerValue == that.maxBytesPerValue &&
                showFirstDataRowsInPreview == that.showFirstDataRowsInPreview &&
                previewRowCount == that.previewRowCount &&
                enablePagingInEditorResults == that.enablePagingInEditorResults &&
                showQuickActionsPopupToolbar == that.showQuickActionsPopupToolbar &&
                enableQuickActionsCustomization == that.enableQuickActionsCustomization &&
                detectBinaryText == that.detectBinaryText &&
                detectBinaryUuid == that.detectBinaryUuid &&
                enableLocalFilterByDefault == that.enableLocalFilterByDefault &&
                enableImmediateCompletion == that.enableImmediateCompletion &&
                useGroupingSeparator == that.useGroupingSeparator &&
                useNumberPattern == that.useNumberPattern &&
                useDatetimeTimestamp == that.useDatetimeTimestamp &&
                useDatetimeWithTz == that.useDatetimeWithTz &&
                useTime == that.useTime &&
                useTimeWithTz == that.useTimeWithTz &&
                useDate == that.useDate &&
                sortViaOrderBy == that.sortViaOrderBy &&
                sortTablesByNumericPrimaryKey == that.sortTablesByNumericPrimaryKey &&
                submitChangesImmediately == that.submitChangesImmediately &&
                enableEditingForJoinQueries == that.enableEditingForJoinQueries &&
                showDmlPreviewForJoinQueries == that.showDmlPreviewForJoinQueries &&
                allowOpeningSecureLinks == that.allowOpeningSecureLinks &&
                allowOpeningStandardLinks == that.allowOpeningStandardLinks &&
                allowOpeningLocalFileLinks == that.allowOpeningLocalFileLinks &&
                assumeHttpIfNoProtocol == that.assumeHttpIfNoProtocol &&
                Objects.equals(gridPaginationControlPosition, that.gridPaginationControlPosition) &&
                Objects.equals(automaticallyTransposeTables, that.automaticallyTransposeTables) &&
                Objects.equals(displayTemporalDataTimeZone, that.displayTemporalDataTimeZone) &&
                Objects.equals(decimalSeparator, that.decimalSeparator) &&
                Objects.equals(groupingSeparator, that.groupingSeparator) &&
                Objects.equals(infinityText, that.infinityText) &&
                Objects.equals(nanText, that.nanText) &&
                Objects.equals(numberPattern, that.numberPattern) &&
                Objects.equals(datetimeTimestampPattern, that.datetimeTimestampPattern) &&
                Objects.equals(datetimeWithTzPattern, that.datetimeWithTzPattern) &&
                Objects.equals(timePattern, that.timePattern) &&
                Objects.equals(timeWithTzPattern, that.timeWithTzPattern) &&
                Objects.equals(datePattern, that.datePattern) &&
                Objects.equals(numericPrimaryKeyDirection, that.numericPrimaryKeyDirection) &&
                Objects.equals(addColumnsToSorting, that.addColumnsToSorting);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limitPageSize, pageSize, resultSetPrefetchSize, filterHistorySize, maxBytesPerValue,
                showFirstDataRowsInPreview, previewRowCount, enablePagingInEditorResults,
                gridPaginationControlPosition, showQuickActionsPopupToolbar, enableQuickActionsCustomization,
                automaticallyTransposeTables, detectBinaryText, detectBinaryUuid, enableLocalFilterByDefault,
                enableImmediateCompletion, displayTemporalDataTimeZone, decimalSeparator, useGroupingSeparator,
                groupingSeparator, infinityText, nanText, useNumberPattern, numberPattern, useDatetimeTimestamp,
                datetimeTimestampPattern, useDatetimeWithTz, datetimeWithTzPattern, useTime, timePattern,
                useTimeWithTz, timeWithTzPattern, useDate, datePattern, sortViaOrderBy,
                sortTablesByNumericPrimaryKey, numericPrimaryKeyDirection, addColumnsToSorting,
                submitChangesImmediately, enableEditingForJoinQueries, showDmlPreviewForJoinQueries,
                allowOpeningSecureLinks, allowOpeningStandardLinks, allowOpeningLocalFileLinks,
                assumeHttpIfNoProtocol);
    }
}
