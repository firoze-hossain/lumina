package dev.lumina.javascript;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Model representing JSHint configuration matching IntelliJ IDEA.
 */
public class JSHintSettings {

    private boolean enabled = false;
    private boolean useConfigFiles = false;
    private String version = "2.13.6 (bundled)";
    private Map<String, Boolean> options = new HashMap<>();

    public JSHintSettings() {
        initDefaultOptions();
    }

    public JSHintSettings(boolean enabled, boolean useConfigFiles, String version, Map<String, Boolean> options) {
        this.enabled = enabled;
        this.useConfigFiles = useConfigFiles;
        this.version = version != null ? version : "2.13.6 (bundled)";
        this.options = options != null ? new HashMap<>(options) : new HashMap<>();
        if (this.options.isEmpty()) {
            initDefaultOptions();
        }
    }

    public JSHintSettings copy() {
        return new JSHintSettings(enabled, useConfigFiles, version, new HashMap<>(options));
    }

    public void initDefaultOptions() {
        options.clear();
        // Defaults matching Screenshot 5
        options.put("bitwise", true);
        options.put("camelcase", false);
        options.put("curly", true);
        options.put("enforceall", false);
        options.put("eqeqeq", true);
        options.put("es3", false);
        options.put("es5", false);
        options.put("forin", true);
        options.put("freeze", false);
        options.put("immed", false);
        options.put("newcap", false);
        options.put("noarg", true);
        options.put("nocomma", false);
        options.put("noempty", true);
        options.put("nonbsp", false);
        options.put("nonew", true);
        options.put("undef", true);
        options.put("varstmt", false);

        // Relaxing defaults
        options.put("funcscope", false);
        options.put("futurehostile", false);
        options.put("globalstrict", false);
        options.put("iterator", false);
        options.put("notypeof", false);
        options.put("shadow", false);
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

    public boolean isOptionEnabled(String key) {
        return Boolean.TRUE.equals(options.get(key));
    }

    public void setOptionEnabled(String key, boolean enabled) {
        options.put(key, enabled);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JSHintSettings that)) return false;
        return enabled == that.enabled &&
                useConfigFiles == that.useConfigFiles &&
                Objects.equals(version, that.version) &&
                Objects.equals(options, that.options);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, useConfigFiles, version, options);
    }
}
