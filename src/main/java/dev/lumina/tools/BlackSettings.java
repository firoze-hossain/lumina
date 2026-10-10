package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing Black code formatter settings in Lumina IDE.
 */
public class BlackSettings implements Cloneable {

    public static final String MODE_PACKAGE = "Package";
    public static final String MODE_BINARY = "Binary";
    public static final String NO_INTERPRETER = "<No interpreter>";

    private String executionMode = MODE_PACKAGE;
    private String pythonInterpreter = NO_INTERPRETER;
    private boolean onCodeReformat = false;
    private boolean onSave = false;
    private String arguments = "";

    public BlackSettings() {
    }

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode != null ? executionMode : MODE_PACKAGE;
    }

    public String getPythonInterpreter() {
        return pythonInterpreter;
    }

    public void setPythonInterpreter(String pythonInterpreter) {
        this.pythonInterpreter = pythonInterpreter != null ? pythonInterpreter : NO_INTERPRETER;
    }

    public boolean isOnCodeReformat() {
        return onCodeReformat;
    }

    public void setOnCodeReformat(boolean onCodeReformat) {
        this.onCodeReformat = onCodeReformat;
    }

    public boolean isOnSave() {
        return onSave;
    }

    public void setOnSave(boolean onSave) {
        this.onSave = onSave;
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments != null ? arguments : "";
    }

    @Override
    public BlackSettings clone() {
        try {
            return (BlackSettings) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlackSettings that = (BlackSettings) o;
        return onCodeReformat == that.onCodeReformat &&
                onSave == that.onSave &&
                Objects.equals(executionMode, that.executionMode) &&
                Objects.equals(pythonInterpreter, that.pythonInterpreter) &&
                Objects.equals(arguments, that.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionMode, pythonInterpreter, onCodeReformat, onSave, arguments);
    }
}
