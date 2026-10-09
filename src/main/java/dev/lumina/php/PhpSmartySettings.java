package dev.lumina.php;

import java.util.Objects;

/**
 * Model representing PHP Smarty settings in Lumina IDE.
 * Faithfully matches Image 2.
 */
public class PhpSmartySettings {

    private String leftDelimiter = "{";
    private String rightDelimiter = "}";
    private boolean useSmarty3WhitespacesPolicy = true;

    public PhpSmartySettings() {}

    public PhpSmartySettings(PhpSmartySettings other) {
        if (other == null) return;
        this.leftDelimiter = other.leftDelimiter;
        this.rightDelimiter = other.rightDelimiter;
        this.useSmarty3WhitespacesPolicy = other.useSmarty3WhitespacesPolicy;
    }

    public PhpSmartySettings copy() {
        return new PhpSmartySettings(this);
    }

    public String getLeftDelimiter() {
        return leftDelimiter;
    }

    public void setLeftDelimiter(String leftDelimiter) {
        this.leftDelimiter = leftDelimiter != null ? leftDelimiter : "{";
    }

    public String getRightDelimiter() {
        return rightDelimiter;
    }

    public void setRightDelimiter(String rightDelimiter) {
        this.rightDelimiter = rightDelimiter != null ? rightDelimiter : "}";
    }

    public boolean isUseSmarty3WhitespacesPolicy() {
        return useSmarty3WhitespacesPolicy;
    }

    public void setUseSmarty3WhitespacesPolicy(boolean useSmarty3WhitespacesPolicy) {
        this.useSmarty3WhitespacesPolicy = useSmarty3WhitespacesPolicy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhpSmartySettings that)) return false;
        return useSmarty3WhitespacesPolicy == that.useSmarty3WhitespacesPolicy &&
                Objects.equals(leftDelimiter, that.leftDelimiter) &&
                Objects.equals(rightDelimiter, that.rightDelimiter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(leftDelimiter, rightDelimiter, useSmarty3WhitespacesPolicy);
    }
}
