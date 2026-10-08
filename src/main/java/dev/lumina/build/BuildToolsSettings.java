package dev.lumina.build;

import java.util.Objects;

/**
 * Model representing Build Tools settings in Lumina IDE.
 * Matches Build, Execution, Deployment > Build Tools.
 */
public class BuildToolsSettings implements Cloneable {

    public enum SyncTrigger {
        ANY_CHANGES("Any changes"),
        EXTERNAL_CHANGES("External changes");

        private final String label;

        SyncTrigger(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private boolean syncOnBuildScriptChanges = true;
    private SyncTrigger syncTrigger = SyncTrigger.EXTERNAL_CHANGES;

    public BuildToolsSettings() {
    }

    public BuildToolsSettings(BuildToolsSettings other) {
        if (other != null) {
            this.syncOnBuildScriptChanges = other.syncOnBuildScriptChanges;
            this.syncTrigger = other.syncTrigger != null ? other.syncTrigger : SyncTrigger.EXTERNAL_CHANGES;
        }
    }

    public boolean isSyncOnBuildScriptChanges() {
        return syncOnBuildScriptChanges;
    }

    public void setSyncOnBuildScriptChanges(boolean syncOnBuildScriptChanges) {
        this.syncOnBuildScriptChanges = syncOnBuildScriptChanges;
    }

    public SyncTrigger getSyncTrigger() {
        return syncTrigger;
    }

    public void setSyncTrigger(SyncTrigger syncTrigger) {
        this.syncTrigger = syncTrigger != null ? syncTrigger : SyncTrigger.EXTERNAL_CHANGES;
    }

    @Override
    public BuildToolsSettings clone() {
        return new BuildToolsSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildToolsSettings that = (BuildToolsSettings) o;
        return syncOnBuildScriptChanges == that.syncOnBuildScriptChanges &&
                syncTrigger == that.syncTrigger;
    }

    @Override
    public int hashCode() {
        return Objects.hash(syncOnBuildScriptChanges, syncTrigger);
    }
}
