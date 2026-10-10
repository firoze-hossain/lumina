package dev.lumina.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Model representing Backup and Sync configuration settings in Lumina IDE.
 * 1:1 specification match with reference settings screen.
 */
public class BackupAndSyncSettings implements Cloneable {

    private boolean enableBackupAndSync = true;
    private String syncAccount = "15103202@iubat.edu";
    private List<String> availableAccounts = new ArrayList<>();

    // Detailed sync categories
    private boolean syncUi = true;
    private boolean syncCodeAndSystem = true;
    private boolean syncKeymaps = true;
    private boolean syncPlugins = true;
    private boolean syncTools = true;

    private long lastSyncTimeMs = System.currentTimeMillis();

    public BackupAndSyncSettings() {
        this.availableAccounts.add("15103202@iubat.edu");
    }

    public boolean isEnableBackupAndSync() {
        return enableBackupAndSync;
    }

    public void setEnableBackupAndSync(boolean enableBackupAndSync) {
        this.enableBackupAndSync = enableBackupAndSync;
    }

    public String getSyncAccount() {
        return syncAccount != null ? syncAccount : "15103202@iubat.edu";
    }

    public void setSyncAccount(String syncAccount) {
        this.syncAccount = syncAccount != null ? syncAccount : "15103202@iubat.edu";
    }

    public List<String> getAvailableAccounts() {
        if (availableAccounts == null) availableAccounts = new ArrayList<>();
        return availableAccounts;
    }

    public void setAvailableAccounts(List<String> availableAccounts) {
        this.availableAccounts = availableAccounts != null ? availableAccounts : new ArrayList<>();
    }

    public boolean isSyncUi() {
        return syncUi;
    }

    public void setSyncUi(boolean syncUi) {
        this.syncUi = syncUi;
    }

    public boolean isSyncCodeAndSystem() {
        return syncCodeAndSystem;
    }

    public void setSyncCodeAndSystem(boolean syncCodeAndSystem) {
        this.syncCodeAndSystem = syncCodeAndSystem;
    }

    public boolean isSyncKeymaps() {
        return syncKeymaps;
    }

    public void setSyncKeymaps(boolean syncKeymaps) {
        this.syncKeymaps = syncKeymaps;
    }

    public boolean isSyncPlugins() {
        return syncPlugins;
    }

    public void setSyncPlugins(boolean syncPlugins) {
        this.syncPlugins = syncPlugins;
    }

    public boolean isSyncTools() {
        return syncTools;
    }

    public void setSyncTools(boolean syncTools) {
        this.syncTools = syncTools;
    }

    public long getLastSyncTimeMs() {
        return lastSyncTimeMs;
    }

    public void setLastSyncTimeMs(long lastSyncTimeMs) {
        this.lastSyncTimeMs = lastSyncTimeMs;
    }

    @Override
    public BackupAndSyncSettings clone() {
        try {
            BackupAndSyncSettings copy = (BackupAndSyncSettings) super.clone();
            copy.availableAccounts = new ArrayList<>(this.availableAccounts);
            return copy;
        } catch (CloneNotSupportedException e) {
            BackupAndSyncSettings copy = new BackupAndSyncSettings();
            copy.enableBackupAndSync = this.enableBackupAndSync;
            copy.syncAccount = this.syncAccount;
            copy.availableAccounts = new ArrayList<>(this.availableAccounts);
            copy.syncUi = this.syncUi;
            copy.syncCodeAndSystem = this.syncCodeAndSystem;
            copy.syncKeymaps = this.syncKeymaps;
            copy.syncPlugins = this.syncPlugins;
            copy.syncTools = this.syncTools;
            copy.lastSyncTimeMs = this.lastSyncTimeMs;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BackupAndSyncSettings that = (BackupAndSyncSettings) o;
        return enableBackupAndSync == that.enableBackupAndSync &&
                syncUi == that.syncUi &&
                syncCodeAndSystem == that.syncCodeAndSystem &&
                syncKeymaps == that.syncKeymaps &&
                syncPlugins == that.syncPlugins &&
                syncTools == that.syncTools &&
                Objects.equals(syncAccount, that.syncAccount) &&
                Objects.equals(availableAccounts, that.availableAccounts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enableBackupAndSync, syncAccount, availableAccounts,
                syncUi, syncCodeAndSystem, syncKeymaps, syncPlugins, syncTools);
    }
}
