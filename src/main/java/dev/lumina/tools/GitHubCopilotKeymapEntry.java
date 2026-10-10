package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing an action and its assigned keymap shortcut for GitHub Copilot.
 */
public class GitHubCopilotKeymapEntry {

    public static final String NOT_ASSIGNED = "Not assigned";

    private String action;
    private String keymap;

    public GitHubCopilotKeymapEntry() {
        this("", NOT_ASSIGNED);
    }

    public GitHubCopilotKeymapEntry(String action, String keymap) {
        this.action = action != null ? action : "";
        this.keymap = keymap != null && !keymap.isBlank() ? keymap : NOT_ASSIGNED;
    }

    public String getAction() {
        return action;
    }

    public String getActionName() {
        return action;
    }

    public void setAction(String action) {
        this.action = action != null ? action : "";
    }

    public void setActionName(String action) {
        setAction(action);
    }

    public String getKeymap() {
        return keymap;
    }

    public void setKeymap(String keymap) {
        this.keymap = keymap != null && !keymap.isBlank() ? keymap : NOT_ASSIGNED;
    }

    public GitHubCopilotKeymapEntry copy() {
        return new GitHubCopilotKeymapEntry(action, keymap);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubCopilotKeymapEntry that = (GitHubCopilotKeymapEntry) o;
        return Objects.equals(action, that.action) &&
                Objects.equals(keymap, that.keymap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, keymap);
    }

    @Override
    public String toString() {
        return action + " -> " + keymap;
    }
}
