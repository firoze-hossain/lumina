package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Tasks > Time Tracking settings in Lumina IDE.
 */
public class TimeTrackingSettings implements Cloneable {

    private boolean enableTimeTracking = false;
    private int suspendDelaySeconds = 600;

    public TimeTrackingSettings() {
    }

    public boolean isEnableTimeTracking() {
        return enableTimeTracking;
    }

    public void setEnableTimeTracking(boolean enableTimeTracking) {
        this.enableTimeTracking = enableTimeTracking;
    }

    public int getSuspendDelaySeconds() {
        return suspendDelaySeconds;
    }

    public void setSuspendDelaySeconds(int suspendDelaySeconds) {
        this.suspendDelaySeconds = suspendDelaySeconds > 0 ? suspendDelaySeconds : 600;
    }

    @Override
    public TimeTrackingSettings clone() {
        try {
            return (TimeTrackingSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            TimeTrackingSettings copy = new TimeTrackingSettings();
            copy.enableTimeTracking = this.enableTimeTracking;
            copy.suspendDelaySeconds = this.suspendDelaySeconds;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeTrackingSettings that = (TimeTrackingSettings) o;
        return enableTimeTracking == that.enableTimeTracking &&
                suspendDelaySeconds == that.suspendDelaySeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableTimeTracking, suspendDelaySeconds);
    }
}
