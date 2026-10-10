package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Python Plots configuration settings in Lumina IDE.
 */
public class PythonPlotsSettings implements Cloneable {

    private boolean showPlotsInToolWindow = true;
    private boolean useMpld3InteractivePlots = true;
    private int maxPlotsCount = 200;
    private boolean suggestInstallKaleido = true;
    private boolean suggestInstallMpld3 = false;
    private boolean suggestInstallPillow = true;

    public PythonPlotsSettings() {
    }

    public boolean isShowPlotsInToolWindow() {
        return showPlotsInToolWindow;
    }

    public void setShowPlotsInToolWindow(boolean showPlotsInToolWindow) {
        this.showPlotsInToolWindow = showPlotsInToolWindow;
    }

    public boolean isUseMpld3InteractivePlots() {
        return useMpld3InteractivePlots;
    }

    public void setUseMpld3InteractivePlots(boolean useMpld3InteractivePlots) {
        this.useMpld3InteractivePlots = useMpld3InteractivePlots;
    }

    public int getMaxPlotsCount() {
        return maxPlotsCount;
    }

    public void setMaxPlotsCount(int maxPlotsCount) {
        this.maxPlotsCount = maxPlotsCount > 0 ? maxPlotsCount : 200;
    }

    public boolean isSuggestInstallKaleido() {
        return suggestInstallKaleido;
    }

    public void setSuggestInstallKaleido(boolean suggestInstallKaleido) {
        this.suggestInstallKaleido = suggestInstallKaleido;
    }

    public boolean isSuggestInstallMpld3() {
        return suggestInstallMpld3;
    }

    public void setSuggestInstallMpld3(boolean suggestInstallMpld3) {
        this.suggestInstallMpld3 = suggestInstallMpld3;
    }

    public boolean isSuggestInstallPillow() {
        return suggestInstallPillow;
    }

    public void setSuggestInstallPillow(boolean suggestInstallPillow) {
        this.suggestInstallPillow = suggestInstallPillow;
    }

    public PythonPlotsSettings copy() {
        return clone();
    }

    @Override
    public PythonPlotsSettings clone() {
        try {
            return (PythonPlotsSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            PythonPlotsSettings copy = new PythonPlotsSettings();
            copy.showPlotsInToolWindow = this.showPlotsInToolWindow;
            copy.useMpld3InteractivePlots = this.useMpld3InteractivePlots;
            copy.maxPlotsCount = this.maxPlotsCount;
            copy.suggestInstallKaleido = this.suggestInstallKaleido;
            copy.suggestInstallMpld3 = this.suggestInstallMpld3;
            copy.suggestInstallPillow = this.suggestInstallPillow;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PythonPlotsSettings that = (PythonPlotsSettings) o;
        return showPlotsInToolWindow == that.showPlotsInToolWindow &&
                useMpld3InteractivePlots == that.useMpld3InteractivePlots &&
                maxPlotsCount == that.maxPlotsCount &&
                suggestInstallKaleido == that.suggestInstallKaleido &&
                suggestInstallMpld3 == that.suggestInstallMpld3 &&
                suggestInstallPillow == that.suggestInstallPillow;
    }

    @Override
    public int hashCode() {
        return Objects.hash(showPlotsInToolWindow, useMpld3InteractivePlots, maxPlotsCount,
                suggestInstallKaleido, suggestInstallMpld3, suggestInstallPillow);
    }
}
