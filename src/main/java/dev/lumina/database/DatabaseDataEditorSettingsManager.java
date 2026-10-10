package dev.lumina.database;

import dev.lumina.util.Settings;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Dynamic configuration manager for Database Data Editor and Viewer settings in Lumina IDE.
 */
public class DatabaseDataEditorSettingsManager {

    private static final String KEY_PREFIX = "database.data_editor.";

    private static volatile DatabaseDataEditorSettingsManager instance;

    private DatabaseDataEditorSettings currentSettings;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private DatabaseDataEditorSettingsManager() {
        this.currentSettings = loadSettings();
    }

    public static DatabaseDataEditorSettingsManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseDataEditorSettingsManager.class) {
                if (instance == null) {
                    instance = new DatabaseDataEditorSettingsManager();
                }
            }
        }
        return instance;
    }

    public synchronized DatabaseDataEditorSettings getSettings() {
        if (currentSettings == null) {
            currentSettings = loadSettings();
        }
        return currentSettings.clone();
    }

    public synchronized void setSettings(DatabaseDataEditorSettings newSettings) {
        if (newSettings == null) {
            this.currentSettings = new DatabaseDataEditorSettings();
        } else {
            this.currentSettings = newSettings.clone();
        }
        saveSettings();
        notifyListeners();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Exception ignored) {
            }
        }
    }

    private DatabaseDataEditorSettings loadSettings() {
        DatabaseDataEditorSettings s = new DatabaseDataEditorSettings();

        // Limitations
        String limitPage = Settings.get(KEY_PREFIX + "limit_page_size");
        if (limitPage != null) s.setLimitPageSize(Boolean.parseBoolean(limitPage));

        String pageSize = Settings.get(KEY_PREFIX + "page_size");
        if (pageSize != null) {
            try { s.setPageSize(Integer.parseInt(pageSize)); } catch (NumberFormatException ignored) {}
        }

        String prefetchSize = Settings.get(KEY_PREFIX + "prefetch_size");
        if (prefetchSize != null) {
            try { s.setResultSetPrefetchSize(Integer.parseInt(prefetchSize)); } catch (NumberFormatException ignored) {}
        }

        String filterHistory = Settings.get(KEY_PREFIX + "filter_history_size");
        if (filterHistory != null) {
            try { s.setFilterHistorySize(Integer.parseInt(filterHistory)); } catch (NumberFormatException ignored) {}
        }

        String maxBytes = Settings.get(KEY_PREFIX + "max_bytes_per_value");
        if (maxBytes != null) {
            try { s.setMaxBytesPerValue(Integer.parseInt(maxBytes)); } catch (NumberFormatException ignored) {}
        }

        String showFirst = Settings.get(KEY_PREFIX + "show_first_rows");
        if (showFirst != null) s.setShowFirstDataRowsInPreview(Boolean.parseBoolean(showFirst));

        String previewRows = Settings.get(KEY_PREFIX + "preview_row_count");
        if (previewRows != null) {
            try { s.setPreviewRowCount(Integer.parseInt(previewRows)); } catch (NumberFormatException ignored) {}
        }

        // Controls Customization
        String pagingInEditor = Settings.get(KEY_PREFIX + "paging_in_editor");
        if (pagingInEditor != null) s.setEnablePagingInEditorResults(Boolean.parseBoolean(pagingInEditor));

        String paginationPos = Settings.get(KEY_PREFIX + "grid_pagination_pos");
        if (paginationPos != null) s.setGridPaginationControlPosition(paginationPos);

        String quickActions = Settings.get(KEY_PREFIX + "quick_actions_toolbar");
        if (quickActions != null) s.setShowQuickActionsPopupToolbar(Boolean.parseBoolean(quickActions));

        String quickActionsCustom = Settings.get(KEY_PREFIX + "quick_actions_customization");
        if (quickActionsCustom != null) s.setEnableQuickActionsCustomization(Boolean.parseBoolean(quickActionsCustom));

        // Data Presentation
        String transpose = Settings.get(KEY_PREFIX + "transpose_tables");
        if (transpose != null) s.setAutomaticallyTransposeTables(transpose);

        String binText = Settings.get(KEY_PREFIX + "detect_bin_text");
        if (binText != null) s.setDetectBinaryText(Boolean.parseBoolean(binText));

        String binUuid = Settings.get(KEY_PREFIX + "detect_bin_uuid");
        if (binUuid != null) s.setDetectBinaryUuid(Boolean.parseBoolean(binUuid));

        String localFilter = Settings.get(KEY_PREFIX + "local_filter");
        if (localFilter != null) s.setEnableLocalFilterByDefault(Boolean.parseBoolean(localFilter));

        String immComp = Settings.get(KEY_PREFIX + "immediate_completion");
        if (immComp != null) s.setEnableImmediateCompletion(Boolean.parseBoolean(immComp));

        String tz = Settings.get(KEY_PREFIX + "temporal_tz");
        if (tz != null) s.setDisplayTemporalDataTimeZone(tz);

        // Custom Number Formats
        String decSep = Settings.get(KEY_PREFIX + "decimal_sep");
        if (decSep != null) s.setDecimalSeparator(decSep);

        String useGrp = Settings.get(KEY_PREFIX + "use_grouping_sep");
        if (useGrp != null) s.setUseGroupingSeparator(Boolean.parseBoolean(useGrp));

        String grpSep = Settings.get(KEY_PREFIX + "grouping_sep");
        if (grpSep != null) s.setGroupingSeparator(grpSep);

        String inf = Settings.get(KEY_PREFIX + "infinity_text");
        if (inf != null) s.setInfinityText(inf);

        String nan = Settings.get(KEY_PREFIX + "nan_text");
        if (nan != null) s.setNanText(nan);

        String useNumPat = Settings.get(KEY_PREFIX + "use_number_pattern");
        if (useNumPat != null) s.setUseNumberPattern(Boolean.parseBoolean(useNumPat));

        String numPat = Settings.get(KEY_PREFIX + "number_pattern");
        if (numPat != null) s.setNumberPattern(numPat);

        // Custom Date/Time Formats
        String useDt = Settings.get(KEY_PREFIX + "use_datetime");
        if (useDt != null) s.setUseDatetimeTimestamp(Boolean.parseBoolean(useDt));

        String dtPat = Settings.get(KEY_PREFIX + "datetime_pattern");
        if (dtPat != null) s.setDatetimeTimestampPattern(dtPat);

        String useDtTz = Settings.get(KEY_PREFIX + "use_datetime_tz");
        if (useDtTz != null) s.setUseDatetimeWithTz(Boolean.parseBoolean(useDtTz));

        String dtTzPat = Settings.get(KEY_PREFIX + "datetime_tz_pattern");
        if (dtTzPat != null) s.setDatetimeWithTzPattern(dtTzPat);

        String useTm = Settings.get(KEY_PREFIX + "use_time");
        if (useTm != null) s.setUseTime(Boolean.parseBoolean(useTm));

        String tmPat = Settings.get(KEY_PREFIX + "time_pattern");
        if (tmPat != null) s.setTimePattern(tmPat);

        String useTmTz = Settings.get(KEY_PREFIX + "use_time_tz");
        if (useTmTz != null) s.setUseTimeWithTz(Boolean.parseBoolean(useTmTz));

        String tmTzPat = Settings.get(KEY_PREFIX + "time_tz_pattern");
        if (tmTzPat != null) s.setTimeWithTzPattern(tmTzPat);

        String useDate = Settings.get(KEY_PREFIX + "use_date");
        if (useDate != null) s.setUseDate(Boolean.parseBoolean(useDate));

        String datePat = Settings.get(KEY_PREFIX + "date_pattern");
        if (datePat != null) s.setDatePattern(datePat);

        // Data Sorting
        String sortOrder = Settings.get(KEY_PREFIX + "sort_order_by");
        if (sortOrder != null) s.setSortViaOrderBy(Boolean.parseBoolean(sortOrder));

        String sortPk = Settings.get(KEY_PREFIX + "sort_numeric_pk");
        if (sortPk != null) s.setSortTablesByNumericPrimaryKey(Boolean.parseBoolean(sortPk));

        String pkDir = Settings.get(KEY_PREFIX + "numeric_pk_dir");
        if (pkDir != null) s.setNumericPrimaryKeyDirection(pkDir);

        String addCol = Settings.get(KEY_PREFIX + "add_col_sort_key");
        if (addCol != null) s.setAddColumnsToSorting(addCol);

        // Data Modification
        String subImm = Settings.get(KEY_PREFIX + "submit_immediately");
        if (subImm != null) s.setSubmitChangesImmediately(Boolean.parseBoolean(subImm));

        String editJoin = Settings.get(KEY_PREFIX + "edit_join_queries");
        if (editJoin != null) s.setEnableEditingForJoinQueries(Boolean.parseBoolean(editJoin));

        String dmlPrev = Settings.get(KEY_PREFIX + "dml_preview_join");
        if (dmlPrev != null) s.setShowDmlPreviewForJoinQueries(Boolean.parseBoolean(dmlPrev));

        // URL Click Settings
        String secLinks = Settings.get(KEY_PREFIX + "allow_https");
        if (secLinks != null) s.setAllowOpeningSecureLinks(Boolean.parseBoolean(secLinks));

        String stdLinks = Settings.get(KEY_PREFIX + "allow_http");
        if (stdLinks != null) s.setAllowOpeningStandardLinks(Boolean.parseBoolean(stdLinks));

        String fileLinks = Settings.get(KEY_PREFIX + "allow_local_files");
        if (fileLinks != null) s.setAllowOpeningLocalFileLinks(Boolean.parseBoolean(fileLinks));

        String assumeHttp = Settings.get(KEY_PREFIX + "assume_http");
        if (assumeHttp != null) s.setAssumeHttpIfNoProtocol(Boolean.parseBoolean(assumeHttp));

        return s;
    }

    private void saveSettings() {
        if (currentSettings == null) return;

        Settings.put(KEY_PREFIX + "limit_page_size", String.valueOf(currentSettings.isLimitPageSize()));
        Settings.put(KEY_PREFIX + "page_size", String.valueOf(currentSettings.getPageSize()));
        Settings.put(KEY_PREFIX + "prefetch_size", String.valueOf(currentSettings.getResultSetPrefetchSize()));
        Settings.put(KEY_PREFIX + "filter_history_size", String.valueOf(currentSettings.getFilterHistorySize()));
        Settings.put(KEY_PREFIX + "max_bytes_per_value", String.valueOf(currentSettings.getMaxBytesPerValue()));
        Settings.put(KEY_PREFIX + "show_first_rows", String.valueOf(currentSettings.isShowFirstDataRowsInPreview()));
        Settings.put(KEY_PREFIX + "preview_row_count", String.valueOf(currentSettings.getPreviewRowCount()));

        Settings.put(KEY_PREFIX + "paging_in_editor", String.valueOf(currentSettings.isEnablePagingInEditorResults()));
        Settings.put(KEY_PREFIX + "grid_pagination_pos", currentSettings.getGridPaginationControlPosition());
        Settings.put(KEY_PREFIX + "quick_actions_toolbar", String.valueOf(currentSettings.isShowQuickActionsPopupToolbar()));
        Settings.put(KEY_PREFIX + "quick_actions_customization", String.valueOf(currentSettings.isEnableQuickActionsCustomization()));

        Settings.put(KEY_PREFIX + "transpose_tables", currentSettings.getAutomaticallyTransposeTables());
        Settings.put(KEY_PREFIX + "detect_bin_text", String.valueOf(currentSettings.isDetectBinaryText()));
        Settings.put(KEY_PREFIX + "detect_bin_uuid", String.valueOf(currentSettings.isDetectBinaryUuid()));
        Settings.put(KEY_PREFIX + "local_filter", String.valueOf(currentSettings.isEnableLocalFilterByDefault()));
        Settings.put(KEY_PREFIX + "immediate_completion", String.valueOf(currentSettings.isEnableImmediateCompletion()));
        Settings.put(KEY_PREFIX + "temporal_tz", currentSettings.getDisplayTemporalDataTimeZone());

        Settings.put(KEY_PREFIX + "decimal_sep", currentSettings.getDecimalSeparator());
        Settings.put(KEY_PREFIX + "use_grouping_sep", String.valueOf(currentSettings.isUseGroupingSeparator()));
        Settings.put(KEY_PREFIX + "grouping_sep", currentSettings.getGroupingSeparator());
        Settings.put(KEY_PREFIX + "infinity_text", currentSettings.getInfinityText());
        Settings.put(KEY_PREFIX + "nan_text", currentSettings.getNanText());
        Settings.put(KEY_PREFIX + "use_number_pattern", String.valueOf(currentSettings.isUseNumberPattern()));
        Settings.put(KEY_PREFIX + "number_pattern", currentSettings.getNumberPattern());

        Settings.put(KEY_PREFIX + "use_datetime", String.valueOf(currentSettings.isUseDatetimeTimestamp()));
        Settings.put(KEY_PREFIX + "datetime_pattern", currentSettings.getDatetimeTimestampPattern());
        Settings.put(KEY_PREFIX + "use_datetime_tz", String.valueOf(currentSettings.isUseDatetimeWithTz()));
        Settings.put(KEY_PREFIX + "datetime_tz_pattern", currentSettings.getDatetimeWithTzPattern());
        Settings.put(KEY_PREFIX + "use_time", String.valueOf(currentSettings.isUseTime()));
        Settings.put(KEY_PREFIX + "time_pattern", currentSettings.getTimePattern());
        Settings.put(KEY_PREFIX + "use_time_tz", String.valueOf(currentSettings.isUseTimeWithTz()));
        Settings.put(KEY_PREFIX + "time_tz_pattern", currentSettings.getTimeWithTzPattern());
        Settings.put(KEY_PREFIX + "use_date", String.valueOf(currentSettings.isUseDate()));
        Settings.put(KEY_PREFIX + "date_pattern", currentSettings.getDatePattern());

        Settings.put(KEY_PREFIX + "sort_order_by", String.valueOf(currentSettings.isSortViaOrderBy()));
        Settings.put(KEY_PREFIX + "sort_numeric_pk", String.valueOf(currentSettings.isSortTablesByNumericPrimaryKey()));
        Settings.put(KEY_PREFIX + "numeric_pk_dir", currentSettings.getNumericPrimaryKeyDirection());
        Settings.put(KEY_PREFIX + "add_col_sort_key", currentSettings.getAddColumnsToSorting());

        Settings.put(KEY_PREFIX + "submit_immediately", String.valueOf(currentSettings.isSubmitChangesImmediately()));
        Settings.put(KEY_PREFIX + "edit_join_queries", String.valueOf(currentSettings.isEnableEditingForJoinQueries()));
        Settings.put(KEY_PREFIX + "dml_preview_join", String.valueOf(currentSettings.isShowDmlPreviewForJoinQueries()));

        Settings.put(KEY_PREFIX + "allow_https", String.valueOf(currentSettings.isAllowOpeningSecureLinks()));
        Settings.put(KEY_PREFIX + "allow_http", String.valueOf(currentSettings.isAllowOpeningStandardLinks()));
        Settings.put(KEY_PREFIX + "allow_local_files", String.valueOf(currentSettings.isAllowOpeningLocalFileLinks()));
        Settings.put(KEY_PREFIX + "assume_http", String.valueOf(currentSettings.isAssumeHttpIfNoProtocol()));
    }
}
