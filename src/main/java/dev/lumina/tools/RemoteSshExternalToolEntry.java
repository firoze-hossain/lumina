package dev.lumina.tools;

import java.util.Objects;

/**
 * Model representing a single Remote SSH External Tool entry in Lumina IDE.
 */
public class RemoteSshExternalToolEntry implements Cloneable {

    public static final String CONN_CURRENT_VAGRANT = "CURRENT_VAGRANT";
    public static final String CONN_DEFAULT_PYTHON_REMOTE = "DEFAULT_PYTHON_REMOTE";
    public static final String CONN_SSH_CONFIG = "SSH_CONFIG";

    private String name = "";
    private String group = "Remote Tools";
    private String description = "";
    private String program = "";
    private String arguments = "";
    private String workingDirectory = "";

    private String connectionType = CONN_SSH_CONFIG;
    private String sshConfiguration = "Select SSH configuration on every run";

    private boolean synchronizeFilesAfterExecution = true;
    private boolean openConsoleForToolOutput = true;
    private boolean makeConsoleActiveOnStdout = false;
    private boolean makeConsoleActiveOnStderr = false;
    private String outputFilters = "";

    public RemoteSshExternalToolEntry() {
    }

    public RemoteSshExternalToolEntry(String name, String group, String description) {
        this.name = name != null ? name : "";
        this.group = group != null ? group : "Remote Tools";
        this.description = description != null ? description : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group != null ? group : "Remote Tools";
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public String getProgram() {
        return program;
    }

    public void setProgram(String program) {
        this.program = program != null ? program : "";
    }

    public String getArguments() {
        return arguments;
    }

    public void setArguments(String arguments) {
        this.arguments = arguments != null ? arguments : "";
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = workingDirectory != null ? workingDirectory : "";
    }

    public String getConnectionType() {
        return connectionType;
    }

    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType != null ? connectionType : CONN_SSH_CONFIG;
    }

    public String getSshConfiguration() {
        return sshConfiguration;
    }

    public void setSshConfiguration(String sshConfiguration) {
        this.sshConfiguration = sshConfiguration != null ? sshConfiguration : "Select SSH configuration on every run";
    }

    public boolean isSynchronizeFilesAfterExecution() {
        return synchronizeFilesAfterExecution;
    }

    public void setSynchronizeFilesAfterExecution(boolean synchronizeFilesAfterExecution) {
        this.synchronizeFilesAfterExecution = synchronizeFilesAfterExecution;
    }

    public boolean isOpenConsoleForToolOutput() {
        return openConsoleForToolOutput;
    }

    public void setOpenConsoleForToolOutput(boolean openConsoleForToolOutput) {
        this.openConsoleForToolOutput = openConsoleForToolOutput;
    }

    public boolean isMakeConsoleActiveOnStdout() {
        return makeConsoleActiveOnStdout;
    }

    public void setMakeConsoleActiveOnStdout(boolean makeConsoleActiveOnStdout) {
        this.makeConsoleActiveOnStdout = makeConsoleActiveOnStdout;
    }

    public boolean isMakeConsoleActiveOnStderr() {
        return makeConsoleActiveOnStderr;
    }

    public void setMakeConsoleActiveOnStderr(boolean makeConsoleActiveOnStderr) {
        this.makeConsoleActiveOnStderr = makeConsoleActiveOnStderr;
    }

    public String getOutputFilters() {
        return outputFilters;
    }

    public void setOutputFilters(String outputFilters) {
        this.outputFilters = outputFilters != null ? outputFilters : "";
    }

    @Override
    public RemoteSshExternalToolEntry clone() {
        try {
            return (RemoteSshExternalToolEntry) super.clone();
        } catch (CloneNotSupportedException e) {
            RemoteSshExternalToolEntry copy = new RemoteSshExternalToolEntry(this.name, this.group, this.description);
            copy.program = this.program;
            copy.arguments = this.arguments;
            copy.workingDirectory = this.workingDirectory;
            copy.connectionType = this.connectionType;
            copy.sshConfiguration = this.sshConfiguration;
            copy.synchronizeFilesAfterExecution = this.synchronizeFilesAfterExecution;
            copy.openConsoleForToolOutput = this.openConsoleForToolOutput;
            copy.makeConsoleActiveOnStdout = this.makeConsoleActiveOnStdout;
            copy.makeConsoleActiveOnStderr = this.makeConsoleActiveOnStderr;
            copy.outputFilters = this.outputFilters;
            return copy;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteSshExternalToolEntry that = (RemoteSshExternalToolEntry) o;
        return synchronizeFilesAfterExecution == that.synchronizeFilesAfterExecution &&
                openConsoleForToolOutput == that.openConsoleForToolOutput &&
                makeConsoleActiveOnStdout == that.makeConsoleActiveOnStdout &&
                makeConsoleActiveOnStderr == that.makeConsoleActiveOnStderr &&
                Objects.equals(name, that.name) &&
                Objects.equals(group, that.group) &&
                Objects.equals(description, that.description) &&
                Objects.equals(program, that.program) &&
                Objects.equals(arguments, that.arguments) &&
                Objects.equals(workingDirectory, that.workingDirectory) &&
                Objects.equals(connectionType, that.connectionType) &&
                Objects.equals(sshConfiguration, that.sshConfiguration) &&
                Objects.equals(outputFilters, that.outputFilters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, group, description, program, arguments, workingDirectory,
                connectionType, sshConfiguration, synchronizeFilesAfterExecution,
                openConsoleForToolOutput, makeConsoleActiveOnStdout, makeConsoleActiveOnStderr, outputFilters);
    }
}
