package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing an Auto-Approve rule for terminal commands initiated by GitHub Copilot chat.
 */
public class TerminalAutoApproveRule implements Cloneable {

    private String pattern = "";
    private boolean autoApprove = false; // false = Never, true = Always

    public TerminalAutoApproveRule() {
    }

    public TerminalAutoApproveRule(String pattern, boolean autoApprove) {
        this.pattern = pattern != null ? pattern : "";
        this.autoApprove = autoApprove;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern != null ? pattern : "";
    }

    public boolean isAutoApprove() {
        return autoApprove;
    }

    public void setAutoApprove(boolean autoApprove) {
        this.autoApprove = autoApprove;
    }

    @Override
    public TerminalAutoApproveRule clone() {
        try {
            return (TerminalAutoApproveRule) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TerminalAutoApproveRule that = (TerminalAutoApproveRule) o;
        return autoApprove == that.autoApprove && Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern, autoApprove);
    }

    @Override
    public String toString() {
        return "TerminalAutoApproveRule{" +
                "pattern='" + pattern + '\'' +
                ", autoApprove=" + autoApprove +
                '}';
    }
}
