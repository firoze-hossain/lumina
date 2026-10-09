package dev.lumina.javascript;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing JSHint configuration in Lumina IDE.
 */
public class JSHintSettings {

    private boolean enabled = false;
    private boolean useConfigFiles = false;
    private String version = "2.13.6 (bundled)";
    private Map<String, Boolean> options = new HashMap<>();
    private Map<String, String> paramOptions = new HashMap<>();

    public JSHintSettings() {
        initDefaultOptions();
    }

    public JSHintSettings(boolean enabled, boolean useConfigFiles, String version, Map<String, Boolean> options) {
        this(enabled, useConfigFiles, version, options, null);
    }

    public JSHintSettings(boolean enabled, boolean useConfigFiles, String version,
                          Map<String, Boolean> options, Map<String, String> paramOptions) {
        this.enabled = enabled;
        this.useConfigFiles = useConfigFiles;
        this.version = version != null ? version : "2.13.6 (bundled)";
        this.options = options != null ? new HashMap<>(options) : new HashMap<>();
        this.paramOptions = paramOptions != null ? new HashMap<>(paramOptions) : new HashMap<>();
        if (this.options.isEmpty() && this.paramOptions.isEmpty()) {
            initDefaultOptions();
        }
    }

    public JSHintSettings copy() {
        return new JSHintSettings(enabled, useConfigFiles, version, new HashMap<>(options), new HashMap<>(paramOptions));
    }

    public void initDefaultOptions() {
        options.clear();
        paramOptions.clear();

        for (JSHintOption opt : JSHintOption.getAllOptions()) {
            if (opt.isParametric()) {
                paramOptions.put(opt.getKey(), opt.getParamDefaultValue());
            } else {
                options.put(opt.getKey(), opt.isDefaultBooleanValue());
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUseConfigFiles() {
        return useConfigFiles;
    }

    public void setUseConfigFiles(boolean useConfigFiles) {
        this.useConfigFiles = useConfigFiles;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, Boolean> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Boolean> options) {
        this.options = options != null ? new HashMap<>(options) : new HashMap<>();
    }

    public Map<String, String> getParamOptions() {
        return paramOptions;
    }

    public void setParamOptions(Map<String, String> paramOptions) {
        this.paramOptions = paramOptions != null ? new HashMap<>(paramOptions) : new HashMap<>();
    }

    public boolean isOptionEnabled(String key) {
        return Boolean.TRUE.equals(options.get(key));
    }

    public void setOptionEnabled(String key, boolean enabled) {
        options.put(key, enabled);
    }

    public String getParamOption(String key) {
        return paramOptions.getOrDefault(key, "");
    }

    public void setParamOption(String key, String value) {
        paramOptions.put(key, value != null ? value : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JSHintSettings that)) return false;
        return enabled == that.enabled &&
                useConfigFiles == that.useConfigFiles &&
                Objects.equals(version, that.version) &&
                Objects.equals(options, that.options) &&
                Objects.equals(paramOptions, that.paramOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, useConfigFiles, version, options, paramOptions);
    }
}
