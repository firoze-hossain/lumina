package dev.lumina.injections;

import java.util.Objects;

/**
 * Domain model representing a language injection configuration.
 * Encapsulates the host language, target injected language, target pattern,
 * scope (Built-in, IDE, Project), active status, and place counters.
 */
public class LanguageInjection implements Cloneable, Comparable<LanguageInjection> {

    private String id;
    private String displayName;
    private String hostLanguage;
    private String injectedLanguage;
    private LanguageInjectionScope scope;
    private boolean enabled;
    private int placesCount;
    private int enabledPlacesCount;
    private String injectionTypeId;
    private String prefix;
    private String suffix;
    private String pattern;
    private String pluginName;

    public LanguageInjection(
            String id,
            String displayName,
            String hostLanguage,
            String injectedLanguage,
            LanguageInjectionScope scope,
            boolean enabled,
            int placesCount,
            String injectionTypeId,
            String pattern
    ) {
        this.id = id;
        this.displayName = displayName;
        this.hostLanguage = hostLanguage;
        this.injectedLanguage = injectedLanguage;
        this.scope = scope != null ? scope : LanguageInjectionScope.IDE;
        this.enabled = enabled;
        this.placesCount = Math.max(1, placesCount);
        this.enabledPlacesCount = enabled ? this.placesCount : 0;
        this.injectionTypeId = injectionTypeId;
        this.pattern = pattern != null ? pattern : "";
        this.prefix = "";
        this.suffix = "";
        this.pluginName = "Core";
    }

    public LanguageInjection(
            String id,
            String displayName,
            String hostLanguage,
            String injectedLanguage,
            LanguageInjectionScope scope,
            boolean enabled,
            int placesCount,
            int enabledPlacesCount,
            String injectionTypeId,
            String prefix,
            String suffix,
            String pattern,
            String pluginName
    ) {
        this.id = id;
        this.displayName = displayName;
        this.hostLanguage = hostLanguage;
        this.injectedLanguage = injectedLanguage;
        this.scope = scope != null ? scope : LanguageInjectionScope.IDE;
        this.enabled = enabled;
        this.placesCount = Math.max(1, placesCount);
        this.enabledPlacesCount = Math.min(this.placesCount, Math.max(0, enabledPlacesCount));
        this.injectionTypeId = injectionTypeId;
        this.prefix = prefix != null ? prefix : "";
        this.suffix = suffix != null ? suffix : "";
        this.pattern = pattern != null ? pattern : "";
        this.pluginName = pluginName != null ? pluginName : "Core";
    }

    public LanguageInjection copy() {
        return new LanguageInjection(
                id,
                displayName,
                hostLanguage,
                injectedLanguage,
                scope,
                enabled,
                placesCount,
                enabledPlacesCount,
                injectionTypeId,
                prefix,
                suffix,
                pattern,
                pluginName
        );
    }

    public boolean isBuiltIn() {
        return scope == LanguageInjectionScope.BUILT_IN;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getHostLanguage() {
        return hostLanguage;
    }

    public void setHostLanguage(String hostLanguage) {
        this.hostLanguage = hostLanguage;
    }

    public String getInjectedLanguage() {
        return injectedLanguage;
    }

    public void setInjectedLanguage(String injectedLanguage) {
        this.injectedLanguage = injectedLanguage;
    }

    public LanguageInjectionScope getScope() {
        return scope;
    }

    public void setScope(LanguageInjectionScope scope) {
        this.scope = scope;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        this.enabledPlacesCount = enabled ? this.placesCount : 0;
    }

    public int getPlacesCount() {
        return placesCount;
    }

    public void setPlacesCount(int placesCount) {
        this.placesCount = Math.max(1, placesCount);
        if (this.enabled) {
            this.enabledPlacesCount = this.placesCount;
        } else {
            this.enabledPlacesCount = 0;
        }
    }

    public int getEnabledPlacesCount() {
        return enabledPlacesCount;
    }

    public void setEnabledPlacesCount(int enabledPlacesCount) {
        this.enabledPlacesCount = Math.min(placesCount, Math.max(0, enabledPlacesCount));
        this.enabled = this.enabledPlacesCount > 0;
    }

    public String getInjectionTypeId() {
        return injectionTypeId;
    }

    public void setInjectionTypeId(String injectionTypeId) {
        this.injectionTypeId = injectionTypeId;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LanguageInjection that)) return false;
        return enabled == that.enabled &&
                placesCount == that.placesCount &&
                enabledPlacesCount == that.enabledPlacesCount &&
                Objects.equals(id, that.id) &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(hostLanguage, that.hostLanguage) &&
                Objects.equals(injectedLanguage, that.injectedLanguage) &&
                scope == that.scope &&
                Objects.equals(injectionTypeId, that.injectionTypeId) &&
                Objects.equals(prefix, that.prefix) &&
                Objects.equals(suffix, that.suffix) &&
                Objects.equals(pattern, that.pattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, displayName, hostLanguage, injectedLanguage, scope, enabled, placesCount);
    }

    @Override
    public int compareTo(LanguageInjection other) {
        if (other == null) return 1;
        int c = String.CASE_INSENSITIVE_ORDER.compare(
                this.displayName != null ? this.displayName : "",
                other.displayName != null ? other.displayName : ""
        );
        if (c != 0) return c;
        return java.util.Objects.compare(this.id, other.id, java.util.Comparator.nullsLast(String::compareTo));
    }

    @Override
    public String toString() {
        return displayName + " [" + injectedLanguage + ", " + scope + "]";
    }
}
