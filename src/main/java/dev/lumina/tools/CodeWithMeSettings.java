package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing collaborative development (Code With Me) configuration settings in Lumina IDE.
 */
public class CodeWithMeSettings implements Cloneable {

    private String userName;
    private String lobbyServerUrl = "";

    public CodeWithMeSettings() {
        this.userName = System.getProperty("user.name", "user");
    }

    public CodeWithMeSettings(String userName, String lobbyServerUrl) {
        this.userName = userName != null ? userName : System.getProperty("user.name", "user");
        this.lobbyServerUrl = lobbyServerUrl != null ? lobbyServerUrl : "";
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName != null ? userName : System.getProperty("user.name", "user");
    }

    public String getLobbyServerUrl() {
        return lobbyServerUrl;
    }

    public void setLobbyServerUrl(String lobbyServerUrl) {
        this.lobbyServerUrl = lobbyServerUrl != null ? lobbyServerUrl : "";
    }

    @Override
    public CodeWithMeSettings clone() {
        try {
            return (CodeWithMeSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeWithMeSettings that = (CodeWithMeSettings) o;
        return Objects.equals(userName, that.userName) &&
                Objects.equals(lobbyServerUrl, that.lobbyServerUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, lobbyServerUrl);
    }
}
