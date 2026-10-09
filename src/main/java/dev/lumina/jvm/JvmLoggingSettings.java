package dev.lumina.jvm;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Settings model for JVM Logging in Lumina IDE.
 */
public class JvmLoggingSettings {

    public static final String DEFAULT_VARIABLE_NAME = "log";
    public static final String DEFAULT_LOGGER = "Unspecified";

    public static final List<String> AVAILABLE_LOGGERS = List.of(
            "Unspecified",
            "SLF4J",
            "Log4j 2",
            "Apache Commons Logging",
            "JUL (java.util.logging)",
            "JBoss Logging",
            "System.Logger"
    );

    private String variableName = DEFAULT_VARIABLE_NAME;
    private String logger = DEFAULT_LOGGER;

    public JvmLoggingSettings() {
    }

    public JvmLoggingSettings(String variableName, String logger) {
        this.variableName = variableName != null ? variableName : DEFAULT_VARIABLE_NAME;
        this.logger = logger != null ? logger : DEFAULT_LOGGER;
    }

    public JvmLoggingSettings(JvmLoggingSettings other) {
        if (other != null) {
            this.variableName = other.variableName;
            this.logger = other.logger;
        }
    }

    public JvmLoggingSettings copy() {
        return new JvmLoggingSettings(this);
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName != null ? variableName : DEFAULT_VARIABLE_NAME;
    }

    public String getLogger() {
        return logger;
    }

    public void setLogger(String logger) {
        this.logger = logger != null ? logger : DEFAULT_LOGGER;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JvmLoggingSettings that = (JvmLoggingSettings) o;
        return Objects.equals(variableName, that.variableName) &&
                Objects.equals(logger, that.logger);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableName, logger);
    }
}
