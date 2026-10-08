package dev.lumina.build;

import java.util.Objects;

/**
 * Configuration model for Build, Execution, Deployment > Console > Python Console settings in Lumina IDE.
 * Matches 1:1 with reference screenshot media_1791450036784_1847c275.png:
 *  - Environment:
 *    - Environment variables
 *    - Python interpreter (Use SDK of module / Use specified interpreter)
 *    - Interpreter options
 *    - Working directory
 *    - Add content roots to PYTHONPATH
 *    - Add source roots to PYTHONPATH
 *  - Starting script
 */
public class PythonConsoleSettings implements Cloneable {

    public static final String DEFAULT_STARTING_SCRIPT =
            "import sys; print('Python %s on %s' % (sys.version, sys.platform))\n" +
            "sys.path.extend([WORKING_DIR_AND_PYTHON_PATHS])";

    private String environmentVariables = "";
    private boolean useModuleSdk = false;
    private String selectedModule = "[none]";
    private boolean useSpecifiedInterpreter = true;
    private String specifiedInterpreter = "<Project Default>";
    private String interpreterOptions = "";
    private String workingDirectory = "";
    private boolean addContentRootsToPythonPath = true;
    private boolean addSourceRootsToPythonPath = true;
    private String startingScript = DEFAULT_STARTING_SCRIPT;

    public PythonConsoleSettings() {
    }

    public PythonConsoleSettings(PythonConsoleSettings other) {
        if (other != null) {
            this.environmentVariables = other.environmentVariables;
            this.useModuleSdk = other.useModuleSdk;
            this.selectedModule = other.selectedModule;
            this.useSpecifiedInterpreter = other.useSpecifiedInterpreter;
            this.specifiedInterpreter = other.specifiedInterpreter;
            this.interpreterOptions = other.interpreterOptions;
            this.workingDirectory = other.workingDirectory;
            this.addContentRootsToPythonPath = other.addContentRootsToPythonPath;
            this.addSourceRootsToPythonPath = other.addSourceRootsToPythonPath;
            this.startingScript = other.startingScript;
        }
    }

    public String getEnvironmentVariables() { return environmentVariables; }
    public void setEnvironmentVariables(String environmentVariables) { this.environmentVariables = environmentVariables != null ? environmentVariables : ""; }

    public boolean isUseModuleSdk() { return useModuleSdk; }
    public void setUseModuleSdk(boolean useModuleSdk) { this.useModuleSdk = useModuleSdk; }

    public String getSelectedModule() { return selectedModule; }
    public void setSelectedModule(String selectedModule) { this.selectedModule = selectedModule != null ? selectedModule : "[none]"; }

    public boolean isUseSpecifiedInterpreter() { return useSpecifiedInterpreter; }
    public void setUseSpecifiedInterpreter(boolean useSpecifiedInterpreter) { this.useSpecifiedInterpreter = useSpecifiedInterpreter; }

    public String getSpecifiedInterpreter() { return specifiedInterpreter; }
    public void setSpecifiedInterpreter(String specifiedInterpreter) { this.specifiedInterpreter = specifiedInterpreter != null ? specifiedInterpreter : "<Project Default>"; }

    public String getInterpreterOptions() { return interpreterOptions; }
    public void setInterpreterOptions(String interpreterOptions) { this.interpreterOptions = interpreterOptions != null ? interpreterOptions : ""; }

    public String getWorkingDirectory() { return workingDirectory; }
    public void setWorkingDirectory(String workingDirectory) { this.workingDirectory = workingDirectory != null ? workingDirectory : ""; }

    public boolean isAddContentRootsToPythonPath() { return addContentRootsToPythonPath; }
    public void setAddContentRootsToPythonPath(boolean addContentRootsToPythonPath) { this.addContentRootsToPythonPath = addContentRootsToPythonPath; }

    public boolean isAddSourceRootsToPythonPath() { return addSourceRootsToPythonPath; }
    public void setAddSourceRootsToPythonPath(boolean addSourceRootsToPythonPath) { this.addSourceRootsToPythonPath = addSourceRootsToPythonPath; }

    public String getStartingScript() { return startingScript; }
    public void setStartingScript(String startingScript) { this.startingScript = startingScript != null ? startingScript : DEFAULT_STARTING_SCRIPT; }

    /**
     * Dynamically detects available Python interpreters on the current system,
     * including virtualenvs in the project and common system installations.
     */
    public static java.util.List<String> detectAvailableInterpreters() {
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add("<Project Default>");
        String userDir = System.getProperty("user.dir", ".");
        String[] venvRelatives = {
                ".venv/bin/python3", ".venv/bin/python",
                "venv/bin/python3", "venv/bin/python",
                "env/bin/python3", "env/bin/python",
                ".venv/Scripts/python.exe", "venv/Scripts/python.exe"
        };
        for (String rel : venvRelatives) {
            java.io.File f = new java.io.File(userDir, rel);
            if (f.exists() && f.canExecute()) {
                list.add(f.getAbsolutePath());
            }
        }
        String[] sysPaths = {
                "/opt/homebrew/bin/python3",
                "/usr/local/bin/python3",
                "/usr/bin/python3",
                "/usr/bin/python"
        };
        for (String sp : sysPaths) {
            java.io.File f = new java.io.File(sp);
            if (f.exists() && f.canExecute() && !list.contains(sp)) {
                list.add(sp);
            }
        }
        return list;
    }

    /**
     * Dynamically detects project modules or directories available for Python console SDK selection.
     */
    public static java.util.List<String> detectAvailableModules() {
        java.util.List<String> list = new java.util.ArrayList<>();
        list.add("[none]");
        try {
            java.io.File cur = new java.io.File(System.getProperty("user.dir", "."));
            String name = cur.getName();
            if (name != null && !name.isBlank() && !name.equals(".") && !list.contains(name)) {
                list.add(name);
            }
        } catch (Throwable ignored) {}
        return list;
    }

    @Override
    public PythonConsoleSettings clone() {
        return new PythonConsoleSettings(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PythonConsoleSettings that = (PythonConsoleSettings) o;
        return useModuleSdk == that.useModuleSdk &&
                useSpecifiedInterpreter == that.useSpecifiedInterpreter &&
                addContentRootsToPythonPath == that.addContentRootsToPythonPath &&
                addSourceRootsToPythonPath == that.addSourceRootsToPythonPath &&
                Objects.equals(environmentVariables, that.environmentVariables) &&
                Objects.equals(selectedModule, that.selectedModule) &&
                Objects.equals(specifiedInterpreter, that.specifiedInterpreter) &&
                Objects.equals(interpreterOptions, that.interpreterOptions) &&
                Objects.equals(workingDirectory, that.workingDirectory) &&
                Objects.equals(startingScript, that.startingScript);
    }

    @Override
    public int hashCode() {
        return Objects.hash(environmentVariables, useModuleSdk, selectedModule, useSpecifiedInterpreter,
                specifiedInterpreter, interpreterOptions, workingDirectory, addContentRootsToPythonPath,
                addSourceRootsToPythonPath, startingScript);
    }
}
