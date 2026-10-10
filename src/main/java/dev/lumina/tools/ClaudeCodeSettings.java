package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Claude Code [Beta] configuration settings in Lumina IDE.
 */
public class ClaudeCodeSettings implements Cloneable {

    public static final String DEFAULT_COMMAND = "claude";

    private String claudeCommand = DEFAULT_COMMAND;
    private String configDirectory = "";
    private boolean suppressNotificationNotFound = false;
    private boolean hideToolbarButton = false;
    private boolean optionEnterMultiLine = true;
    private boolean automaticUpdates = true;
    private boolean acceptConnectionsAllInterfaces = false;

    public ClaudeCodeSettings() {
    }

    public String getClaudeCommand() {
        return claudeCommand;
    }

    public void setClaudeCommand(String claudeCommand) {
        this.claudeCommand = claudeCommand != null && !claudeCommand.isBlank() ? claudeCommand : DEFAULT_COMMAND;
    }

    public String getConfigDirectory() {
        return configDirectory;
    }

    public void setConfigDirectory(String configDirectory) {
        this.configDirectory = configDirectory != null ? configDirectory : "";
    }

    public boolean isSuppressNotificationNotFound() {
        return suppressNotificationNotFound;
    }

    public void setSuppressNotificationNotFound(boolean suppressNotificationNotFound) {
        this.suppressNotificationNotFound = suppressNotificationNotFound;
    }

    public boolean isHideToolbarButton() {
        return hideToolbarButton;
    }

    public void setHideToolbarButton(boolean hideToolbarButton) {
        this.hideToolbarButton = hideToolbarButton;
    }

    public boolean isOptionEnterMultiLine() {
        return optionEnterMultiLine;
    }

    public void setOptionEnterMultiLine(boolean optionEnterMultiLine) {
        this.optionEnterMultiLine = optionEnterMultiLine;
    }

    public boolean isAutomaticUpdates() {
        return automaticUpdates;
    }

    public void setAutomaticUpdates(boolean automaticUpdates) {
        this.automaticUpdates = automaticUpdates;
    }

    public boolean isAcceptConnectionsAllInterfaces() {
        return acceptConnectionsAllInterfaces;
    }

    public void setAcceptConnectionsAllInterfaces(boolean acceptConnectionsAllInterfaces) {
        this.acceptConnectionsAllInterfaces = acceptConnectionsAllInterfaces;
    }

    @Override
    public ClaudeCodeSettings clone() {
        try {
            return (ClaudeCodeSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClaudeCodeSettings that = (ClaudeCodeSettings) o;
        return suppressNotificationNotFound == that.suppressNotificationNotFound &&
                hideToolbarButton == that.hideToolbarButton &&
                optionEnterMultiLine == that.optionEnterMultiLine &&
                automaticUpdates == that.automaticUpdates &&
                acceptConnectionsAllInterfaces == that.acceptConnectionsAllInterfaces &&
                Objects.equals(claudeCommand, that.claudeCommand) &&
                Objects.equals(configDirectory, that.configDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(claudeCommand, configDirectory, suppressNotificationNotFound,
                hideToolbarButton, optionEnterMultiLine, automaticUpdates, acceptConnectionsAllInterfaces);
    }
}
