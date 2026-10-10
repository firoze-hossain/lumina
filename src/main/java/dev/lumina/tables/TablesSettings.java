package dev.lumina.tables;

import java.util.Objects;

/**
 * Configuration model for table viewing, statistics, and column rendering settings.
 */
public class TablesSettings {

    public static final String MODE_OFF = "Off";
    public static final String MODE_COMPACT = "Compact";
    public static final String MODE_DETAILED = "Detailed";

    public static final int DEFAULT_MAX_COLUMNS = 1200;

    private String columnStatisticsMode = MODE_OFF;
    private boolean autoCompactForSmallTables = true;
    private boolean displayTensorsAsTable = true;
    private boolean limitRenderedColumns = true;
    private int maxRenderedColumns = DEFAULT_MAX_COLUMNS;
    private boolean enableLocalFiltersByDefault = false;

    public TablesSettings() {
    }

    public TablesSettings(TablesSettings other) {
        if (other != null) {
            this.columnStatisticsMode = other.columnStatisticsMode;
            this.autoCompactForSmallTables = other.autoCompactForSmallTables;
            this.displayTensorsAsTable = other.displayTensorsAsTable;
            this.limitRenderedColumns = other.limitRenderedColumns;
            this.maxRenderedColumns = other.maxRenderedColumns;
            this.enableLocalFiltersByDefault = other.enableLocalFiltersByDefault;
        }
    }

    public String getColumnStatisticsMode() {
        return columnStatisticsMode;
    }

    public void setColumnStatisticsMode(String columnStatisticsMode) {
        this.columnStatisticsMode = columnStatisticsMode != null ? columnStatisticsMode : MODE_OFF;
    }

    public boolean isAutoCompactForSmallTables() {
        return autoCompactForSmallTables;
    }

    public void setAutoCompactForSmallTables(boolean autoCompactForSmallTables) {
        this.autoCompactForSmallTables = autoCompactForSmallTables;
    }

    public boolean isDisplayTensorsAsTable() {
        return displayTensorsAsTable;
    }

    public void setDisplayTensorsAsTable(boolean displayTensorsAsTable) {
        this.displayTensorsAsTable = displayTensorsAsTable;
    }

    public boolean isLimitRenderedColumns() {
        return limitRenderedColumns;
    }

    public void setLimitRenderedColumns(boolean limitRenderedColumns) {
        this.limitRenderedColumns = limitRenderedColumns;
    }

    public int getMaxRenderedColumns() {
        return maxRenderedColumns;
    }

    public void setMaxRenderedColumns(int maxRenderedColumns) {
        this.maxRenderedColumns = Math.max(1, maxRenderedColumns);
    }

    public boolean isEnableLocalFiltersByDefault() {
        return enableLocalFiltersByDefault;
    }

    public void setEnableLocalFiltersByDefault(boolean enableLocalFiltersByDefault) {
        this.enableLocalFiltersByDefault = enableLocalFiltersByDefault;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TablesSettings that = (TablesSettings) o;
        return autoCompactForSmallTables == that.autoCompactForSmallTables &&
                displayTensorsAsTable == that.displayTensorsAsTable &&
                limitRenderedColumns == that.limitRenderedColumns &&
                maxRenderedColumns == that.maxRenderedColumns &&
                enableLocalFiltersByDefault == that.enableLocalFiltersByDefault &&
                Objects.equals(columnStatisticsMode, that.columnStatisticsMode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnStatisticsMode, autoCompactForSmallTables, displayTensorsAsTable,
                limitRenderedColumns, maxRenderedColumns, enableLocalFiltersByDefault);
    }
}
