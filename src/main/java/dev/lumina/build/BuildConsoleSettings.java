package dev.lumina.build;

import java.util.Objects;

/**
 * Configuration model for Build, Execution, Deployment > Console settings in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791450015890_b8b24dc5.png:
 *  - General settings:
 *    - Always show Debug Console
 *    - Use IPython if available
 *    - Show console variables by default
 *    - Use existing console for "Run with Python Console"
 *    - Command queue for Python Console
 *  - Code completion: Static / Runtime / None
 */
public class BuildConsoleSettings implements Cloneable {

    private boolean alwaysShowDebugConsole = true;
    private boolean useIPythonIfAvailable = true;
    private boolean showConsoleVariablesByDefault = true;
    private boolean useExistingConsoleForRunWithPythonConsole = false;
    private boolean commandQueueForPythonConsole = false;
    private String codeCompletion = "Static";

    public BuildConsoleSettings() {
    }

    public BuildConsoleSettings(BuildConsoleSettings other) {
        if (other != null) {
            this.alwaysShowDebugConsole = other.alwaysShowDebugConsole;
            this.useIPythonIfAvailable = other.useIPythonIfAvailable;
            this.showConsoleVariablesByDefault = other.showConsoleVariablesByDefault;
            this.useExistingConsoleForRunWithPythonConsole = other.useExistingConsoleForRunWithPythonConsole;
            this.commandQueueForPythonConsole = other.commandQueueForPythonConsole;
            this.codeCompletion = other.codeCompletion;
        }
    }

    public boolean isAlwaysShowDebugConsole() { return alwaysShowDebugConsole; }
    public void setAlwaysShowDebugConsole(boolean alwaysShowDebugConsole) { this.alwaysShowDebugConsole = alwaysShowDebugConsole; }

    public boolean isUseIPythonIfAvailable() { return useIPythonIfAvailable; }
    public void setUseIPythonIfAvailable(boolean useIPythonIfAvailable) { this.useIPythonIfAvailable = useIPythonIfAvailable; }

    public boolean isShowConsoleVariablesByDefault() { return showConsoleVariablesByDefault; }
    public void setShowConsoleVariablesByDefault(boolean showConsoleVariablesByDefault) { this.showConsoleVariablesByDefault = showConsoleVariablesByDefault; }

    public boolean isUseExistingConsoleForRunWithPythonConsole() { return useExistingConsoleForRunWithPythonConsole; }
    public void setUseExistingConsoleForRunWithPythonConsole(boolean val) { this.useExistingConsoleForRunWithPythonConsole = val; }

    public boolean isCommandQueueForPythonConsole() { return commandQueueForPythonConsole; }
    public void setCommandQueueForPythonConsole(boolean commandQueueForPythonConsole) { this.commandQueueForPythonConsole = commandQueueForPythonConsole; }

    public String getCodeCompletion() { return codeCompletion; }
    public void setCodeCompletion(String codeCompletion) { this.codeCompletion = codeCompletion != null ? codeCompletion : "Static"; }

    @Override
    public BuildConsoleSettings clone() {
        return new BuildConsoleSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildConsoleSettings that = (BuildConsoleSettings) o;
        return alwaysShowDebugConsole == that.alwaysShowDebugConsole &&
                useIPythonIfAvailable == that.useIPythonIfAvailable &&
                showConsoleVariablesByDefault == that.showConsoleVariablesByDefault &&
                useExistingConsoleForRunWithPythonConsole == that.useExistingConsoleForRunWithPythonConsole &&
                commandQueueForPythonConsole == that.commandQueueForPythonConsole &&
                Objects.equals(codeCompletion, that.codeCompletion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(alwaysShowDebugConsole, useIPythonIfAvailable, showConsoleVariablesByDefault,
                useExistingConsoleForRunWithPythonConsole, commandQueueForPythonConsole, codeCompletion);
    }
}
