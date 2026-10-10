package dev.lumina.scala;

import java.util.Objects;

/**
 * Model for Languages & Frameworks > Scala > Updates settings.
 * Matches Image 1:
 *  - Update channel: default "Stable Releases" ("Stable Releases", "Early Access Program", "Nightly")
 */
public class ScalaUpdatesSettings {

    public static final String CHANNEL_STABLE = "Stable Releases";
    public static final String CHANNEL_EAP = "Early Access Program";
    public static final String CHANNEL_NIGHTLY = "Nightly";

    private String updateChannel = CHANNEL_STABLE;
    private long lastCheckedTimestamp = 0;

    public ScalaUpdatesSettings() {
    }

    public ScalaUpdatesSettings(String updateChannel, long lastCheckedTimestamp) {
        this.updateChannel = updateChannel != null ? updateChannel : CHANNEL_STABLE;
        this.lastCheckedTimestamp = lastCheckedTimestamp;
    }

    public String getUpdateChannel() {
        return updateChannel;
    }

    public void setUpdateChannel(String updateChannel) {
        this.updateChannel = updateChannel != null ? updateChannel : CHANNEL_STABLE;
    }

    public long getLastCheckedTimestamp() {
        return lastCheckedTimestamp;
    }

    public void setLastCheckedTimestamp(long lastCheckedTimestamp) {
        this.lastCheckedTimestamp = lastCheckedTimestamp;
    }

    public ScalaUpdatesSettings copy() {
        return new ScalaUpdatesSettings(updateChannel, lastCheckedTimestamp);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScalaUpdatesSettings that = (ScalaUpdatesSettings) o;
        return lastCheckedTimestamp == that.lastCheckedTimestamp &&
                Objects.equals(updateChannel, that.updateChannel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(updateChannel, lastCheckedTimestamp);
    }

    @Override
    public String toString() {
        return "ScalaUpdatesSettings{" +
                "updateChannel='" + updateChannel + '\'' +
                ", lastCheckedTimestamp=" + lastCheckedTimestamp +
                '}';
    }
}
