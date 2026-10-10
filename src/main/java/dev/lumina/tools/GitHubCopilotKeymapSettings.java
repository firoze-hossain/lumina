package dev.lumina.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Model representing keymap settings for Tools > GitHub Copilot > Keymap matching Images 1 & 2.
 */
public class GitHubCopilotKeymapSettings implements Cloneable {

    private List<GitHubCopilotKeymapEntry> entries = new ArrayList<>();

    public GitHubCopilotKeymapSettings() {
        this.entries = createDefaultEntries();
    }

    public GitHubCopilotKeymapSettings(List<GitHubCopilotKeymapEntry> entries) {
        if (entries != null) {
            for (GitHubCopilotKeymapEntry e : entries) {
                this.entries.add(e != null ? e.copy() : new GitHubCopilotKeymapEntry());
            }
        }
    }

    public static List<GitHubCopilotKeymapEntry> createDefaultEntries() {
        List<GitHubCopilotKeymapEntry> list = new ArrayList<>();
        list.add(new GitHubCopilotKeymapEntry("Copilot: Accept Current Diff Block", "Ctrl+Y"));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Accept Next Edit Suggestion", "Tab"));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Disable Completions", "Ctrl+Alt+Shift+O"));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Discard Current Diff Block", "Ctrl+N"));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Enable completions", "Ctrl+Alt+Shift+O"));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Hide Next Edit Suggestions", "Escape"));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Open Chat", "Ctrl+Shift+C"));
        list.add(new GitHubCopilotKeymapEntry("Open Inline Chat", "Ctrl+Shift+G"));
        list.add(new GitHubCopilotKeymapEntry("Copilot: AI Credit Usage", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Collect Logs and Report Issue", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Disable Completions for File", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Edit Keyboard Shortcuts...", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Generate Commit Message", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Log CA Certificates", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Log Diagnostics", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Manage Paid Premium Requests", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Open Completions", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Open Settings...", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Review Code Changes", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Send Feedback...", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Show GitHub Copilot Status", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Sign in to GitHub", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Sign out from GitHub", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Status", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: Upgrade to Pro", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Copilot: View GitHub Documentation...", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Disable Next Edit Suggestions", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Enable Next Edit Suggestions", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Fix the vulnerable dependency with Copilot", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Generate Agent Instructions", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Scan and Resolve CVEs with Copilot", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("Upgrade with Copilot", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        list.add(new GitHubCopilotKeymapEntry("logModels", GitHubCopilotKeymapEntry.NOT_ASSIGNED));
        return list;
    }

    public List<GitHubCopilotKeymapEntry> getEntries() {
        return entries;
    }

    public void setEntries(List<GitHubCopilotKeymapEntry> entries) {
        this.entries = new ArrayList<>();
        if (entries != null) {
            for (GitHubCopilotKeymapEntry e : entries) {
                this.entries.add(e != null ? e.copy() : new GitHubCopilotKeymapEntry());
            }
        }
    }

    public String getShortcut(String action) {
        if (action == null) return GitHubCopilotKeymapEntry.NOT_ASSIGNED;
        for (GitHubCopilotKeymapEntry e : entries) {
            if (action.equals(e.getAction())) {
                return e.getKeymap();
            }
        }
        return GitHubCopilotKeymapEntry.NOT_ASSIGNED;
    }

    public void setShortcut(String action, String keymap) {
        if (action == null) return;
        for (GitHubCopilotKeymapEntry e : entries) {
            if (action.equals(e.getAction())) {
                e.setKeymap(keymap);
                return;
            }
        }
        entries.add(new GitHubCopilotKeymapEntry(action, keymap));
    }

    @Override
    public GitHubCopilotKeymapSettings clone() {
        return new GitHubCopilotKeymapSettings(entries);
    }

    public GitHubCopilotKeymapSettings copy() {
        return clone();
    }

    @Override
    public String toString() {
        return "GitHubCopilotKeymapSettings{entries=" + entries.size() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubCopilotKeymapSettings that = (GitHubCopilotKeymapSettings) o;
        return Objects.equals(entries, that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }
}
