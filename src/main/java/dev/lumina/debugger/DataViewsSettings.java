package dev.lumina.debugger;

import java.util.Objects;

/**
 * Settings for Build, Execution, Deployment > Debugger > Data Views (Image 3).
 */
public class DataViewsSettings implements Cloneable {

    private boolean sortValuesAlphabetically = false;
    private boolean enableAutoExpressions = true;

    // Editor section
    private boolean showValuesInline = true;
    private boolean showValueTooltip = true;
    private int valueTooltipDelayMs = 700;
    private boolean showValueTooltipOnCodeSelection = false;

    public DataViewsSettings() {
    }

    public DataViewsSettings(DataViewsSettings other) {
        if (other != null) {
            this.sortValuesAlphabetically = other.sortValuesAlphabetically;
            this.enableAutoExpressions = other.enableAutoExpressions;
            this.showValuesInline = other.showValuesInline;
            this.showValueTooltip = other.showValueTooltip;
            this.valueTooltipDelayMs = other.valueTooltipDelayMs;
            this.showValueTooltipOnCodeSelection = other.showValueTooltipOnCodeSelection;
        }
    }

    public boolean isSortValuesAlphabetically() {
        return sortValuesAlphabetically;
    }

    public void setSortValuesAlphabetically(boolean sortValuesAlphabetically) {
        this.sortValuesAlphabetically = sortValuesAlphabetically;
    }

    public boolean isEnableAutoExpressions() {
        return enableAutoExpressions;
    }

    public void setEnableAutoExpressions(boolean enableAutoExpressions) {
        this.enableAutoExpressions = enableAutoExpressions;
    }

    public boolean isShowValuesInline() {
        return showValuesInline;
    }

    public void setShowValuesInline(boolean showValuesInline) {
        this.showValuesInline = showValuesInline;
    }

    public boolean isShowValueTooltip() {
        return showValueTooltip;
    }

    public void setShowValueTooltip(boolean showValueTooltip) {
        this.showValueTooltip = showValueTooltip;
    }

    public int getValueTooltipDelayMs() {
        return valueTooltipDelayMs;
    }

    public void setValueTooltipDelayMs(int valueTooltipDelayMs) {
        this.valueTooltipDelayMs = valueTooltipDelayMs;
    }

    public boolean isShowValueTooltipOnCodeSelection() {
        return showValueTooltipOnCodeSelection;
    }

    public void setShowValueTooltipOnCodeSelection(boolean showValueTooltipOnCodeSelection) {
        this.showValueTooltipOnCodeSelection = showValueTooltipOnCodeSelection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataViewsSettings that)) return false;
        return sortValuesAlphabetically == that.sortValuesAlphabetically &&
                enableAutoExpressions == that.enableAutoExpressions &&
                showValuesInline == that.showValuesInline &&
                showValueTooltip == that.showValueTooltip &&
                valueTooltipDelayMs == that.valueTooltipDelayMs &&
                showValueTooltipOnCodeSelection == that.showValueTooltipOnCodeSelection;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sortValuesAlphabetically, enableAutoExpressions,
                showValuesInline, showValueTooltip, valueTooltipDelayMs,
                showValueTooltipOnCodeSelection);
    }

    @Override
    public DataViewsSettings clone() {
        return new DataViewsSettings(this);
    }
}
