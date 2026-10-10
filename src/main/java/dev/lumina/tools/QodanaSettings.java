package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Tools > Qodana configuration settings in Lumina IDE.
 */
public class QodanaSettings implements Cloneable {

    public static final String DEFAULT_QODANA_URL = "qodana.cloud";

    private String qodanaUrl = DEFAULT_QODANA_URL;
    private boolean loggedIn = false;
    private String accountName = "";

    public QodanaSettings() {
    }

    public String getQodanaUrl() {
        return qodanaUrl;
    }

    public void setQodanaUrl(String qodanaUrl) {
        this.qodanaUrl = qodanaUrl != null && !qodanaUrl.isBlank() ? qodanaUrl.trim() : DEFAULT_QODANA_URL;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName != null ? accountName : "";
    }

    public QodanaSettings copy() {
        return clone();
    }

    @Override
    public QodanaSettings clone() {
        try {
            return (QodanaSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            QodanaSettings copy = new QodanaSettings();
            copy.qodanaUrl = this.qodanaUrl;
            copy.loggedIn = this.loggedIn;
            copy.accountName = this.accountName;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QodanaSettings that = (QodanaSettings) o;
        return loggedIn == that.loggedIn &&
                Objects.equals(qodanaUrl, that.qodanaUrl) &&
                Objects.equals(accountName, that.accountName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qodanaUrl, loggedIn, accountName);
    }
}
