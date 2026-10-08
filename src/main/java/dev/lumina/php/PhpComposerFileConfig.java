package dev.lumina.php;

import java.util.Objects;

/**
 * Model representing a configured Composer file in a PHP project matching IntelliJ IDEA.
 */
public class PhpComposerFileConfig {

    private String id;
    private String path;
    private String executionMode; // e.g. "Composer executable" or "PHP Script"
    private String customOptions;

    public PhpComposerFileConfig() {
        this.id = "default";
        this.path = "composer.json";
        this.executionMode = "Composer executable";
        this.customOptions = "";
    }

    public PhpComposerFileConfig(String id, String path) {
        this.id = id;
        this.path = path;
        this.executionMode = "Composer executable";
        this.customOptions = "";
    }

    public PhpComposerFileConfig(String id, String path, String executionMode, String customOptions) {
        this.id = id;
        this.path = path;
        this.executionMode = executionMode != null ? executionMode : "Composer executable";
        this.customOptions = customOptions != null ? customOptions : "";
    }

    public PhpComposerFileConfig copy() {
        return new PhpComposerFileConfig(id, path, executionMode, customOptions);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public String getCustomOptions() {
        return customOptions;
    }

    public void setCustomOptions(String customOptions) {
        this.customOptions = customOptions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PhpComposerFileConfig that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(path, that.path) &&
                Objects.equals(executionMode, that.executionMode) &&
                Objects.equals(customOptions, that.customOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, path, executionMode, customOptions);
    }

    @Override
    public String toString() {
        return path != null ? path : "";
    }
}
