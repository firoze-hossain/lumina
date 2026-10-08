package dev.lumina.kubernetes;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Model representing a regex-based log filter in Kubernetes settings.
 */
public class KubernetesLogFilter implements Cloneable {

    private boolean enabled;
    private String pattern;
    private boolean bold;
    private boolean italic;
    private String colorHex;

    private transient Pattern compiledPattern;

    public KubernetesLogFilter() {
        this(true, "", false, false, "#DFE1E5");
    }

    public KubernetesLogFilter(boolean enabled, String pattern, boolean bold, boolean italic, String colorHex) {
        this.enabled = enabled;
        this.pattern = pattern;
        this.bold = bold;
        this.italic = italic;
        this.colorHex = colorHex != null ? colorHex : "#DFE1E5";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.compiledPattern = null;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }

    public boolean matches(String line) {
        if (!enabled || pattern == null || pattern.isBlank() || line == null) {
            return false;
        }
        try {
            if (compiledPattern == null) {
                compiledPattern = Pattern.compile(pattern);
            }
            return compiledPattern.matcher(line).find();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public KubernetesLogFilter clone() {
        try {
            return (KubernetesLogFilter) super.clone();
        } catch (CloneNotSupportedException e) {
            return new KubernetesLogFilter(this.enabled, this.pattern, this.bold, this.italic, this.colorHex);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesLogFilter that = (KubernetesLogFilter) o;
        return enabled == that.enabled &&
                bold == that.bold &&
                italic == that.italic &&
                Objects.equals(pattern, that.pattern) &&
                Objects.equals(colorHex, that.colorHex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, pattern, bold, italic, colorHex);
    }
}
